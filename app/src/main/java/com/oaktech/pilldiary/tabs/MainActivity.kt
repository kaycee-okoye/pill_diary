package com.oaktech.pilldiary.tabs

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.oaktech.pilldiary.*
import com.oaktech.pilldiary.tabs.views.PillFragment
import com.oaktech.pilldiary.databinding.ActivityMainBinding
import com.oaktech.pilldiary.tabs.controllers.TabAdapter
import com.oaktech.pilldiary.tabs.controllers.PillBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Home screen activity
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // binding for layout file
    private lateinit var fragments: List<Fragment> // fragment displayed in the tab view
    private lateinit var fragmentTitles: List<String> // title of each tab

    // tab indices of the two tabs
    private val MEDICATION_TAB_INDEX: Int = 0
    private val ENTRIES_TAB_INDEX: Int = 1

    private var screenHeight: Int = 0 // height of the phone screen
    private var screenWidth: Int = 0 // width of the phone screen
    private var isSearching: Boolean = false // if the search bar is currently showing
    private var currentPage: Int = 0 // the index of the tab currently being shown


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragments = listOf<Fragment>(PillFragment(), EntryFragment())
        fragmentTitles = listOf<String>("Medication", "Entries")

        // setup and populate the tab layout from the layout file
        val viewPager: ViewPager2 = binding.viewPager
        val tabs: TabLayout = binding.tabs
        val adapter: TabAdapter = TabAdapter(this, fragments)
        viewPager.adapter = adapter
        TabLayoutMediator(tabs, viewPager, true) { tab, position ->
            tab.text = fragmentTitles[position]
        }.attach()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenHeight = displayMetrics.heightPixels // set the screen height
        screenWidth = displayMetrics.widthPixels // set the screen width

        val fab: FloatingActionButton = binding.fab
        fab.setOnClickListener { view ->
            // if the floating action button is clicked
            when (currentPage) {
                MEDICATION_TAB_INDEX -> showPillManagementDialog(null) // show popup to add
                // medication if the tab
                // layout is currently showing medications
                ENTRIES_TAB_INDEX -> showEntryManagementDialog(null) // show popup to add
                // new entry if the tab
                // layout is currently showing entries
            }
        }

        binding.appBarSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // if the search bar is currently being shown and the user edits the text
                val currentFragment = fragments.get(currentPage)
                when (currentPage) {
                    MEDICATION_TAB_INDEX -> {
                        // apply search filter for medications currently viewing medications
                        (currentFragment as PillFragment)
                            .setFilter(binding.appBarSearch.text.toString())
                    }

                    ENTRIES_TAB_INDEX -> {
                        // apply search filter for entries if the user is currently viewing entries
                        (currentFragment as EntryFragment)
                            .setTextFilter(binding.appBarSearch.text.toString())
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            // if the user navigates to a new tab
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (isSearching) hideSearchBar() // stop showing searchbar
                // set index of current tab
                viewPager.currentItem = tab.position
                currentPage = tab.position
                toggleFilterIconVisibility() // make filter icon visible if the user is looking at entries
                // because the medication doesn't have any additional filters
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        toggleFilterIconVisibility()
    }

    /**
     * Method to display popup window to allow the user apply filters to Entries list view
     */
    fun showFilterDialog() {
        blurBackground()

        val currentFragment: EntryFragment = fragments.get(ENTRIES_TAB_INDEX) as EntryFragment
        val filters = currentFragment.getFilters()

        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val height = screenHeight * 5 / 10
        val width = screenWidth * 9 / 10

        // inflate a custom view using layout inflate
        val view = inflater.inflate(R.layout.entry_filter, null, false)
        val spinner =
            view.findViewById<Spinner>(R.id.entry_type) // dropdown spinner to show all the types
        // of entries to choose from
        val entryTypes = arrayOf<String>("All Entries", "Medication Entries", "Custom Entries")
        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this,
            R.layout.spinner_text, entryTypes
        )
        arrayAdapter.setDropDownViewResource(R.layout.spinner_text)
        spinner.adapter = arrayAdapter

        // view switchers to show the date/time selector when the user selects
        // that they want to filter based on each one
        val dateSwitcher = view.findViewById<ViewSwitcher>(R.id.date_switch)
        val timeSwitcher = view.findViewById<ViewSwitcher>(R.id.time_switch)
        val datePicker =
            view.findViewById<DatePicker>(R.id.date_filter) // date picker to select
        // date to filter for
        val timePicker =
            view.findViewById<TimePicker>(R.id.time_filter) // time picker to select
        // time to filter for
        val dateCheckBox =
            view.findViewById<CheckBox>(R.id.date_filter_check) // checkbox to filter
        // by date and make date picker visible
        val timeCheckBox =
            view.findViewById<CheckBox>(R.id.time_filter_check) // checkbox to filter
        // by time and make time picker visible

        val applyButton = view.findViewById<Button>(R.id.apply_filter) // apply filters to list view
        val cancelButton = view.findViewById<Button>(R.id.cancel_filter) // dismiss popup
        val clearButton = view.findViewById<Button>(R.id.clear_filter) // undo all filters


        // if user has already selected filters in the past without clearing, let
        // the popup reflect this
        when (filters.searchType) {
            EntrySearchType.pillAdministration -> spinner.setSelection(1)
            EntrySearchType.notes -> spinner.setSelection(2)
            else -> spinner.setSelection(0)
        }
        if (filters.dateFilter != null) {
            dateCheckBox.isChecked = true
            dateSwitcher.showNext()
            datePicker.updateDate(
                filters.dateFilter!!.year,
                filters.dateFilter!!.monthValue,
                filters.dateFilter!!.dayOfMonth
            )
        }
        if (filters.timeFilter != null) {
            timeCheckBox.isChecked = true
            timeSwitcher.showNext()
            timePicker.hour = filters.timeFilter!!.hour
            timePicker.minute = filters.timeFilter!!.minute
        }

        // initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // custom view to show in popup window
            width, // width of popup window
            height // window height
        )
        popupWindow.elevation = 10.0F
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true

        popupWindow.setOnDismissListener(PopupWindow.OnDismissListener {
            refreshTab()
            unblurBackground()
        })

        view.findViewById<CheckBox>(R.id.date_filter_check).setOnClickListener {
            dateSwitcher.showNext()
        }

        view.findViewById<CheckBox>(R.id.time_filter_check).setOnClickListener {
            timeSwitcher.showNext()
        }

        clearButton.setOnClickListener {
            currentFragment.clearFilters()
        }

        applyButton.setOnClickListener {
            // put all selected filters in a data class and send to the
            // fragment to apply them to listview
            var dateFilter: LocalDate? = null
            var timeFilter: LocalTime? = null
            var searchType = EntrySearchType.all

            if (dateCheckBox.isChecked == true) {
                dateFilter = LocalDate.of(
                    datePicker.year, datePicker.month, datePicker.dayOfMonth
                )
            }
            if (timeCheckBox.isChecked) {
                timeFilter = LocalTime.of(timePicker.hour, timePicker.minute)
            }
            searchType = when (spinner.selectedItemPosition) {
                0 -> EntrySearchType.all
                1 -> EntrySearchType.pillAdministration
                2 -> EntrySearchType.notes
                else -> EntrySearchType.all
            }
            currentFragment.applyFilters(
                EntryFilter(
                    dateFilter,
                    timeFilter,
                    searchType
                )
            )
            popupWindow.dismiss()
        }

        cancelButton.setOnClickListener {
            // dismiss popup window when the cancel button is pressed
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(
            binding.root, // location to display popup window
            Gravity.CENTER, // exact position of layout to display popup
            0, // x offset
            0 // y offset
        )
    }

    /**
     * Method to display popup window to add/edit medication
     *
     * @param pill the pill being edited, optional
     */
    fun showPillManagementDialog(pill: Pill?) {
        blurBackground()
        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupHeight = screenHeight * 4 / 10
        val popupWidth = screenWidth * 9 / 10

        // inflate a custom view using layout inflate
        val view = inflater.inflate(R.layout.pill_maker, null, false)
        val addButton = view.findViewById<Button>(R.id.add_pill)
        val cancelButton = view.findViewById<Button>(R.id.cancel_pill)
        val nameInput = view.findViewById<EditText>(R.id.inpPill)
        val dosageInput = view.findViewById<EditText>(R.id.inpDosage)
        val titleTextView = view.findViewById<TextView>(R.id.head)

        // check if this is a create/edit operation
        if (pill != null) {
            addButton.text = "Update"
            titleTextView.text = "Edit Medication"
            nameInput.setText(pill.name)
            dosageInput.setText(pill.dosage)
        }

        // initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // custom view to show in popup window
            popupWidth, // width of popup window
            popupHeight // window height
        )
        popupWindow.elevation = 10.0F
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true

        popupWindow.setOnDismissListener(PopupWindow.OnDismissListener {
            // when popup is being dismissed
            refreshTab()
            unblurBackground()
        })

        addButton.setOnClickListener {
            if (nameInput.text.isNullOrBlank()) {
                Toast.makeText(
                    this, "Please input medication name", Toast.LENGTH_SHORT
                ).show()
            } else if (nameInput.text.isNullOrBlank()) {
                Toast.makeText(
                    this, "Please input medication dosage", Toast.LENGTH_SHORT
                ).show()
            } else {
                val base: PillBase = PillBase(this)
                if (
                    (pill != null && pill.name == nameInput.text.toString().trim()) ||
                    !base.pillNameAlreadyExists(nameInput.text.toString().trim())
                ) {
                    base.upsertPill(
                        Pill(
                            nameInput.text.toString().trim(),
                            dosageInput.text.toString().trim(),
                            null
                        )
                    )
                    Toast.makeText(this, "Medication Added", Toast.LENGTH_SHORT).show()
                    popupWindow.dismiss()
                } else {
                    Toast.makeText(
                        this,
                        "Medication name already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        cancelButton.setOnClickListener {
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(
            binding.root, // location to display popup window
            Gravity.CENTER, // exact position of layout to display popup
            0, // x offset
            0 // y offset
        )
    }

    /**
     * Method to display popup window to add/edit entry
     *
     * @param entry the entry being edited, optional
     */
    fun showEntryManagementDialog(entry: Entry?) {
        blurBackground() // blur screen when the popup is being shown

        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupHeight = screenHeight * 4 / 10
        val popupWidth = screenWidth * 9 / 10

        // inflate a custom view using layout inflate
        val view = inflater.inflate(R.layout.entry_maker, null, false)
        val addButton = view.findViewById<Button>(R.id.add_entry)
        val cancelButton = view.findViewById<Button>(R.id.cancel_entry)
        val entryMessageInput = view.findViewById<EditText>(R.id.inpEntry)
        val titleTextView = view.findViewById<TextView>(R.id.head)

        // check if this is a create/edit operation
        if (entry != null) {
            entryMessageInput.setText(entry.entryMessage)
            addButton.text = "Update"
            titleTextView.text = "Edit Entry"
        }

        // initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            popupWidth.toInt(), // Width of popup window
            popupHeight.toInt() // Window height
        )
        popupWindow.elevation = 10.0F
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true

        addButton.setOnClickListener {
            if (entryMessageInput.text.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Entry cannot be blank",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // close current popup and open new popup to set date/time of
                // entry
                val updatedEntry = if (entry == null) Entry() else entry
                updatedEntry.entryMessage = entryMessageInput.text.toString()
                popupWindow.dismiss()
                showEntryDateInputDialog(updatedEntry)
            }
        }

        popupWindow.setOnDismissListener(PopupWindow.OnDismissListener {
            unblurBackground() // if the popup is being dismissed, unblur the background
        })

        cancelButton.setOnClickListener {
            popupWindow.dismiss() // dismiss popup if the cancel button is pressed
        }

        popupWindow.showAtLocation(
            binding.root, // location to display popup window
            Gravity.CENTER, // exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }

    /**
     * Method to display popup window to add/edit date & time of an entry
     *
     * @param entry the entry being edited, optional
     */
    private fun showEntryDateInputDialog(entry: Entry) {
        blurBackground() // blur screen behind the popup

        val inflater: LayoutInflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupWidth = screenWidth * 97 / 100
        val popupHeight = popupWidth // make the popup window a square

        // inflate a custom view using layout inflate
        val view = inflater.inflate(R.layout.date_manager, null, false)
        val confirmButton = view.findViewById<Button>(R.id.confirm)
        val backButton = view.findViewById<Button>(R.id.back)
        val cancelButton = view.findViewById<Button>(R.id.cancel)
        val dateInput = view.findViewById<DatePicker>(R.id.inpDate)
        val timeInput = view.findViewById<TimePicker>(R.id.inpTime)

        var dateTime = if (entry.entryDate == null) LocalDateTime.now() else entry.entryDate
        dateInput.updateDate(dateTime!!.year, dateTime.monthValue, dateTime.dayOfMonth)
        timeInput.hour = dateTime.hour
        timeInput.minute = dateTime.minute

        // initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // custom view to show in popup window
            popupWidth, // width of popup window
            popupHeight // window height
        )
        popupWindow.elevation = 10.0F
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true

        confirmButton.setOnClickListener {
            // set date and time of entry from date and time widgets
            dateTime = LocalDateTime.of(
                dateInput.year, dateInput.month, dateInput.dayOfMonth,
                timeInput.hour, timeInput.minute
            )
            entry.entryDate = dateTime // set date and time of the entry

            // add/edit entry in database
            val base: PillBase = PillBase(this) // initialize database helper class
            base.upsertEntry(entry) // add/edit entry in database
            Toast.makeText(
                this, if (entry.id == null) "Entry Added" else "Entry Updated",
                Toast.LENGTH_SHORT
            ).show()

            popupWindow.dismiss() // dismiss this popup when done
            refreshTab() // refresh to apply changes to the entries tab
        }

        backButton.setOnClickListener {
            popupWindow.dismiss() //dismiss this popup if the back button is pressed
            showEntryManagementDialog(entry) //reopen the popup window for managing an entry
        }

        popupWindow.setOnDismissListener(PopupWindow.OnDismissListener {
            unblurBackground() //if the popup is being dismissed, unblur the background
        })

        cancelButton.setOnClickListener {
            popupWindow.dismiss() //dismiss popup if the cancel button is pressed
        }

        popupWindow.showAtLocation(
            binding.root, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }

    /**
     * Method to make the screen appear blurry when a popup is shown on top of it
     */
    private fun blurBackground() {
        val lp = window.attributes
        lp.alpha = 0.5f
        window.attributes = lp
    }

    /**
     * Method to unblur the screen when a popup is dismissed
     */
    private fun unblurBackground() {
        val lp = window.attributes
        lp.alpha = 1f
        window.attributes = lp
    }

    /**
     * Method to refresh the listview in the tab currently being displayed
     */
    fun refreshTab() {
        when (currentPage) {
            MEDICATION_TAB_INDEX -> {
                val current: PillFragment =
                    fragments.get(currentPage) as PillFragment
                current.updatePillList()
            }

            ENTRIES_TAB_INDEX -> {
                val current: EntryFragment = fragments.get(currentPage) as EntryFragment
                current.updateEntryList()
            }
        }
    }

    /**
     * Method to refresh the listview in the tab containing entry listview
     */
    fun refreshEntries() {
        (fragments.get(ENTRIES_TAB_INDEX) as EntryFragment).updateEntryList()
    }

    /**
     * Method to toggle visibility of search bar
     */
    fun toggleSearchBarVisibility(view: View) {
        if (!isSearching) {
            //if the user starts searching
            binding.search.setImageResource(R.drawable.ic_cancel) // change search icon to
            // a cancel icon
            isSearching =
                !isSearching // change boolean to notify that the user is currently searching
            binding.switcher.showNext() // show search bar
            binding.appBarSearch.requestFocusFromTouch() // give search bar focus
        } else hideSearchBar()
    }

    /**
     * Method make search bar invisible
     */
    fun hideSearchBar() {
        binding.search.setImageResource(R.drawable.ic_search) //switch the app bar icon back
        // to a search icon
        binding.appBarSearch.text.clear() //clear search bar
        hideKeyboard() //stop showing input keyboard
        isSearching = !isSearching //set boolean to show user has stopped searching
        binding.switcher.showNext() //stop showing search bar
    }

    /**
     * Method to display popup window to allow the user apply filters to Entries list view
     */
    fun filterSearch(view: View) {
        if (currentPage == ENTRIES_TAB_INDEX) {
            showFilterDialog()
        }
    }

    /**
     * Method to reverse the order of the listview in the tab that's currently being viewed
     */
    fun reverseListOrder(view: View) {
        when (currentPage) {
            MEDICATION_TAB_INDEX -> {
                val current: PillFragment =
                    fragments.get(currentPage) as PillFragment
                current.reverse()
            }

            ENTRIES_TAB_INDEX -> {
                val current: EntryFragment = fragments.get(currentPage) as EntryFragment
                current.reverse()
            }
        }
    }

    /**
     * Method to stop displaying device keyboard, if it's currently being displayed
     */
    fun hideKeyboard() {
        try {
            val imm: InputMethodManager =
                this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(this.getCurrentFocus()?.getWindowToken(), 0)
        } catch (e: Exception) {
        }
    }

    /**
     * Method to toggle filter icon visibility
     *
     *  It is visible if the user is looking at entries
     *  Specifically because the medication doesn't have any additional filters
     */
    fun toggleFilterIconVisibility() {
        if (currentPage == 1)
            binding.filter.setColorFilter(ContextCompat.getColor(this, R.color.appbar_text))
        else
            binding.filter.setColorFilter(ContextCompat.getColor(this, R.color.appbar))
    }
}