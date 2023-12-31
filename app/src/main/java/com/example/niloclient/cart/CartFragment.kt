package com.example.niloclient.cart

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.niloclient.Constants
import com.example.niloclient.R
import com.example.niloclient.databinding.FragmentCartBinding
import com.example.niloclient.entities.Order
import com.example.niloclient.entities.Product
import com.example.niloclient.entities.ProductOrder
import com.example.niloclient.order.OrderActivity
import com.example.niloclient.product.MainActivity
import com.example.niloclient.product.MainAux
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class CartFragment : BottomSheetDialogFragment(), OnCartListener {

    private var binding: FragmentCartBinding? = null

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private lateinit var adapter: ProductCartAdapter

    private var totalPrice = 0.0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentCartBinding.inflate(LayoutInflater.from(activity))
        binding?.let {
            val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
            bottomSheetDialog.setContentView(it.root)

            bottomSheetBehavior = BottomSheetBehavior.from(it.root.parent as View)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

            setupRecyclerView()
            setupButtons()

            getProducts()
            configAnalytics()

            return bottomSheetDialog
        }

        return super.onCreateDialog(savedInstanceState)
    }

    private fun setupRecyclerView() {
        binding?.let {
            adapter = ProductCartAdapter(mutableListOf(), this)

            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@CartFragment.adapter
            }

            /* (1..5).forEach {
                 val product = Product(it.toString(), "Producto $it", "This product is $it",
                     "", it, 2.0 * it)
                 adapter.add(product)
             }*/
        }
    }

    private fun setupButtons() {
        binding?.let {
            it.ibCancel.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            it.efab.setOnClickListener {
                //requestOrder()
                requestOrderTransaction()
            }
        }
    }

    private fun getProducts() {
        (activity as? MainAux)?.getProductsCart()?.forEach {
            adapter.add(it)
        }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.METHOD, "check_track")
        }
    }

    private fun requestOrder() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { myUser ->

            enableUI(false)
            val products = hashMapOf<String, ProductOrder>()
            adapter.getProducts().forEach { product ->
                products.put(
                    product.id!!,
                    ProductOrder(product.id!!, product.name!!, product.newQuantity)
                )
            }
            val order = Order(
                clientId = myUser.uid,
                products = products,
                totalPrice = totalPrice,
                status = 1
            )

            val db = FirebaseFirestore.getInstance()
            db.collection(Constants.COLL_REQUESTS)
                .add(order)
                .addOnSuccessListener {
                    dismiss()
                    (activity as? MainAux)?.clearCart()
                    startActivity(Intent(context, OrderActivity::class.java))

                    Toast.makeText(activity, "Compra realizada", Toast.LENGTH_SHORT).show()
                    //Analytics
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO) {
                        val products = mutableListOf<Bundle>()
                        order.products.forEach {
                            if (it.value.quantity > 5) {
                                val bundle = Bundle()
                                bundle.putString("id_product", it.key)
                                products.add(bundle)
                            }
                        }
                        param(FirebaseAnalytics.Param.QUANTITY, products.toTypedArray())

                    }
                    firebaseAnalytics.setUserProperty(
                        Constants.USER_PROP_QUANTITY,
                        if (products.size > 0) "con_mayoreo" else "sin_mayoreo"
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error Al comprar", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    enableUI(true)
                }
        }
    }

    private fun requestOrderTransaction() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { myUser ->
            enableUI(false)

            val products = hashMapOf<String, ProductOrder>()
            adapter.getProducts().forEach { product ->
                products.put(
                    product.id!!,
                    ProductOrder(product.id!!, product.name!!, product.newQuantity)
                )
            }
            val order = Order(
                clientId = myUser.uid,
                products = products,
                totalPrice = totalPrice,
                status = 1
            )
            val db = FirebaseFirestore.getInstance()

            val requestDoc = db.collection(Constants.COLL_REQUESTS).document()
            val productsRef = db.collection(Constants.COLL_PRODUCTS)

            db.runBatch { batch ->
                batch.set(requestDoc, order)

                order.products.forEach {
                    batch.update(productsRef.document(it.key),
                        Constants.PROP_QUANTITY,
                        FieldValue.increment(-it.value.quantity.toLong()))
                }
            }
                .addOnSuccessListener {
                    dismiss()
                    (activity as? MainAux)?.clearCart()
                    startActivity(Intent(context, OrderActivity::class.java))

                    Toast.makeText(activity, "Compra realizada", Toast.LENGTH_SHORT).show()
                    //Analytics
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO) {
                        val products = mutableListOf<Bundle>()
                        order.products.forEach {
                            if (it.value.quantity > 5) {
                                val bundle = Bundle()
                                bundle.putString("id_product", it.key)
                                products.add(bundle)
                            }
                        }
                        param(FirebaseAnalytics.Param.QUANTITY, products.toTypedArray())

                    }
                    firebaseAnalytics.setUserProperty(
                        Constants.USER_PROP_QUANTITY,
                        if (products.size > 0) "con_mayoreo" else "sin_mayoreo"
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error Al comprar", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    enableUI(true)
                }
        }
    }

    private fun enableUI(enable: Boolean) {
        binding?.let {
            it.ibCancel.isEnabled = enable
            it.efab.isEnabled = enable
        }
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.updateTotal()
        super.onDestroyView()
        binding = null
    }

    override fun setQuantity(product: Product) {
        adapter.update(product)

    }

    override fun showTotal(total: Double) {
        totalPrice = total
        binding?.let {
            it.tvTotal.text = getString(R.string.product_full_cart, total)
        }
    }

}