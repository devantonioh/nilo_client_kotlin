package com.example.niloclient.product

import com.example.niloclient.entities.Product
import com.google.firebase.auth.FirebaseUser

interface MainAux {
    fun getProductsCart() : MutableList<Product>
    fun updateTotal()
    fun clearCart()

    fun getProductsSelected (): Product?
    fun showButton(isVisible: Boolean)
    fun addProductToCart(product: Product)

    fun updateTitle(user: FirebaseUser)
}