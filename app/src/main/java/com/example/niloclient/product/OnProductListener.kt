package com.example.niloclient.product

import com.example.niloclient.entities.Product

interface OnProductListener {
    fun onClick(product: Product)
    fun loadMore()

}