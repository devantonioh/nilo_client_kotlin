package com.example.niloclient.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.niloclient.Constants
import com.example.niloclient.R
import com.example.niloclient.databinding.FragmentDetailBinding
import com.example.niloclient.entities.Product
import com.example.niloclient.product.MainAux
import com.google.firebase.storage.FirebaseStorage

class DetailFragment : Fragment() {
    private var binding: FragmentDetailBinding? = null
    private var product: Product? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        binding?.let {
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getProuct()
        setupButtons()
    }



    private fun getProuct() {
        product = (activity as? MainAux)?.getProductsSelected()
        product?.let {product ->
            binding?.let {
                it.tvName.text = product.name
                it.tvDescription.text=product.description
                it.tvQuantity.text= getString(R.string.detail_quantity, product.quantity)
                setNewQuantity(product)
                
                Glide.with(this)
                    .load(product.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_access_time)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .into(it.imgProduct)
            /*    context?.let { context ->
                    val productRef = FirebaseStorage.getInstance().reference
                        .child(product.sellerId)
                        .child(Constants.PATH_PRODUCT_IMAGES)
                        .child(product.id!!)

                    productRef.listAll()
                        .addOnSuccessListener { imgList ->
                            val detailAdapter = DetailAdapter(imgList.items, context)
                            it.vpProduct.apply {
                                adapter = detailAdapter
                            }
                        }
                }*/

            }
        }
    }

    private fun setNewQuantity(product: Product) {
        binding?.let {
            it.etNewQuantity.setText(product.newQuantity.toString())
            val newQuantityStr = getString(R.string.detail_total_price, product.totalPrice(),
                product.newQuantity, product.price)
            it.tvTotalPrice.text= HtmlCompat.fromHtml(newQuantityStr, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun setupButtons() {
        product?.let { product ->
            binding?.let {binding ->
                binding.ibSub.setOnClickListener {
                    if (product.newQuantity > 1){
                        product.newQuantity -= 1
                        setNewQuantity(product)
                    }
                }
                binding.ibSum.setOnClickListener {
                    if (product.newQuantity < product.quantity){
                        product.newQuantity += 1
                        setNewQuantity(product)
                    }
                }
                binding.efab.setOnClickListener {
                    product.newQuantity = binding.etNewQuantity.text.toString().toInt()
                    addToCart(product)
                }
            }
        }
    }

    //onBackPressed()  is deprecated
    private fun addToCart(product: Product) {
        (activity as? MainAux)?.let {
            it.addProductToCart(product)
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }
}