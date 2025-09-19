package com.jeff.mosbookings.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jeff.mosbookings.databinding.ItemReportCardBinding
import com.jeff.mosbookings.fragments.ReportsFragment

class ReportsAdapter(
    private var reports: List<ReportsFragment.ReportItem>
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    fun updateReports(newReports: List<ReportsFragment.ReportItem>) {
        reports = newReports
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        holder.bind(report)
    }

    override fun getItemCount(): Int = reports.size

    class ReportViewHolder(private val binding: ItemReportCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: ReportsFragment.ReportItem) {
            binding.apply {
                reportTitle.text = report.title
                reportValue.text = report.value
                reportIcon.setImageResource(report.iconRes)
            }
        }
    }
}
