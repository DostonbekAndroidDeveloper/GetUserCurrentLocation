package com.dostonbek.achieveaddress



import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private lateinit var getLocationButton: Button

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationTextView = findViewById(R.id.locationTextView)
        getLocationButton = findViewById(R.id.getLocationButton)

        getLocationButton.setOnClickListener {
            getLocation()
        }

        locationTextView.setOnClickListener {
            openGoogleMaps()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        Log.d(TAG, "Permissions granted, fetching location...")

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d(TAG, "Location fetched: ${location.latitude}, ${location.longitude}")

                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0].getAddressLine(0)
                    locationTextView.text = "Location: $address"
                    Log.d(TAG, "Address found: $address")
                } else {
                    locationTextView.text = "Location: Lat: ${location.latitude}, Lon: ${location.longitude}"
                    Log.d(TAG, "No address found, using lat/lon")
                }
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Location is null")
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Error getting location: ${exception.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error getting location", exception)
        }

        // Add this block to check and prompt the user to enable location settings
        if (!isLocationEnabled()) {
            showLocationSettingsAlert()
        }
    }

    private fun openGoogleMaps() {
        val locationText = locationTextView.text.toString()
        if (locationText.startsWith("Location: ")) {
            val address = locationText.removePrefix("Location: ")
            val uri = "geo:0,0?q=${android.net.Uri.encode(address)}"
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No app to handle map request", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No location to show", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to check if location services are enabled
    private fun isLocationEnabled(): Boolean {
        val locationMode: Int
        try {
            locationMode = Settings.Secure.getInt(
                contentResolver, Settings.Secure.LOCATION_MODE
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    // Function to show an alert dialog directing the user to enable location settings
    private fun showLocationSettingsAlert() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Location Settings")
        alertDialog.setMessage("Location is disabled. Please enable location services.")
        alertDialog.setPositiveButton("Settings") { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        alertDialog.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        alertDialog.show()
    }
}
