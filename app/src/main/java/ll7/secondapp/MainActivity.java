package ll7.secondapp;

import android.app.FragmentManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.location.Location;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Environment;
import android.util.Log;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import android.os.Bundle;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private SupportMapFragment sMap;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LatLng currLocation;
    private ClusterManager<MyItem> mClusterManager1;
    private ClusterManager<MyItem> mClusterManager2;

    private class MyItem implements ClusterItem {
        private final LatLng mPosition;
        private int icon;

        public MyItem(double lat, double lng, int icon) {
            mPosition = new LatLng(lat, lng);
            this.icon = icon;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        public BitmapDescriptor getIcon() {
            return BitmapDescriptorFactory.defaultMarker((icon == 0) ?
                    BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_ROSE);
        }
    }

    private class CustomClusterIcon extends DefaultClusterRenderer<MyItem> {

        private int icon_type;

        public CustomClusterIcon (int icon_type, ClusterManager<MyItem> clusterManager) {
            super(getApplicationContext(), mMap, clusterManager);
            this.icon_type = icon_type;
        }

        @Override
        protected void onBeforeClusterItemRendered(MyItem item, MarkerOptions markerOptions) {
            markerOptions.icon(item.getIcon());
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<MyItem> cluster, MarkerOptions markerOptions) {
            // Draw multiple people.
            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
            IconGenerator ig = new IconGenerator(getApplicationContext());

            int draw_icon = (icon_type == 0) ? R.drawable.ic_call_black_24dp : R.drawable.ic_chat_black_24dp;
            int color = (icon_type == 0) ? android.R.color.holo_red_light : android.R.color.holo_blue_bright;

            final Drawable clusterIcon = getResources().getDrawable(draw_icon);
            clusterIcon.setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);

            ig.setBackground(clusterIcon);

            //modify padding for one or two digit numbers
            ig.setContentPadding((cluster.getSize() < 10) ? 40 : 30, 20, 0, 0);

            Bitmap icon = ig.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            return cluster.getSize() > 5;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sMap = SupportMapFragment.newInstance();

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
        onOptionsItemSelected(navigationView.getMenu().getItem(0));

        Fragment fragment = new FragmentHome();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.mainFrame, fragment);
        ft.commit();

        sMap.getMapAsync(this);

        buildGoogleApiClient();
        if(mGoogleApiClient!= null)
            mGoogleApiClient.connect();
        else Toast.makeText(this, "Not connected...", Toast.LENGTH_SHORT).show();
    }

    public void addMarkers(int db_type) {
        String filePath = Environment.getExternalStorageDirectory() + "/ll7.secondapp/correlated/" +
                ((db_type == 0) ? "call_loc.db" : "sms_loc.db");
        Log.d("", "FILE PATH IS: " + filePath);
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READONLY);
            Log.d("", (db == null) ? "NO DATABASE" : "DATABASE FOUND");

            Cursor mCur = db.rawQuery("SELECT * FROM data", null);
            int cnt = 0;
            while (mCur.moveToNext()) {
                double lat = mCur.getFloat(4);
                double lon = mCur.getFloat(5);
                MyItem offsetItem = new MyItem(lat, lon, 0);
                ((db_type == 0) ? mClusterManager1 : mClusterManager2).addItem(offsetItem);
                cnt++;
            }
            Log.d("", ""+cnt);
            mCur.close();

        } catch(Exception e) {
            Log.d("","Database not found.. :(");
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        mMap.moveCamera(CameraUpdateFactory.newLatLng((currLocation != null) ? currLocation : new LatLng(40.3440, 74.6514)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mClusterManager1 = new ClusterManager<>(this, mMap);
        mClusterManager1.setRenderer(new CustomClusterIcon(0, mClusterManager1));
        mClusterManager2 = new ClusterManager<>(this, mMap);
        mClusterManager2.setRenderer(new CustomClusterIcon(1, mClusterManager2));
        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(mClusterManager1);
        mMap.setOnMarkerClickListener(mClusterManager1);
        mMap.setOnCameraIdleListener(mClusterManager2);
        mMap.setOnMarkerClickListener(mClusterManager2);

        addMarkers(0);
        addMarkers(1);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawer(GravityCompat.START);
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
        return (item.getItemId() == R.id.action_settings) ? true : super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        FragmentManager fm = getFragmentManager();
        android.support.v4.app.FragmentManager sfm = getSupportFragmentManager();

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (sMap.isAdded()) sfm.beginTransaction().hide(sMap).commit();

        FrameLayout layout = (FrameLayout)findViewById(R.id.mainFrame);
        layout.setVisibility(View.VISIBLE);

        if (id == R.id.nav_home) {
            fragmentReplacer(fm, new FragmentHome());
        } else if (id == R.id.nav_loc) {
            mapFragmentHelper(sfm, layout);
        } else if (id == R.id.nav_act) {
            fragmentReplacer(fm, new FragmentAct());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void mapFragmentHelper(android.support.v4.app.FragmentManager sfm, FrameLayout layout) {
        layout.setVisibility(View.GONE);
        if(!sMap.isAdded())
            sfm.beginTransaction().add(R.id.map, sMap).commit();
        else
            sfm.beginTransaction().show(sMap).commit();
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
        currLocation = (mLastLocation != null) ? new LatLng(mLastLocation.getLatitude(),
                mLastLocation.getLongitude()) : null;
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
