package com.example.niloclient.order

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.niloclient.R
import com.example.niloclient.databinding.ItemOrderBinding
import com.example.niloclient.entities.Order

class OrderAdapter(private val orderList: MutableList<Order>, private  val listener: OnOrderListener)
    : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    private lateinit var context : Context

    private val aValues : Array<String> by lazy {
        context.resources.getStringArray(R.array.status_value)
    }
    private val aKeys : Array<Int> by lazy {
        context.resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orderList[position]

        holder.setListener(order)

        holder.binding.tvId.text = context.getString(
            R.string.order_id, order.id
        )

        var names = ""
        order.products.forEach {
            names += "${it.value.name}, "
        }
        holder.binding.tvProductNames.text = names.dropLast(2)

        holder.binding.tvTotalPrice.text = context.getString(R.string.product_full_cart, order.totalPrice)

        val index = aKeys.indexOf(order.status)
        val statusStr = if (index != -1) aValues[index] else context.getString(R.string.order_status_unknown)

        holder.binding.tvStatus.text = context.getString(R.string.order_status, statusStr)
    }

    override fun getItemCount(): Int = orderList.size

    fun add(order: Order){
        orderList.add(order)
        notifyItemInserted(orderList.size -1)
    }

    inner class  ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val binding = ItemOrderBinding.bind(view)

        fun setListener(order: Order){
            binding.btnTrack.setOnClickListener {
                listener.onTrack(order)
            }
            binding.chpChat.setOnClickListener {
                listener.onStartChat(order)
            }
        }
    }
}