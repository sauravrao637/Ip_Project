package com.camo.ip_project.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.camo.ip_project.database.local.model.UserHRV
import com.camo.ip_project.databinding.LayoutUserHrvDataBinding
import com.camo.ip_project.databinding.ResultLayoutBinding
import java.util.*

class HrvDataRVAdapter(private var list: List<UserHRV>): RecyclerView.Adapter<HrvDataRVAdapter.ViewHolder>() {

    class ViewHolder(val binding: LayoutUserHrvDataBinding): RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutUserHrvDataBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = list[position]
        holder.binding.apply {
            iResult.tvBpm.text = data.heartRate.toString()
            iResult.tvNni.text = data.nni.toString()
            iResult.tvSdnn.text = data.sdnn.toString()
            iResult.tvRmssd.text = data.rmssd.toString()
            tvUsername.text = data.userName
            tvTimestamp.text = Date(data.unixTimestamp).toString()
        }
    }

    fun updateData(newList: List<UserHRV>){
        list = newList
        notifyDataSetChanged()
    }
}