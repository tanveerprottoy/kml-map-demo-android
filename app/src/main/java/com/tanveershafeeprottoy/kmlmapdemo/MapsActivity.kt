package com.tanveershafeeprottoy.kmlmapdemo

import android.annotation.SuppressLint
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.Feature
import com.google.maps.android.data.Layer.OnFeatureClickListener
import com.google.maps.android.data.kml.KmlContainer
import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlPolygon
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.lang.ref.WeakReference


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = MapsActivity::class.java.simpleName
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var googleMap: GoogleMap
    private var location: Location? = null
    private var currentLatLng: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        retrieveFileFromResource()
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                this.location = location
                currentLatLng = LatLng(location?.latitude!!, location?.longitude!!)
                /*googleMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLng!!)
                        .title("Marker in polygon")
                )*/
            }
    }

    private fun retrieveFileFromResource() {
        LoadLocalKmlFile(R.raw.test).execute()
    }

    private fun moveCameraToKml(kmlLayer: KmlLayer) {
        try {
            //Retrieve the first container in the KML layer
            var container = kmlLayer.containers.iterator().next()
            //Retrieve a nested container within the first container
            container = container.containers.iterator().next()
            //Retrieve the first placemark in the nested container
            val placeMark = container.placemarks.iterator().next()
            placeMark.properties.forEach {
                Log.d(
                    TAG,
                    "placeMark property: " + it.toString()
                )
            }
            //Retrieve a polygon object in a placemark
            // val kmlPolygon = placeMark.geometry as KmlLineString
            val kmlPolygon = placeMark.geometry as KmlPolygon
            // val kmlPoint = placeMark.geometry as KmlPoint
            //Create LatLngBounds of the outer coordinates of the polygon
            val builder = LatLngBounds.Builder()
/*            builder.include(
                LatLng(
                    kmlPoint.geometryObject.latitude,
                    kmlPoint.geometryObject.longitude
                )
            )*/
            for(latLng in kmlPolygon.outerBoundaryCoordinates) {
                builder.include(latLng)
            }
            /*for(latLng in kmlPolygon.geometryObject) {
                builder.include(latLng)
            }*/
            val width = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels
            val centerLatLng = getPolygonCenterPoint(kmlPolygon.geometryObject[0]!!)
            googleMap.addMarker(
                MarkerOptions()
                    .position(currentLatLng!!)
                    .title("Marker in polygon")
            )
            Log.d(
                TAG,
                "marker is inside polygon: " + polygonContainsLocation(
                    currentLatLng!!,
                    kmlPolygon.geometryObject[0]!!
                )
            )
            /*googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    latLngCurrent,
                    30f
                )
            )*/
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    builder.build(),
                    width,
                    height,
                    1
                )
            )
        }
        catch(e: Exception) {
            // may fail depending on the KML being shown
            e.printStackTrace()
            Log.e(
                TAG,
                "Exception: " + e.message
            )
        }
    }

    inner class LoadLocalKmlFile constructor(private val resourceId: Int) :
        AsyncTask<String?, Void?, KmlLayer?>() {

        override fun doInBackground(vararg strings: String?): KmlLayer? {
            try {
                return KmlLayer(googleMap, resourceId, WeakReference(this@MapsActivity).get())
            }
            catch(e: XmlPullParserException) {
                e.printStackTrace()
            }
            catch(e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(kmlLayer: KmlLayer?) {
            addKmlToMap(kmlLayer)
        }
    }

    private fun addKmlToMap(kmlLayer: KmlLayer?) {
        if(kmlLayer != null) {
            kmlLayer.addLayerToMap()
            kmlLayer.setOnFeatureClickListener(OnFeatureClickListener { feature: Feature ->
                Toast.makeText(
                    this@MapsActivity,
                    "Feature clicked: " + feature.id,
                    Toast.LENGTH_SHORT
                ).show()
                for(property in feature.properties) {
                    Log.d(
                        TAG,
                        "geometryType: " + property.toString()
                    )
                }
            })
            for(container in kmlLayer.containers) {
                Log.d(
                    TAG,
                    "containerId: " + container.containerId
                )
            }
            for(placeMark in kmlLayer.placemarks) {
                // Do something to Placemark
                Log.d(
                    TAG,
                    "placeMark: " + placeMark.id
                )
            }
            for(container in kmlLayer.containers) {
                if(container.hasProperty("name")) {
                    Log.d("KML", container.getProperty("name"))
                }
            }
            accessContainers(kmlLayer.containers)
            moveCameraToKml(kmlLayer)
        }
    }

    fun accessContainers(containers: Iterable<KmlContainer>) {
        for(container in containers) {
            Log.d("accessContainers", container.getProperty("name"))
            if(container.hasContainers()) {
                accessContainers(container.containers)
            }
        }
    }

    private fun polygonContainsLocation(
        point: LatLng,
        polygon: List<LatLng>,
        geodesic: Boolean = false
    ): Boolean {
        return PolyUtil.containsLocation(point, polygon, geodesic)
    }

    private fun getPolygonCenterPoint(polygon: List<LatLng>): LatLng? {
        var centerLatLng: LatLng? = null
        val builder: LatLngBounds.Builder = LatLngBounds.Builder()
        for(element in polygon) {
            builder.include(element)
        }
        val bounds: LatLngBounds = builder.build()
        centerLatLng = bounds.center
        return centerLatLng
    }

    /*private fun getPlaceMarkData(kmlPlaceMarkIterable: Iterable<KmlPlacemark>): SparseArray<String> {
        var kmlPlacemark: KmlPlacemark
        val i = 0
        while(kmlPlaceMarkIterable.iterator().hasNext()) {
            kmlPlacemark = kmlPlaceMarkIterable.iterator().next()
            kmlPlacemark.properties.forEach {

            }
        }
    }*/
}