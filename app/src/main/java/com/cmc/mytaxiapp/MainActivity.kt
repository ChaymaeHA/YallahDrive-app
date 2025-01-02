package com.cmc.mytaxiapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnStart: Button
    private lateinit var tvDistance: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvFare: TextView
    private lateinit var btnProfile: ImageButton

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var sharedPreferences: SharedPreferences

    private var startTime: Long = 0
    private var startLocation: LatLng? = null
    private var currentLocation: LatLng? = null
    private var totalDistance: Float = 0f
    private var totalTime: Long = 0
    private var fare: Float = 0f

    private val baseFare = 2.5f
    private val farePerKm = 1.5f
    private val farePerMinute = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tvDistance = findViewById(R.id.tvDistance)
        tvTime = findViewById(R.id.tvTime)
        tvFare = findViewById(R.id.tvFare)
        btnStart = findViewById(R.id.btnStart)
        btnProfile = findViewById(R.id.btn_profile)
        sharedPreferences = getSharedPreferences("DriverPreferences", Context.MODE_PRIVATE)

        if (!sharedPreferences.getBoolean("is_registered", false)) {
            startActivity(Intent(this, DriverRegistrationActivity::class.java))
            finish()
        } else {
            initMap()
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            initMap()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "This app needs location permissions",
                1,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        setupLocationRequest()

        btnStart.setOnClickListener {
            if (startLocation == null) {
                startTrip()
            } else {
                stopTrip()
            }
        }
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    currentLocation = LatLng(location.latitude, location.longitude)
                    currentLocation?.let { loc ->
                        mMap.clear()
                        mMap.addMarker(MarkerOptions().position(loc).title("Driver"))
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f))

                        startLocation?.let { startLoc ->
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                startLoc.latitude,
                                startLoc.longitude,
                                loc.latitude,
                                loc.longitude,
                                results
                            )
                            totalDistance += results[0] / 1000f
                        }
                    }
                    updateUI()
                }
            }
        }
    }

    private fun startTrip() {
        startTime = System.currentTimeMillis()
        getCurrentLocation { location ->
            if (location == null) {
                Toast.makeText(this, "Unable to get starting location", Toast.LENGTH_SHORT).show()
                return@getCurrentLocation
            }
            startLocation = location
            totalDistance = 0f
            totalTime = 0
            fare = baseFare
            btnStart.text = "Stop Trip"
            updateUI()
            startLocationUpdates()
        }
    }

    private fun stopTrip() {
        val endTime = System.currentTimeMillis()
        totalTime = (endTime - startTime) / 60000 // Convert to minutes
        fare += totalDistance * farePerKm + totalTime * farePerMinute
        stopLocationUpdates()
        showNotification()
        updateUI()
        btnStart.text = "Start Trip"
        startLocation = null
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(callback: (LatLng?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                callback(location?.let { LatLng(it.latitude, it.longitude) })
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    private fun updateUI() {
        tvDistance.text = "${"%.2f".format(totalDistance)} km"
        tvTime.text = "$totalTime min"
        tvFare.text = "${"%.2f".format(fare)} DH"
    }

    private fun showNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tripChannel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Trip Notifications", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Trip Finished")
            .setContentText("Fare: ${"%.2f".format(fare)} DH, Distance: ${"%.2f".format(totalDistance)} km, Time: $totalTime min")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(1, notification)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isMyLocationEnabled = true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            initMap()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }
}
