package com.discassist;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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

public class MapsActivity extends AppCompatActivity implements GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    //Current location ( constantly updated)
    Location myLocation;

    //Progress view / loading spinnger thing
    ProgressDialog mProgressDialog;

    //Current course information as hash maps/json
    ArrayList<HashMap<String, String>> courses;
    JSONArray jsonarray;

    //Course markers for all courses
    List<Marker> courseMarkers = new ArrayList<Marker>(); //Markers for COURSES

    //Course marks for holes and tees
    List<Marker> currentCourseHoles = new ArrayList<Marker>(); //Markers for current course
    List<Marker> currentCoursesRegTees = new ArrayList<Marker>(); //Markers for current course
    List<Marker> currentCoursesProTees = new ArrayList<Marker>(); //Markers for current course

    //Whether a course is currently expanded
    boolean courseShown = false;

    //Whether regular tees are being shown
    boolean regTeesShown = false;

    //Whether pro tees are being shown
    boolean proTeesShown = false;

    //Whether or not we've placed courses on the map
    boolean coursesLoaded = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        // Create a Progress Dialog to inform them of the courses being loaded
        mProgressDialog = new ProgressDialog(MapsActivity.this);

        //Prevent this from showing when you change the orientation or reload this screen
        if ( !coursesLoaded ) {
            mProgressDialog.setTitle("Getting Courses");
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.show();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_maps, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setQueryHint("Find a Course");
        searchView.setIconifiedByDefault(false);


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                for (Marker marker : courseMarkers) {

                    //Rudimentary search option..
                    //TODO: search by location
                    if(marker.getTitle().toLowerCase().contains(s)){
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 18);
                        mMap.animateCamera(update);
                        marker.showInfoWindow();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "No Courses Found...", Toast.LENGTH_SHORT).show();
                    }
                }
                searchView.clearFocus();
                // Zoom to bar
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //TODO: Auto fill here based on current string
                return false;
            }

        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.settings:
                Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Keep track of our current location.. May have purpose later
     */
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

            //Hybrid satellite type is dope as shit for this. We need brighter icons though!
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            //Listener to keep track of current location
            mMap.setOnMyLocationChangeListener(myLocationChangeListener);

            //Zoom us in right quick ya hear
            zoomToCurrentLocation();

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }

        if ( !coursesLoaded ) {
            new getCourseInformation().execute();
            coursesLoaded = true;
        }

    }

    /**
     * Zooms to the users current location (For when the map first starts)
     */
    public void zoomToCurrentLocation(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();

        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location != null)
        {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                    .zoom(10)                   // Sets the zoom
                    //.bearing(90)                // Sets the orientation of the camera to east
                    //.tilt(40)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }

    }

    /**
     * Added by default with android studio.. No use really for our purposes
     */
    private void setUpMap() {

    }

    /**
     * Handles all of the marker clicks.. This can be confusing, hopefully my comments help!
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        //Dialog/Actions for showing the selected courses tees and holes
        DialogInterface.OnClickListener showCourse = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        dialog.dismiss();
                        //Do nothing

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();

                        //For the sake of convenience, only display one course at a time.. Easy update though
                        //TODO: If we want to be able to show multiple courses, have an arraylist of the currentCourse markers (arraylist of arraylists.. Easy fix, just no real purpose to it that I see)
                        hideCourse();

                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude))      // Sets the center of the map to location user
                                .zoom(17)                   // Sets the zoom
                                .build();                   // Creates a CameraPosition from the builder

                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                        showHoles(courseMarkers.indexOf(marker));
                        showRegularTees(courseMarkers.indexOf(marker));


                        courseShown = true;
                        regTeesShown = true;
                        proTeesShown = false;

                        break;
                }
            }
        };

        //Dialog/Actions for hiding the selected courses tees and holes
        DialogInterface.OnClickListener hideCourse = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        dialog.dismiss();
                        //Do nothing

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();

                        hideCourse();

                        courseShown = false;
                        regTeesShown = false;
                        proTeesShown = false;

                        break;
                }
            }
        };

        //If they are selecting a marker for a course (as apposed to a tee or hole marker)
        if ( courseMarkers.contains(marker) ) {

            //If this course is already  shown (i.e, we are wanting to hide it)
            if ( courseShown ) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setMessage("Hide this course?")
                        .setNegativeButton("Yes", hideCourse)
                        .setPositiveButton("No", hideCourse)
                        .show();

            //If this course is not shown (i.e, we want to show it)
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                builder.setMessage("Show this course?")
                        .setNegativeButton("Yes", showCourse)
                        .setPositiveButton("No", showCourse)
                        .show();
            }


        }

        return false;
    }

    /**
     * Hide the full course
     */
    private void hideCourse(){
        hideCourseHoles();
        hideCourseRegularTees();
        hideCourseProTees();
    }

    /**
     * Hides the courses holes/baskets
     */
    private void hideCourseHoles(){
        if ( courseShown ) {
            for (int i = 0; i < currentCourseHoles.size(); i++) {
                currentCourseHoles.get(i).remove();
            }
        }
    }

    /**
     * Hides the courses regular tees
     */
    private void hideCourseRegularTees(){
        if ( regTeesShown ) {
            for (int i = 0; i < currentCoursesRegTees.size(); i++) {
                currentCoursesRegTees.get(i).remove();
            }
        }
    }

    /**
     * Hides the courses pro tees
     */
    private void hideCourseProTees(){
        if ( proTeesShown ) {
            for (int i = 0; i < currentCoursesProTees.size(); i++) {
                currentCoursesProTees.get(i).remove();
            }
        }
    }

    /**
     * Shows the baskets for this course
     * @param courseId
     */
    private void showHoles(int courseId){
        try {
            JSONArray obj = new JSONArray(courses.get(courseId).get("holes"));

            for (int i = 0; i < obj.length(); i++ ) {
                JSONObject curHole = obj.getJSONObject(i);

                String name = "Hole " + curHole.getString("num");
                Double latitude = Double.valueOf(curHole.getString("lat"));
                Double longitude = Double.valueOf(curHole.getString("lon"));

                BitmapFactory.Options o = new BitmapFactory.Options();

                //TODO: Get a new icon/set them up to scale correctly
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.course_icon, o);

                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title(name)
                        .icon(BitmapDescriptorFactory.fromBitmap(img)));

                currentCourseHoles.add((m));


            }

        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Show the regular tees for this course
     * @param courseId
     */
    private void showRegularTees(int courseId){
        try {
            JSONArray obj = new JSONArray(courses.get(courseId).get("regTees"));

            for (int i = 0; i < obj.length(); i++ ) {
                JSONObject curHole = obj.getJSONObject(i);

                String name = "(Reg) Tee " + curHole.getString("num");
                Double latitude = Double.valueOf(curHole.getString("lat"));
                Double longitude = Double.valueOf(curHole.getString("lon"));

                BitmapFactory.Options o = new BitmapFactory.Options();

                //TODO: New icon, scaling correctly.
                //TODO: Also, we definitely want the Tee images to be rotated towards the hole.. Currently they point due north.
                //TODO: My idea: Store the orientation (Degree of rotation) in the database for each one to keep this function generic
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.tee_placeholder, o);

                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title(name)
                        //.rotation(-1) TODO: Setup orientation as mentioned above..
                        .icon(BitmapDescriptorFactory.fromBitmap(img)));

                currentCoursesRegTees.add((m));


            }

        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Show the pro tees for this course
     * @param courseId
     */
    private void showProTees(int courseId){
        try {
            JSONArray obj = new JSONArray(courses.get(courseId).get("proTees"));

            for (int i = 0; i < obj.length(); i++ ) {
                JSONObject curHole = obj.getJSONObject(i);

                String name = "(Pro) Tee " + curHole.getString("num");
                Double latitude = Double.valueOf(curHole.getString("lat"));
                Double longitude = Double.valueOf(curHole.getString("lon"));

                BitmapFactory.Options o = new BitmapFactory.Options();

                //TODO: New icon, scaling correctly.
                //TODO: Also, we definitely want the Tee images to be rotated towards the hole.. Currently they point due north.
                //TODO: My idea: Store the orientation (Degree of rotation) in the database for each one to keep this function generic
                Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.tee_placeholder, o);

                Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title(name)
                                //.rotation(-1) TODO: Setup orientation as mentioned above..
                        .icon(BitmapDescriptorFactory.fromBitmap(img)));

                currentCoursesProTees.add((m));


            }

        } catch (JSONException e1) {
            e1.printStackTrace();
        }
    }

    //Atask that grabs all of the courses information..
    //TODO: Will want to localize to within a range set in the settings (i.e, 50 mile radius) to avoid excess data usage
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
                    course.put("regTees", obj.getString("regTees"));
                    course.put("proTees", obj.getString("proTees"));

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
                        .snippet(desc) //TODO: We are going to want a custom info window to display some info like (City, State) - # of holes, rating, discs lost here, etc.
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
