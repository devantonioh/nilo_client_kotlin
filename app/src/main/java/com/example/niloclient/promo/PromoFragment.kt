package com.example.niloclient.promo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.niloclient.R
import com.example.niloclient.databinding.FragmentPromoBinding
import com.example.niloclient.product.MainAux
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

class PromoFragment : Fragment(){
    private var binding: FragmentPromoBinding? = null

    private var mainTitle :String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPromoBinding.inflate(inflater, container, false)
        binding?.let {
            return  it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configRemoteConfig()
        configActionBar()

    }

    private fun configActionBar() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainTitle=it.supportActionBar?.title.toString()
            it.supportActionBar?.title =getString(R.string.promo_title)
        }
    }

    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener {
                if (it.isSuccessful){
                    val percentaje = remoteConfig.getDouble("percentaje")
                    val photoUrl = remoteConfig.getString("photoUrl")
                    val message = remoteConfig.getString("message")

                    binding?.let {
                        it.tvMessage.text = message
                        it.tvPercentaje.text = percentaje.toString()

                        Glide.with(this)
                            .load(photoUrl)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .placeholder(R.drawable.ic_access_time)
                            .error(R.drawable.ic_local_offer)
                            .centerCrop()
                            .into(it.imgPromo)
                    }

                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            it.supportActionBar?.title = mainTitle
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }
}