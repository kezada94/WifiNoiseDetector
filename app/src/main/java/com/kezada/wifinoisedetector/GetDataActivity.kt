package com.kezada.wifinoisedetector

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_get_data.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class GetDataActivity : AppCompatActivity() {

    private val requestCode : Int = 1

    private val startButton : Button? by lazy {
        findViewById<Button>(R.id.buttonStart)
    }
    private val stopButton : Button? by lazy {
        findViewById<Button>(R.id.buttonStop)
    }
    private val textView : TextView? by lazy {
        findViewById<TextView>(R.id.textView2)
    }
    private val mapView : MapView? by lazy {
        findViewById<MapView>(R.id.mapView)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (wifi!!.startScan()) {
                wifiList = wifi!!.scanResults
            }
            netCount = wifiList?.size ?: 0

            print(tickCount.toString())

            if (netCount != 0) {
                tickCount = tickCount!! + 1
                for (i in 0..netCount - 1) {
                    listaRedes.add(Red(wifiList!![i].SSID, wifiList!![i].BSSID, wifiList!![i].level, wifiList!![i].frequency))
                    if (tickCount!! >= 20) {
                        stopButton?.callOnClick()
                        calcularPromedios()
                        ready = true
                    }
                }
            }
        }
    }
    private val locationRequest = LocationRequest().apply {
        interval = 7000
        fastestInterval = 4000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private lateinit var mapReadyCallback: OnMapReadyCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var mLocation : Location? = null
    private var wifiList: List<ScanResult>? = null
    private var wifi: WifiManager? = null
    private var netCount : Int = 0
    private var tickCount : Int? = 0
    private var googleMap : GoogleMap? = null
    private var listaRedes : ArrayList<Red> = ArrayList()
    private var listaPromedios : ArrayList<Red> = ArrayList()
    private var ready : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_data)
        stopButton?.isEnabled = false

        startButton?.setOnClickListener({ view ->
            registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
            stopButton?.isEnabled = true
            startButton?.isEnabled = false
        })

        stopButton?.setOnClickListener({ view ->
            unregisterReceiver(receiver)
            stopButton?.isEnabled = false
            startButton?.isEnabled = true
            tickCount = 0
            textView?.text = "0"
        })
        wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE), requestCode)
            }

            if (checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE), requestCode)
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    mLocation = location
                    val lat = mLocation?.latitude ?: .0
                    val lon = mLocation?.longitude ?: .0
                    val latLng  = LatLng(lat, lon)

                    googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    googleMap?.clear()
                    googleMap?.addMarker(MarkerOptions().position(latLng).title(tickCount?.toString()).draggable(false).visible(true))

                    if(ready){
                        parseJSONData()
                    }
                }
            }
        }
        mapReadyCallback = object : OnMapReadyCallback {
            override fun onMapReady(p0: GoogleMap?) {
                googleMap = p0
                googleMap?.setMinZoomPreference(16f)
                val lat = mLocation?.latitude ?: .0
                val lon = mLocation?.longitude ?: .0
                val latLng  = LatLng(lat, lon)
                googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }
        textView2?.text = "0"


        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(mapReadyCallback)
    }

    private fun calcularPromedios() {
        listaRedes.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER, { it.ssid.toString() }))
        var lastSsid: String? = null
        var count = -1
        var count2 = 2
        for (red in listaRedes){
            if (!lastSsid.equals(red.mac)){

                lastSsid = red.mac
                val redloca = Red(red.ssid, red.mac, red.rssi, red.frequency)
                listaPromedios.add(redloca)
                count2 = 2
                count++

            } else {
                listaPromedios[count].rssi = listaPromedios[count].rssi + (1/count2)*(red.rssi - listaPromedios[count].rssi)
                count2++
            }

        }
        listaRedes.clear()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            requestCode -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permisos adquiridos", Toast.LENGTH_SHORT).show()
                } else {
                    finish()
                }
                return;
            }
        }
    }

    private fun parseJSONData() {
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss", Locale.US)
        val currentDate = sdf.format(Date())

        var jsonString = "{" +
                "\"redes\" : ["
        for (red in listaPromedios){
            jsonString += "{ \"ssid\" : \"" + red.ssid + "\"," +
                    "\"mac\" : \"" + red.mac + "\"," +
                    "\"rssi\" : " + red.rssi + "," +
                    "\"frecuencia\" : " + red.frequency + "},"
        }
        jsonString = jsonString.substring(0, jsonString.length-1)
        jsonString += "], \"latitud\" : " + mLocation?.latitude + "," +
                "\"longitud\" : " + mLocation?.longitude + "," +
                "\"fecha\" : \"" + currentDate + "\"}"



        val json = JSONObject(jsonString)
        val jsonRequest = object : JsonObjectRequest(Request.Method.POST, "http://pidb.ddns.net/json.php", json,
                Response.Listener<JSONObject> { response ->
                    Toast.makeText(this, "Se envio correctamente", Toast.LENGTH_SHORT).show()
                },
                Response.ErrorListener { error ->
                    Toast.makeText(this, "No se envio nada", Toast.LENGTH_SHORT).show()
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json")
                return headers
            }
        }
        ready = false
        listaPromedios.clear()
        RequestHandler.getInstance(applicationContext).addToRequestQueue(jsonRequest)

    }

    private fun startLocationUpdates() {
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */)
    }
    private fun stopLocationUpdates() {
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.removeLocationUpdates(locationCallback)

    }

    override fun onStop() {
        super.onStop()
        if (stopButton?.isEnabled == true){
            stopButton?.callOnClick()
        }
        mapView?.onStop()
        stopLocationUpdates()

    }

    override fun onResume() {
        super.onResume();
        mapView?.onResume();
        startLocationUpdates()

    }


    override fun onStart() {
        super.onStart();
        mapView?.onStart();
    }



    override fun onPause() {
        mapView?.onPause();
        super.onPause();
    }

    override fun onDestroy() {
        mapView?.onDestroy();
        super.onDestroy();
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory();
    }

    fun print(sting : String){
        Log.w("GetDataAct", sting)
    }


}
/*
*
* {
	"redes": [{
			"ssid": "VERA",
			"mac": "21:12:12:12:12:12",
			"rssi": 23,
			"frequency": 2121
		},
		{
			"ssid": "JUANA",
			"mac": "32:54:23:54:23:32",
			"rssi": 56,
			"frequency": 2122
		}
	],
	"latitude": 122.2123,
	"longitude": 121.1212,
	"date": 12212.12
}
*
*
*
*
*
*
* */