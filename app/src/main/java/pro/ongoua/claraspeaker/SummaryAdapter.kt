package pro.ongoua.claraspeaker

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class SummaryAdapter(
    private val onPlayClicked: (Summary) -> Unit,
    private val onDeleteClicked: (Summary) -> Unit
) : ListAdapter<Summary, SummaryAdapter.SummaryViewHolder>(DiffCallback) {

    private var playingId: Int? = null
    private var loadingId: Int? = null
    private val expandedIds = mutableSetOf<Int>()

    fun setPlayingId(id: Int?) {
        if (id == playingId) return
        val previous = playingId
        playingId = id
        notifyForId(previous)
        notifyForId(id)
    }

    fun setLoadingId(id: Int?) {
        if (id == loadingId) return
        val previous = loadingId
        loadingId = id
        notifyForId(previous)
        notifyForId(id)
    }

    private fun notifyForId(id: Int?) {
        if (id == null) return
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) notifyItemChanged(index)
    }

    private fun toggleExpanded(summary: Summary) {
        if (!expandedIds.add(summary.id)) expandedIds.remove(summary.id)
        notifyForId(summary.id)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val summary = getItem(position)
        holder.bind(summary, playingId, loadingId, expandedIds.contains(summary.id))
        // Le clic sur la carte replie/déplie le texte.
        holder.itemView.setOnClickListener { toggleExpanded(summary) }
        holder.playButton.setOnClickListener { onPlayClicked(summary) }
        holder.deleteButton.setOnClickListener { onDeleteClicked(summary) }
    }

    class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView as MaterialCardView
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.textViewDateTime)
        private val badgeTextView: TextView = itemView.findViewById(R.id.textViewBadge)
        private val summaryTextView: TextView = itemView.findViewById(R.id.textViewSummary)
        private val voiceModelTextView: TextView = itemView.findViewById(R.id.textViewVoiceModel)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val playButton: ImageButton = itemView.findViewById(R.id.playButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        private val strokePx = (2 * itemView.resources.displayMetrics.density).toInt()

        fun bind(summary: Summary, playingId: Int?, loadingId: Int?, expanded: Boolean) {
            val context = itemView.context

            dateTimeTextView.text = DateUtils.getRelativeTimeSpanString(
                summary.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )

            val pending = summary.audioFilePath == null
            badgeTextView.text = context.getString(
                if (pending) R.string.summary_status_pending else R.string.summary_status_played
            )

            summaryTextView.text = summary.text
            summaryTextView.maxLines = if (expanded) Integer.MAX_VALUE else 3

            if (summary.voiceModel != null) {
                voiceModelTextView.visibility = View.VISIBLE
                voiceModelTextView.text = context.getString(R.string.summary_voice, summary.voiceModel)
            } else {
                voiceModelTextView.visibility = View.GONE
            }

            val isLoading = loadingId == summary.id
            val isPlaying = playingId == summary.id
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            // INVISIBLE (pas GONE) : le bouton garde sa place, sinon ses ancres se
            // replient et le spinner remonte se superposer au texte.
            playButton.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
            playButton.setImageResource(if (isPlaying) R.drawable.ic_stop_24 else R.drawable.ic_play_24)
            playButton.contentDescription =
                context.getString(if (isPlaying) R.string.summary_stop else R.string.summary_play)

            // Surbrillance de la carte en cours de lecture.
            card.strokeWidth = if (isPlaying) strokePx else 0
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
