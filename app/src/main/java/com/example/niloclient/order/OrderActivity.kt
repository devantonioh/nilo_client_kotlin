package com.example.niloclient.order

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.niloclient.Constants
import com.example.niloclient.R
import com.example.niloclient.chat.ChatFragment
import com.example.niloclient.databinding.ActivityOrderBinding
import com.example.niloclient.entities.Order
import com.example.niloclient.track.TrackFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding

    private lateinit var adapter: OrderAdapter

    private lateinit var orderSelected : Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFirestore()

        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        intent?.let {
            val actionIntent = it.getIntExtra(Constants.ACTION_INTENT, 0)
            if ( actionIntent == 1){
            val id = intent.getStringExtra(Constants.PROP_ID) ?: ""
            val status = intent.getIntExtra(Constants.PROP_STATUS, 0)
            orderSelected = Order(id = id, status= status)

            val fragment = TrackFragment()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.containerMain, fragment)
                .addToBackStack(null)
                .commit()
            }
        }
    }


    private fun setupRecyclerView() {
        adapter = OrderAdapter(mutableListOf(), this)

        binding.recyclerView.apply {
            layoutManager =  LinearLayoutManager(this@OrderActivity)
            adapter= this@OrderActivity.adapter
        }
    }

    private fun setupFirestore() {
        FirebaseAuth.getInstance().currentUser?.let { user->
            val db = FirebaseFirestore.getInstance()

            db.collection(Constants.COLL_REQUESTS)
                //.orderBy(Constants.PROP_DATE, Query.Direction.ASCENDING)
                //.orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)
                //.whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                //.whereNotIn(Constants.PROP_STATUS, listOf(4))
                //.whereGreaterThan(Constants.PROP_STATUS, 2)
                //.whereLessThan(Constants.PROP_STATUS, 4)
                //.whereEqualTo(Constants.PROP_STATUS, 3)
                //.whereGreaterThanOrEqualTo(Constants.PROP_STATUS, 2)
                //.whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                //.orderBy(Constants.PROP_STATUS, Query.Direction.DESCENDING)
                  .whereEqualTo(Constants.PROP_CLIENT_ID, user.uid)
                  /*.orderBy(Constants.PROP_STATUS, Query.Direction.ASCENDING)
                  .whereLessThan(Constants.PROP_STATUS, 4)*/
                  .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener {
                    for (document in it){
                        val order = document.toObject(Order::class.java)
                        order.id = document.id
                        adapter.add(order)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT).show()
                }
        }

    }


    override fun onTrack(order: Order) {
        orderSelected = order

        val fragment = TrackFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()

    }

    override fun onStartChat(order: Order) {
        orderSelected = order

        val fragment = ChatFragment()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()

    }

    override fun getOrdderSelected(): Order = orderSelected
}