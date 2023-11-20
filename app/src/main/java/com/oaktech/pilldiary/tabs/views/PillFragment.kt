package com.oaktech.pilldiary.tabs.views

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.oaktech.pilldiary.Entry
import com.oaktech.pilldiary.tabs.controllers.PillListViewAdapter
import com.oaktech.pilldiary.Pill
import com.oaktech.pilldiary.tabs.controllers.PillBase
import com.oaktech.pilldiary.tabs.controllers.ListViewItemClickListener
import com.oaktech.pilldiary.databinding.MedicationBinding
import com.oaktech.pilldiary.tabs.MainActivity
import kotlin.collections.ArrayList

class PillFragment : Fragment(), ListViewItemClickListener {
    override var highlightedIndex: Int = -1 // index of last selected item in listview

    // set to -1 if not item has been selected
    private var pillList: ArrayList<Pill> = ArrayList() // list of medications that will

    // be populated from database
    private var nameFilter: String = "" // filter medication list based on name
    private var isReversed: Boolean =
        false // whether to show the list in ascending/descending order

    lateinit private var binding: MedicationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MedicationBinding.inflate(inflater, container, false)
        updatePillList() // populate pill list from database and apply filters
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        updatePillList() // refresh pill list from database
    }

    /**
     * Method to confirm & implement pill deletion
     *
     * @param position index of pill in pill list
     */
    fun delete(position: Int) {
        // show popup to delete item at specific index in pill list from database
        val item = pillList.get(position) as Pill
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Medication")
        builder.setMessage("Are you sure you want to delete this medication?")

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val base: PillBase = PillBase(context)
                    base.deletePill(item)
                    Toast.makeText(context, "Medication succesfully deleted", Toast.LENGTH_SHORT)
                        .show()
                    updatePillList()
                }
            }
        }
        builder.setPositiveButton("Yes", dialogClickListener)
        builder.setNegativeButton("No", dialogClickListener)
        builder.create().show()
    }

    /**
     * Method to populate pill list from database
     */
    fun updatePillList() {
        val base: PillBase = PillBase(context) // initialize database handler class
        val listView = binding.listDisplay
        pillList = base.getPills() //get all pills
        if (pillList.isNotEmpty()) {
            // apply filter for entry name
            if (!nameFilter.isBlank()) {
                // if user has added a filter
                pillList = pillList.filter { p: Pill ->
                    p.name.lowercase().contains(nameFilter.lowercase())
                } as ArrayList<Pill> //filter based on name
            }

            if (isReversed) {
                pillList.sortByDescending { pill -> pill } //sort in descending order
                // if the user applied the filter
            } else {
                pillList.sortBy { pill -> pill } //sort in ascending order of medication name
            }
        }
        val adapter = PillListViewAdapter(pillList, this)
        listView.setAdapter(adapter)
        listView.layoutManager = LinearLayoutManager(context)
    }

    /**
     * Method to delete entry at given index if its trash icon is clicked
     *
     * @param position index of pill in pill list
     */
    override fun onTrashClickListener(position: Int) {
        delete(position)
    }

    /**
     * Method to create automatically generated entry for pill administration
     *
     * This will state that a given pill was administered
     *
     * @param position index of administered pill in pill list
     */
    override fun onCellClickListener(position: Int) {
        val item = pillList.get(position) as Pill
        item.dosage = if (item.dosage.isBlank()) "" else item.dosage
        val base = PillBase(context)
        base.upsertEntry(Entry(item))
        Toast.makeText(context, Entry(item).entryMessage, Toast.LENGTH_SHORT).show()
        val home = activity as MainActivity
        home.refreshEntries()
    }

    /**
     * Method to edit pill at given index when it is long pressed
     *
     * @param position index of administered pill in pill list
     */
    override fun onCellLongClickListener(position: Int) {
        val pill = pillList.get(position) as Pill
        val mainActivity = activity as MainActivity
        mainActivity.showPillManagementDialog(pill)
    }

    /**
     * Method to apply filter to list view
     *
     * @param name names of medications in pill list will be filtered based on this
     */
    fun setFilter(name: String) {
        nameFilter = name
        updatePillList()  //refresh pill list from database
    }

    /**
     * Method to reverse sorting order of list view
     */
    fun reverse() {
        //reverse sorting order
        isReversed = !isReversed
        updatePillList() //refresh pill list from database
    }
}