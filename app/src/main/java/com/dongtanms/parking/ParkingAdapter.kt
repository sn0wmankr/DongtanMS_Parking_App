package com.dongtanms.parking

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_TYPE_DATE = 0
private const val VIEW_TYPE_ENTRY = 1

class ParkingAdapter(
    private val onDelete: (ParkingEntry) -> Unit,
    private val onDone: (ParkingEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ListItem>()

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.DateItem -> VIEW_TYPE_DATE
            is ListItem.EntryItem -> VIEW_TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
                DateViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_parking, parent, false)
                EntryViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.DateItem -> (holder as DateViewHolder).bind(item)
            is ListItem.EntryItem -> (holder as EntryViewHolder).bind(item.parkingEntry)
        }
    }

    override fun getItemCount(): Int = items.size

    fun removeItem(entry: ParkingEntry) {
        val position = items.indexOfFirst { it is ListItem.EntryItem && it.parkingEntry.id == entry.id }
        if (position > -1) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateData(newList: List<ListItem>) {
        val diffResult = DiffUtil.calculateDiff(ListItemDiffCallback(this.items, newList))
        this.items.clear()
        this.items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        private val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val btnDone: Button = view.findViewById(R.id.btnDone)
        private val btnDelete: Button = view.findViewById(R.id.btnDelete)
        private val timeFormat = SimpleDateFormat("(HH:mm)", Locale.KOREAN)

        fun bind(item: ParkingEntry) {
            tvNumber.text = item.plateNumber
            tvTimestamp.text = timeFormat.format(Date(item.createdAt))
            val done = item.status == "done"

            tvStatus.text = if (done) "✅ 완료" else "🟡 대기중"
            itemView.alpha = if (done) 0.5f else 1f
            btnDone.isEnabled = !done
            tvNumber.setTextColor(if (done) Color.GRAY else Color.BLACK)

            btnDone.setOnClickListener { onDone(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDateHeader: TextView = view.findViewById(R.id.tvDateHeader)

        fun bind(item: ListItem.DateItem) {
            tvDateHeader.text = item.date
        }
    }

    class ListItemDiffCallback(private val oldList: List<ListItem>, private val newList: List<ListItem>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old == new
        }
    }
}
