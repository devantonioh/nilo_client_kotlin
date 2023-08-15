package com.example.niloclient.cart

import com.example.niloclient.entities.Product

interface OnCartListener {
    fun setQuantity(product: Product)
    fun showTotal(total: Double)

}