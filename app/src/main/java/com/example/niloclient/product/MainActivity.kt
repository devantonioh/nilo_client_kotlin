package com.example.niloclient.product

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.niloclient.Constants
import com.example.niloclient.R
import com.example.niloclient.cart.CartFragment
import com.example.niloclient.databinding.ActivityMainBinding
import com.example.niloclient.detail.DetailFragment
import com.example.niloclient.entities.Product
import com.example.niloclient.order.OrderActivity
import com.example.niloclient.profile.ProfileFragment
import com.example.niloclient.promo.PromoFragment
import com.example.niloclient.settings.SettingsActivity
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import java.security.MessageDigest

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var binding: ActivityMainBinding

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener: ListenerRegistration

    private var queryPagination: Query? = null

    private var productSelected: Product? = null

    private val productCartList = mutableListOf<Product>()

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val response = IdpResponse.fromResultIntent(it.data)

            if (it.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()

                    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                    val token = preferences.getString(Constants.PROP_TOKEN, null)

                    token?.let {
                        val db = FirebaseFirestore.getInstance()
                        val tokenMap = hashMapOf(Pair(Constants.PROP_TOKEN, token))

                        db.collection(Constants.COLL_USERS)
                            .document(user.uid)
                            .collection(Constants.COLL_TOKENS)
                            .add(tokenMap)
                            .addOnSuccessListener {
                                Log.i("registered token", token)
                                /*preferences.edit {
                                    putString(Constants.PROP_TOKEN, null)
                                        .apply()
                                }*/
                            }
                            .addOnFailureListener {
                                Log.i("No registered token", token)
                            }
                    }
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, "Hasta pronto", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    response.error?.let {
                        if (it.errorCode == ErrorCodes.NO_NETWORK) {
                            Toast.makeText(this, "Sin red", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                this, "Código de error: ${it.errorCode}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configToolbar()
        configRemoteConfig()
        configAuth()
        configRecyclerView()
        configButtons()
        configAnalytics()

        //fcm
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful){
                val token = task.result
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                preferences.edit {
                    putString(Constants.PROP_TOKEN, token)
                        .apply()
                }
                Log.i("get token", token.toString())
            }else{
                Log.i("get token fail", task.exception.toString())
            }
        }

    }

    private fun configToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 3600 = 1s*60s*60m = 1h
            minimumFetchIntervalInSeconds = 5
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate()
                //quitar en un futuro
            .addOnSuccessListener {
                Snackbar.make(binding.root, "Datos locales / remotos", Snackbar.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "Datos loclaes", Snackbar.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                if (it.isSuccessful){
                    val isPromoDay = remoteConfig.getBoolean("isPromoDay")
                    val promCounter = remoteConfig.getLong("promCounter")
                    val percentaje = remoteConfig.getDouble("percentaje")
                    val photoUrl = remoteConfig.getString("photoUrl")
                    val message = remoteConfig.getString("message")

                    if (isPromoDay){
                       // Snackbar.make(binding.root, "Hay promocion", Snackbar.LENGTH_SHORT).show()
                        val badge = BadgeDrawable.create(this)
                        BadgeUtils.attachBadgeDrawable(badge, binding.toolbar, R.id.action_promo)
                        badge.number = promCounter.toInt()

                    }
                }
            }
    }

    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                //supportActionBar?.title = auth.currentUser?.displayName
                updateTitle(auth.currentUser!!)
                binding.llProgress.visibility = View.GONE
                binding.nsvProducts.visibility = View.VISIBLE
            } else {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.FacebookBuilder().build(),
                    AuthUI.IdpConfig.PhoneBuilder().build()
                )
                val loginView = AuthMethodPickerLayout
                    .Builder(R.layout.view_login)
                    .setEmailButtonId(R.id.btnEmail)
                    .setGoogleButtonId(R.id.btnGoogle)
                    .setFacebookButtonId(R.id.btnFacebook)
                    .setPhoneButtonId(R.id.btnPhone)
                    .setTosAndPrivacyPolicyId(R.id.tvPolicy)
                    .build()

                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .setTosAndPrivacyPolicyUrls(
                            "https://www.facebook.com/cursosAndroidAnt",//prueba para que funciones
                            "https://www.facebook.com/cursosAndroidAnt"//los terminos y condiciones
                        )
                        .setAuthMethodPickerLayout(loginView)
                        .setTheme(R.style.LoginTheme)
                        .build()
                )
            }
        }
        // Hashes Key:  HTC2VOLtHjy5ThQCECmAxdoShJQ=
        //de facebook login
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = getPackageManager().getPackageInfo(
                    "com.example.niloclient",
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                for (signature in info.signingInfo.apkContentsSigners) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d(
                        "API >= 28 KeyHash:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT)
                    );
                }
            } else {
                val info = getPackageManager().getPackageInfo(
                    "com.example.niloclient",
                    PackageManager.GET_SIGNATURES
                );
                for (signature in info.signatures) {
                    val md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    Log.d(
                        "API < 28 KeyHash:",
                        Base64.encodeToString(md.digest(), Base64.DEFAULT)
                    );
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
    }

    private fun configRecyclerView() {
        adapter = ProductAdapter(mutableListOf(Product()), this)
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(
                this@MainActivity, 3,
                GridLayoutManager.HORIZONTAL, false
            )
            adapter = this@MainActivity.adapter
        }

    }

    private fun configButtons() {
        binding.btnViewCart.setOnClickListener {
            val fragment = CartFragment()
            fragment.show(
                supportFragmentManager.beginTransaction(),
                CartFragment::class.java.simpleName
            )
        }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> {
                AuthUI.getInstance().signOut(this)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sesión terminada.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            binding.nsvProducts.visibility = View.GONE
                            binding.llProgress.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(this, "No se pudo cerrar la sesión.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            }

            R.id.action_order_history -> startActivity(Intent(this, OrderActivity::class.java))

            R.id.action_profile -> {
                val fragment = ProfileFragment()
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.containerMain, fragment)
                    .addToBackStack(null)
                    .commit()

                showButton(false)
            }
            R.id.action_settings ->{
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.action_promo ->{
                val fragment = PromoFragment()
                supportFragmentManager
                    .beginTransaction()
                    .add(R.id.containerMain, fragment)
                    .addToBackStack(null)
                    .commit()

                showButton(false)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configFirestoreRealtime() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)

        firestoreListener = productRef
            .limit(6)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
                 }
                snapshots?.let { items ->
                    val lastItem = items.documents[items.size() -1]
                    queryPagination = productRef
                        .startAfter(lastItem)
                        .limit(6)

                    for (snapshot in snapshots!!.documentChanges) {
                        val product = snapshot.document.toObject(Product::class.java)
                        product.id = snapshot.document.id
                        when (snapshot.type) {
                            DocumentChange.Type.ADDED -> adapter.add(product)
                            DocumentChange.Type.MODIFIED -> adapter.update(product)
                            DocumentChange.Type.REMOVED -> adapter.delete(product)
                        }
                    }
                }
        }
    }

    override fun onClick(product: Product) {
        val index = productCartList.indexOf(product)
        if (index != -1) {
            productSelected = productCartList[index]
        } else {
            productSelected = product
        }

        val fragment = DetailFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()

        showButton(false)
        //Analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, product.id!!)
            param(FirebaseAnalytics.Param.ITEM_NAME, product.name!!)
        }
    }

    override fun getProductsCart(): MutableList<Product> = productCartList

    override fun getProductsSelected(): Product? = productSelected

    override fun showButton(isVisible: Boolean) {
        binding.btnViewCart.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun addProductToCart(product: Product) {
        val index = productCartList.indexOf(product)
        if (index != -1) {
            productCartList.set(index, product)
        } else {
            productCartList.add(product)
        }
        updateTotal()

    }

    override fun updateTotal() {
        var total = 0.0
        productCartList.forEach { product ->
            total += product.totalPrice()
        }
        if (total == 0.0) {
            binding.tvTotal.text = getString(R.string.product_empty_cart)
        } else {
            binding.tvTotal.text = getString(R.string.product_full_cart, total)
        }
    }

    override fun clearCart() {
        productCartList.clear()
    }


    override fun updateTitle(user: FirebaseUser) {
        supportActionBar?.title = user.displayName
    }

    override fun loadMore() {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTS)

        queryPagination?.let {
            it.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                snapshots?.let { items ->
                    val lastItem = items.documents[items.size() - 1]
                    queryPagination = productRef
                        .startAfter(lastItem)
                        .limit(6)

                    for (snapshot in snapshots!!.documentChanges) {
                        val product = snapshot.document.toObject(Product::class.java)
                        product.id = snapshot.document.id
                        when (snapshot.type) {
                            DocumentChange.Type.ADDED -> adapter.add(product)
                            DocumentChange.Type.MODIFIED -> adapter.update(product)
                            DocumentChange.Type.REMOVED -> adapter.delete(product)
                        }
                    }
                }
            }
        }

    }
}