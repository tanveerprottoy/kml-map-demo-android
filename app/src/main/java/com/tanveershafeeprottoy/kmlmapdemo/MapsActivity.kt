package com.tanveershafeeprottoy.kmlmapdemo

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.data.Feature
import com.google.maps.android.data.Layer.OnFeatureClickListener
import com.google.maps.android.data.kml.KmlContainer
import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlPoint
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.lang.ref.WeakReference


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private val TAG = MapsActivity::class.java.simpleName

    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
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
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        retrieveFileFromResource()
    }

    private fun retrieveFileFromResource() {
        LoadLocalKmlFile(R.raw.westcampus).execute()
    }

    private fun moveCameraToKml(kmlLayer: KmlLayer) {
        try {
            //Retrieve the first container in the KML layer
            var container = kmlLayer.containers.iterator().next()
            //Retrieve a nested container within the first container
            // container = container.containers.iterator().next()
            //Retrieve the first placemark in the nested container
            val placemark = container.placemarks.iterator().next()
            //Retrieve a polygon object in a placemark
            val kmlPoint = placemark.geometry as KmlPoint
            //Create LatLngBounds of the outer coordinates of the polygon
            val builder = LatLngBounds.Builder()
            builder.include(
                LatLng(
                    kmlPoint.geometryObject.latitude,
                    kmlPoint.geometryObject.longitude
                )
            )
            val width = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 1))
        }
        catch(e: Exception) {
            // may fail depending on the KML being shown
            e.printStackTrace()
        }
    }

    inner class LoadLocalKmlFile constructor(private val resourceId: Int) : AsyncTask<String?, Void?, KmlLayer?>() {

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
            })
            for(container in kmlLayer.containers) {
                Log.i(
                    TAG,
                    "containerId: " + container.containerId
                )
            }
            for(placeMark in kmlLayer.placemarks) {
                // Do something to Placemark
                Log.i(
                    TAG,
                    "placeMark: " + placeMark.id
                )
            }
            for(container in kmlLayer.containers) {
                if(container.hasProperty("name")) {
                    Log.i("KML", container.getProperty("name"))
                }
            }
            moveCameraToKml(kmlLayer)
        }
    }

    fun accessContainers(containers: Iterable<KmlContainer>) {
        for(container in containers) {
            if(container.hasContainers()) {
                accessContainers(container.containers)
            }
        }
    }
}