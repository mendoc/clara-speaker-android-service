package pro.ongoua.claraspeaker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.widget.Button

class SummaryAdapter(private val onItemClicked: (Summary) -> Unit, private val onDeleteClicked: (Summary) -> Unit) :
    ListAdapter<Summary, SummaryAdapter.SummaryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val summary = getItem(position)
        holder.bind(summary)
        holder.itemView.setOnClickListener {
            onItemClicked(summary)
        }
        holder.deleteButton.setOnClickListener {
            onDeleteClicked(summary)
        }
    }

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.textViewDateTime)
        private val summaryTextView: TextView = itemView.findViewById(R.id.textViewSummary)
        private val voiceModelTextView: TextView = itemView.findViewById(R.id.textViewVoiceModel)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        private val dateFormatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.FRENCH)

        fun bind(summary: Summary) {
            dateTimeTextView.text = dateFormatter.format(Date(summary.createdAt))
            summaryTextView.text = summary.text
            voiceModelTextView.text = "Voix: ${summary.voiceModel ?: "Inconnue"}"
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Summary>() {
            override fun areItemsTheSame(oldItem: Summary, newItem: Summary): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Summary, newItem: Summary): Boolean {
                return oldItem == newItem
            }
        }
    }
}