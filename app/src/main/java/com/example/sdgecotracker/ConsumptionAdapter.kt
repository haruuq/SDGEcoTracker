package com.example.sdgecotracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ConsumptionAdapter(
    private var entries: List<EcoEntry>,
    private val onEditClick: (EcoEntry) -> Unit,
    private val onDeleteClick: (EcoEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    // Flattened list: either a String (date header) or EcoEntry
    private var displayItems: List<Any> = emptyList()

    init { buildDisplayItems() }

    private fun buildDisplayItems() {
        val grouped = entries.groupBy { it.date }
        val flat = mutableListOf<Any>()
        grouped.forEach { (date, list) ->
            flat.add(date) // header
            flat.addAll(list)
        }
        displayItems = flat
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDateHeader)
        val tvAvgScore: TextView = view.findViewById(R.id.tvDateAvgScore)
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDetails: TextView = view.findViewById(R.id.tvEntryDetails)
        val tvScore: TextView = view.findViewById(R.id.tvEntryScore)
        val ivEdit: ImageView = view.findViewById(R.id.ivEdit)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun getItemViewType(position: Int): Int {
        return if (displayItems[position] is String) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_consumption, parent, false)
            ItemViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val date = displayItems[position] as String
                holder.tvDate.text = date
                // Compute avg score for this date
                val dayEntries = entries.filter { it.date == date }
                val avg = if (dayEntries.isNotEmpty()) dayEntries.map { it.score }.average().toInt() else 0
                holder.tvAvgScore.text = "Avg Score: $avg"
            }
            is ItemViewHolder -> {
                val entry = displayItems[position] as EcoEntry
                holder.tvDetails.text = "${entry.category}  •  ${entry.item}  •  ${entry.quantity}"
                holder.tvScore.text = "Score: ${entry.score}"
                val scoreColor = when {
                    entry.score >= 70 -> Color.parseColor("#347414")
                    entry.score >= 45 -> Color.parseColor("#F57F17")
                    else -> Color.parseColor("#D32F2F")
                }
                holder.tvScore.setTextColor(scoreColor)
                holder.ivEdit.setOnClickListener { onEditClick(entry) }
                holder.ivDelete.setOnClickListener { onDeleteClick(entry) }
            }
        }
    }

    override fun getItemCount() = displayItems.size

    fun updateData(newEntries: List<EcoEntry>) {
        entries = newEntries
        buildDisplayItems()
        notifyDataSetChanged()
    }
}
