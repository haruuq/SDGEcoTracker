package com.example.sdgecotracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserManagementAdapter(
    private var users: MutableList<User>,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserManagementAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvRole: TextView = view.findViewById(R.id.tvUserRole)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditUser)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.username
        holder.tvRole.text = user.role.uppercase()
        holder.btnEdit.setOnClickListener { onEditClick(user) }
        holder.btnDelete.setOnClickListener { onDeleteClick(user) }
        
        // Ensure visibility
        holder.itemView.visibility = View.VISIBLE
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers.toMutableList()
        notifyDataSetChanged()
    }
}
