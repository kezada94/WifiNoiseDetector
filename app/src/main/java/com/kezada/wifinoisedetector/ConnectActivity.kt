package com.kezada.wifinoisedetector

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest

class ConnectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)

        //RequestHandler.getInstance(applicationContext).addToRequestQueue(isConnected)
        val intent = Intent(this, GetDataActivity::class.java)
        startActivity(intent)
    }

}
