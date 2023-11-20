package com.oaktech.pilldiary.tabs.controllers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oaktech.pilldiary.Entry
import com.oaktech.pilldiary.R

/**
 * Class to configure adapter used in Entry ListView
 */
class EntryListViewAdapter(
    private val entries: ArrayList<Entry>,
    private val listViewItemClickListener: ListViewItemClickListener
) :
    RecyclerView.Adapter<EntryListViewAdapter.ViewHolder>() {

    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        val dateInput = itemView.findViewById<TextView>(R.id.dateEntry)
        val entryMessageInput = itemView.findViewById<TextView>(R.id.entryEntry)
        val trashIcon = itemView.findViewById<ImageButton>(R.id.trashIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val entryView = inflater.inflate(R.layout.list_display, parent, false)
        return ViewHolder(entryView)
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val max: Int = 45 // maximum number of characters of entry text that are displayed
        val entry: Entry = entries.get(position)

        holder.dateInput.setText(entry.displayDate())

        // enforce maximum number of characters if this entry is not currently highlighted
        val entryMessage = if (
            entry.entryMessage.length > max && position != listViewItemClickListener.highlightedIndex)
            entry.entryMessage.substring(0, max - 3) + "..." else entry.entryMessage
        holder.entryMessageInput.setText(entryMessage)

        holder.trashIcon.setOnClickListener {
            listViewItemClickListener.onTrashClickListener(position)
        }
        holder.itemView.setOnClickListener {
            listViewItemClickListener.onCellClickListener(position)
        }
        holder.itemView.setOnLongClickListener {
            listViewItemClickListener.onCellLongClickListener(position)
            true
        }
    }
}