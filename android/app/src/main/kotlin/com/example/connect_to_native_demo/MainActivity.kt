package com.example.connect_to_native_demo

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.flutter.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import java.util.stream.Stream


class MainActivity : FlutterActivity() {

    private val CHANNEL = "samples.flutter.dev/battery"


    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        startMethodChannel(flutterEngine)
        startEventChannel(flutterEngine)


    }

    fun startMethodChannel(flutterEngine: FlutterEngine) {

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger, CHANNEL
        ).setMethodCallHandler { call, result ->
            // This method is invoked on the main thread.


            if (call.method == "getDataFromFlutter") {


                val data = call.argument<String>("data");

                if (data != null) {
                    Toast.makeText(this, "$data=>Native-toast", Toast.LENGTH_SHORT).show()
                    Log.d("data", data)
                } else {
                    Toast.makeText(this, "data is null=>Native-toast", Toast.LENGTH_SHORT).show()
                    Log.d("data", "null")
                }

            } else if (call.method == "getBatteryLevel") {

                val batteryLevel = getBatteryLevel()
                if (batteryLevel != -1) {

                    result.success(batteryLevel)
                } else {
                    result.error("UNAVAILABLE", "Battery level not available.", null)
                }
            } else if (call.method == "getUserLocation") {


                if (checkPermission()) {
                    val fusedLocationClient: FusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(this)
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        result.success(
                            "${location?.latitude}___${location?.longitude}"
                        )


                    }

                } else {
                    requestPermission()
                }


            } else {
                result.notImplemented()
            }
        }


    }

    fun startEventChannel(flutterEngine: FlutterEngine) {

        Log.d("data", "startEventChannel")
        try{
            EventChannel(
                flutterEngine.dartExecutor.binaryMessenger, "locationStatusStream"
            ).setStreamHandler(
                object : EventChannel.StreamHandler {
                    @RequiresApi(Build.VERSION_CODES.N)
                    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {

                        Log.d("data", "onListen")


                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: android.location.Location) {

                                events?.success("${location.latitude}___${location.longitude}")
                            }

                            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {

                                events?.success("${provider}___${status}___${extras}")
                            }

                            override fun onProviderEnabled(provider: String) {
                                events?.success("Your location service is true")
                            }

                            override fun onProviderDisabled(provider: String) {
                                events?.success("Your location service is false")
                            }
                        }
                        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        if (ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                ACCESS_FINE_LOCATION
                            ) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) != PERMISSION_GRANTED
                        ) {
                           requestPermission()
                            return
                        }
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            1000,
                            1f, listener)

                    }

                    override fun onCancel(arguments: Any?) {
                        Log.d("data", "onCancel")
                        // This method is invoked on the main thread.
                    }
                }
            )
        } catch (e: Exception) {
            e.message?.let { Log.e("Error", it) }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryLevel: Int
        batteryLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(
                null, IntentFilter(
                    Intent.ACTION_BATTERY_CHANGED
                )
            )
            intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(
                BatteryManager.EXTRA_SCALE, -1
            )
        }

        return batteryLevel
    }


    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), 1)
        ActivityCompat.OnRequestPermissionsResultCallback { requestCode, permissions, grantResults ->
            if (requestCode == 1) {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d("data", "permission denied");
                }
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }

        }
    }

    private fun checkPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED)
    }

    fun getLocationStream(context: Context): Sequence<Location?> {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {


            override fun onProviderEnabled(provider: String) {
                // Not used in this implementation
            }

            override fun onProviderDisabled(provider: String) {
                // Not used in this implementation
            }

            override fun onLocationChanged(provider: Location) {

            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // Not used in this implementation
            }
        }

        // Request location updates from the LocationManager
        val minTime = 1000L // minimum time (in milliseconds) between updates
        val minDistance = 0f // minimum distance (in meters) between updates
        if (ActivityCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PERMISSION_GRANTED
        ) {

            return null!!
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, locationListener)

        // Create a stream from the location updates
        return generateSequence {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.asSequence()
    }



}


