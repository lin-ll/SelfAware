package ll7.secondapp;

import android.app.FragmentManager;
import android.location.Location;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.view.*;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    SupportMapFragment smsMap;
    SupportMapFragment callMap;
    GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LatLng currLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        smsMap = SupportMapFragment.newInstance();
        callMap = SupportMapFragment.newInstance();

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);

        Fragment fragment = new FragmentHome();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.mainFrame, fragment);
        ft.commit();

        smsMap.getMapAsync(this);
        callMap.getMapAsync(this);

        buildGoogleApiClient();
        if(mGoogleApiClient!= null)
            mGoogleApiClient.connect();
        else Toast.makeText(this, "Not connected...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        if (currLocation != null)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currLocation));
        else {
            LatLng princeton = new LatLng(40.3440, 74.6514);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(princeton));
        }
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) return true;
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        FragmentManager fm = getFragmentManager();
        android.support.v4.app.FragmentManager sfm = getSupportFragmentManager();

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (smsMap.isAdded())
            sfm.beginTransaction().hide(smsMap).commit();
        if (callMap.isAdded())
            sfm.beginTransaction().hide(callMap).commit();

        FrameLayout layout = (FrameLayout)findViewById(R.id.mainFrame);
        layout.setVisibility(View.VISIBLE); // you can use INVISIBLE also instead of GONE

        if (id == R.id.nav_home) {
            fragmentReplacer(fm, new FragmentHome());
        } else if (id == R.id.nav_call_loc) {
            layout.setVisibility(View.GONE); // you can use INVISIBLE also instead of GONE
            if(!callMap.isAdded())
                sfm.beginTransaction().add(R.id.map, callMap).commit();
            else
                sfm.beginTransaction().show(callMap).commit();
        } else if (id == R.id.nav_sms_loc) {
            layout.setVisibility(View.GONE); // you can use INVISIBLE also instead of GONE
            if(!smsMap.isAdded())
                sfm.beginTransaction().add(R.id.map, smsMap).commit();
            else
                sfm.beginTransaction().show(smsMap).commit();
            fragmentReplacer(fm, new FragmentCallAct());
        } else if (id == R.id.nav_call_act) {
            fragmentReplacer(fm, new FragmentCallAct());
        } else if (id == R.id.nav_sms_act) {
            fragmentReplacer(fm, new FragmentCallAct());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void fragmentReplacer(FragmentManager fm, Fragment fragment) {
        fm.beginTransaction().replace(R.id.mainFrame, fragment).commit();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Toast.makeText(this, "Failed to connect...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle arg0) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null)
            currLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        Toast.makeText(this, "Connection suspended...", Toast.LENGTH_SHORT).show();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}
