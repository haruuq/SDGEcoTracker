package com.example.sdgecotracker

/*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// This class is currently unused and references missing models. 
// It has been commented out to allow the project to build.

class EcoActivityAdapter(
    private var activities: List<EcoActivity>,
    private val onItemClick: (EcoActivity) -> Unit,
    private val onDeleteClick: (EcoActivity) -> Unit
) : RecyclerView.Adapter<EcoActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_eco_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.tvTitle.text = activity.title
        holder.tvDescription.text = activity.description
        holder.tvTimestamp.text = activity.timestamp
        
        holder.itemView.setOnClickListener { onItemClick(activity) }
        holder.btnDelete.setOnClickListener { onDeleteClick(activity) }
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<EcoActivity>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}
*/
