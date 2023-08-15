package com.example.niloclient.order

import com.example.niloclient.entities.Order

interface OnOrderListener {
    fun onTrack(order: Order)
    fun onStartChat(order: Order)
}