package com.oaktech.pilldiary.tabs.controllers

/**
 * Interface of methods used in ListView items
 */
interface ListViewItemClickListener {
    /*
        "position" is the index of the listview item that the action
        was performed on

        "item" in this file refers to the specific listview item
        that the action was performed on
     */
    var highlightedIndex: Int // index of list view item that was last selected
    fun onTrashClickListener(position: Int) // function called when item's trash icon is selected
    fun onCellClickListener(position: Int) // function called when item is tapped
    fun onCellLongClickListener(position: Int) // function called when item is long pressed
}