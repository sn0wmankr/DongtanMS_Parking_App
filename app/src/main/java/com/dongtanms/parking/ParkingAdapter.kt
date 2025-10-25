package com.dongtanms.parking

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ParkingAdapter(
    private var items: List<ParkingEntry>,
    private val onDelete: (ParkingEntry) -> Unit,
    private val onDone: (ParkingEntry) -> Unit
) : RecyclerView.Adapter<ParkingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val btnDone: Button = view.findViewById(R.id.btnDone)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvNumber.text = item.plateNumber
        val done = item.status == "done"

        holder.tvStatus.text = if (done) "✅ 완료" else "🟡 대기중"
        holder.tvNumber.alpha = if (done) 0.5f else 1f
        holder.btnDone.isEnabled = !done

        holder.btnDone.setOnClickListener { onDone(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }

        if (holder.itemView.alpha == 0f) {
            holder.itemView.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }

        val targetAlpha = if (done) 0.5f else 1f
        holder.itemView.animate().alpha(targetAlpha).setDuration(400).start()

        val textColor = if (done) Color.GRAY else Color.BLACK
        holder.tvNumber.setTextColor(textColor)
    }


    override fun getItemCount(): Int = items.size

    fun updateData(newList: List<ParkingEntry>) {
        val oldSize = items.size
        items = newList
        notifyItemRangeChanged(0, oldSize.coerceAtLeast(newList.size))
    }
}
