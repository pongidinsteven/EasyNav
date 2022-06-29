package id.easynav

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.widget.Toast
import android.location.Location
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.huawei.hms.location.*
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.*
import id.easynav.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback, HuaweiMap.OnCameraIdleListener,
    HuaweiMap.OnCameraMoveStartedListener, HuaweiMap.OnCameraMoveListener{

    private lateinit var binding : ActivityMainBinding

    private var mapViewBundle : Bundle? = null

    private var hMap : HuaweiMap? = null

    private var locationRequest : LocationRequest? = null

    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient

    private var requestPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    private var userMarker : Marker? = null

    private var userLocationRetrieved = false

    private var currentLat : Double = 0.0
    private var currentLng : Double = 0.0

    private var lastUserLat : Double = 0.0
    private var lastUserLng : Double = 0.0

    companion object {
        private val RUNTIME_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private const val REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                result ->

            if(result[Manifest.permission.ACCESS_FINE_LOCATION] != true &&
                result[Manifest.permission.ACCESS_COARSE_LOCATION] != true) {
                // Location permission not given, show permission request
                showLocationPermissionRequest()
            }
        }

        setSupportActionBar(binding.mainToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.mainToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.more -> {
                    // Show additional menu
                    Toast.makeText(this, "More clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.search -> {
                    // Show search places fragment
                    Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        binding.mainToolbar.setNavigationOnClickListener {
            // Show drawer menu
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show()
        }

        //Initialize map
        if(savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        binding.mapView.onCreate(mapViewBundle)
        binding.mapView.getMapAsync(this)

        // Check location permission
        showLocationPermissionRequest()

        binding.btnMyLocation.setOnClickListener {
            if(hasLocationPermission(this@MainActivity, *RUNTIME_PERMISSIONS)) {
                checkDeviceLocationSetting()
            } else {
                showLocationPermissionRequest()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if(outState.getBundle(MAPVIEW_BUNDLE_KEY) == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        binding.mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    override fun onMapReady(map: HuaweiMap?) {
        hMap = map

        hMap?.setOnCameraIdleListener(this)
        hMap?.setOnCameraMoveStartedListener(this)
        hMap?.setOnCameraMoveListener(this)
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onRestart() {
        super.onRestart()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_CODE -> if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if(hasLocationPermission(this@MainActivity, *RUNTIME_PERMISSIONS)) {
                    // Location permission granted, calculate user location
                    checkDeviceLocationSetting()
                } else {
                    showPermissionRejectedDialog()
                }
            }
        }
        return
    }

    override fun onCameraIdle() {
        hMap?.cameraPosition?.let {
            currentLat = it.target.latitude
            currentLng = it.target.longitude
        }
        Log.d("EasyNav", "Camera moved, lat: $currentLat, lng: $currentLng")

        if(userLocationRetrieved) {
            userLocationRetrieved = false
            binding.btnMyLocation.setImageResource(R.drawable.ic_my_location_on)
        }
    }

    override fun onCameraMove() {

    }

    override fun onCameraMoveStarted(p0: Int) {
        Log.d("EasyNav", "Camera move started")
        binding.btnMyLocation.setImageResource(R.drawable.ic_my_location_off)
    }

    private fun showPermissionRejectedDialog() {
        val rejectedDialogBuilder : AlertDialog.Builder? = this?.let {
            AlertDialog.Builder(it)
        }
        rejectedDialogBuilder?.setMessage(R.string.location_permission_message)
            ?.setTitle(R.string.location_permission_title)
            ?.setPositiveButton(R.string.allow) { dialog, id ->
                showLocationPermissionRequest()
            }
            ?.setNegativeButton(R.string.deny) { dialog, id ->
                // Do nothing
            }?.create()?.show()
    }

    private fun showLocationPermissionRequest() {
        ActivityCompat.requestPermissions(this@MainActivity, RUNTIME_PERMISSIONS, REQUEST_CODE)
    }

    private fun hasLocationPermission(context : Context, vararg permissions : String) : Boolean{
        for(permission in permissions) {
            if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun checkDeviceLocationSetting() {
        val settingsClient = LocationServices.getSettingsClient(this)
        val builder = LocationSettingsRequest.Builder()
        locationRequest = LocationRequest()
        builder.addLocationRequest(locationRequest)
        val locationSettingRequest = builder.build()
        settingsClient.checkLocationSettings(locationSettingRequest)
            .addOnSuccessListener { locationSettingResponse ->
                getUserLocation()
            }
            .addOnFailureListener {
                // Handle location failure
            }
    }

    private fun getUserLocation() {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.numUpdates = 1

        val locationCallback : LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let { result ->
                    Log.d("EasyNav", "Location lat: ${result.locations[0].latitude}, long: ${result.locations[0].longitude}, bearing: ${result.locations[0].bearing}")
                    // Move camera to user location when location is retrieved
                    userLocationRetrieved = true
                    lastUserLat = result.locations[0].latitude
                    lastUserLng = result.locations[0].longitude
                    moveCameraToLocation(lastUserLat, lastUserLng)
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            .addOnSuccessListener {

            }
            .addOnFailureListener {

            }
    }

    private fun moveCameraToLocation(lat : Double, lng : Double) {
        val cameraPosition = CameraPosition(LatLng(lat, lng), 18.0f, 0.0f, 0.0f)
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
        val animationDuration = getUserLocationAnimationTime(currentLat, currentLng, lastUserLat, lastUserLng)
        hMap?.animateCamera(cameraUpdate, animationDuration, null)
        drawUserMarker(lat, lng)
    }

    private fun drawUserMarker(lat : Double, lng : Double) {
        if(userMarker != null) {
            userMarker!!.remove()
        }

        val option = MarkerOptions()
            .position(LatLng(lat, lng))
            .anchorMarker(0.5f, 0.5f)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_user_map_icon))
        hMap?.addMarker(option)
    }

    private fun getUserLocationAnimationTime(initLat : Double, initLng : Double,
                                             latestLat : Double, latestLng : Double) : Int {
        val minAnimTime = 500
        val maxAnimTime = 2000
        var animTime = maxAnimTime //Set default animTime to max
        var maxDistance = 10000.0
        // Calculate distance of user location and current camera location
        val initLocation = Location("Init")
        initLocation.latitude = initLat
        initLocation.longitude = initLng

        val latestLocation = Location("Latest")
        latestLocation.latitude = latestLat
        latestLocation.longitude = latestLng

        val distance = initLocation.distanceTo(latestLocation)  //In meter
        if(distance < maxDistance) {
            animTime = (distance / maxDistance * animTime).toInt()
            if(animTime < minAnimTime) {
                animTime = minAnimTime
            }
        }
        Log.d("EasyNav", "Distance: $distance")
        return animTime
    }
}