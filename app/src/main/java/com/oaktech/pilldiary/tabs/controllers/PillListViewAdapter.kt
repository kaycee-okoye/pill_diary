package com.oaktech.pilldiary.tabs.controllers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.oaktech.pilldiary.Pill
import com.oaktech.pilldiary.R

/**
 * Class to configure adapter used in Pill ListView
 */
class PillListViewAdapter(
    private val pills: List<Pill>, private val listViewItemClickListener: ListViewItemClickListener
) :
    RecyclerView.Adapter<PillListViewAdapter.ViewHolder>() {

    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        val nameDisplay = itemView.findViewById<TextView>(R.id.pillEntry)
        val dosageDisplay = itemView.findViewById<TextView>(R.id.dosageEntry)
        val letterIcon = itemView.findViewById<TextView>(R.id.letterIcon)
        val trashIcon = itemView.findViewById<ImageButton>(R.id.trashIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val pillView = inflater.inflate(R.layout.pill_display, parent, false)
        return ViewHolder(pillView)
    }

    override fun getItemCount(): Int {
        return pills.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pill: Pill = pills.get(position)
        holder.nameDisplay.setText(pill.name)
        holder.dosageDisplay.setText(
            if (pill.dosage.isBlank()) "Dosage: " else "Dosage: ${pill.dosage}"
        )
        holder.letterIcon.setText(pill.name.substring(0, 1))

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