package com.discassist;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    Location myLocation;
    ProgressDialog mProgressDialog;
    ArrayList<HashMap<String, String>> courses;
    List<Marker> courseMarkers = new ArrayList<Marker>(); //Markers for COURSES
    List<Marker> currentCoursesMarkers = new ArrayList<Marker>(); //Markers for current course
    JSONArray jsonarray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        // Create a Progress Dialog
        mProgressDialog = new ProgressDialog(MapsActivity.this);
        mProgressDialog.setTitle("Getting Courses");
        mProgressDialog.setMessage("Loading...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            myLocation = location;
        }
    };

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            mMap.setMyLocationEnabled(true);

            mMap.setOnMyLocationChangeListener(myLocationChangeListener);

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }

        new getCourseInformation().execute();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        dialog.dismiss();
                        //Do nothing

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();

                        Toast.makeText(getApplicationContext(), "Display Course Info Page", Toast.LENGTH_SHORT).show();
                        displayCourseComponents(courses.indexOf(marker));
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setMessage("Display this course?")
                .setNegativeButton("Yes", dialogClickListener)
                .setPositiveButton("No", dialogClickListener)
                .show();

        return false;
    }

    private void displayCourseComponents(int courseId){
        try {
            JSONArray obj = new JSONArray(courses.get(courseId).get("holes"));
            obj.length();

//            double latitude = Double.parseDouble(obj.get);
//            double longitude = Double.parseDouble(obj.getString("lon"));
//            String name = obj.get("name");
//            String desc = obj.get("desc");
//
//
//            BitmapFactory.Options o = new BitmapFactory.Options();
//            Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.course_icon, o);
//
//            Marker m = mMap.addMarker(new MarkerOptions()
//                    .position(new LatLng(latitude, longitude))
//                    .title(name)
//                    .snippet(desc)
//                    .icon(BitmapDescriptorFactory.fromBitmap(img)));
//
//            courseMarkers.add((m));
//
//            mMap.setOnMarkerClickListener(MapsActivity.this);
//
//            mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
//
//                @Override
//                public void onInfoWindowClick(Marker marker) {
//
//                    /** TODO: This will take you to a description of the course and some general stats including
//                     *  Discs lost, average hole distance, average score, etc.
//                     *
//                     */
//
//
//                    // Make new intent to detail view
//                    //  Intent i = new Intent(getApplicationContext(), BarDetail.class);
//                    //  i.putExtra("jsonArray", jsonarray.toString());
//                    //  i.putExtra("bar", marker.getTitle());
//                    //  startActivity(i);
//
//                }
//
//
//            });


        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    // DownloadJSON AsyncTask
    private class getCourseInformation extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Create an array
            courses = new ArrayList<HashMap<String, String>>();

            try {
                // Retrieve JSON Objects from the given URL address
                jsonarray = JSONFunctions.getJSONfromURL("http://gerlichsoftwaresolutions.net/__projects/discassist/grabMarkers.php");

                for (int i = 0; i < jsonarray.length(); i++) {
                    HashMap<String, String> course = new HashMap<String, String>();
                    JSONObject obj = jsonarray.getJSONObject(i);
                    // Retrieve JSON Objects
                    course.put("id", String.valueOf(i));
                    course.put("name", obj.getString("name"));
                    course.put("lat", obj.getString("lat"));
                    course.put("lon", obj.getString("lon"));
                    course.put("desc", obj.getString("desc"));
                    course.put("holes", obj.getString("holes"));

                    // Set the JSON Objects into the array
                    courses.add(course);
                }

            } catch (JSONException e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void args) {

            // Add markers to the map
            for (int i = 0; i < courses.size(); i++) {
                // Get the current bar HashMap
                HashMap<String, String> course = courses.get(i);

                double latitude = Double.parseDouble(course.get("lat"));
                double longitude = Double.parseDouble(course.get("lon"));
                String name = course.get("name");
                String desc = course.get("desc");


                BitmapFactory.Options o = new BitmapFactory.Options();
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.course_icon, o);

                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title(name)
                        .snippet(desc)
                        .icon(BitmapDescriptorFactory.fromBitmap(img)));

                courseMarkers.add((m));

                mMap.setOnMarkerClickListener(MapsActivity.this);

                mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                    @Override
                    public void onInfoWindowClick(Marker marker) {

                        /** TODO: This will take you to a description of the course and some general stats including
                         *  Discs lost, average hole distance, average score, etc.
                         *
                         */


                        // Make new intent to detail view
                        //  Intent i = new Intent(getApplicationContext(), BarDetail.class);
                        //  i.putExtra("jsonArray", jsonarray.toString());
                        //  i.putExtra("bar", marker.getTitle());
                        //  startActivity(i);

                    }


                });


            }

            mProgressDialog.dismiss();

//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
//                    new LatLng(myLocation.getLatitude(), myLocation.getLongitude()), 13));

            Toast.makeText(getApplicationContext(), "Tap your course to begin!", Toast.LENGTH_SHORT).show();
        }
    }
}
