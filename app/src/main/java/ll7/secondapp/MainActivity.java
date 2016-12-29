package ll7.secondapp;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Environment;
import android.support.multidex.MultiDex;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private SupportMapFragment sMap;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LatLng currLocation;
    private ClusterManager<MyItem> mClusterManager1, mClusterManager2;
    private Collection<MyItem> callCollection, smsCollection;
    private static final int CALL = 0, SMS = 1;
    private final String[] opts = new String[]{"See Calls", "See SMS"};
    private boolean[] checkedOpts;

    private class MyItem implements ClusterItem {
        private final LatLng mPosition;
        private final int icon;

        public MyItem(double lat, double lng, int ic) { mPosition = new LatLng(lat, lng); this.icon = ic; }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }

        public BitmapDescriptor getIcon() {
            return BitmapDescriptorFactory.defaultMarker((icon == CALL) ?
                    BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_BLUE);
        }
    }

    private class CustomClusterIcon extends DefaultClusterRenderer<MyItem> {
        private final int icon_type;

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

            int draw_icon = (icon_type == CALL) ? R.drawable.ic_call_black_24dp : R.drawable.ic_chat_black_24dp;
            int color = (icon_type == CALL) ? android.R.color.holo_red_light : android.R.color.holo_blue_dark;
            final Drawable clusterIcon = getResources().getDrawable(draw_icon);
            clusterIcon.setColorFilter(getResources().getColor(color), PorterDuff.Mode.SRC_ATOP);

            ig.setBackground(clusterIcon);
            //modify padding for one or two digit numbers
            ig.setContentPadding((cluster.getSize() < 10) ? 40 : 30, 20, 0, 0);

            Bitmap icon = ig.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) { return cluster.getSize() > 1; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MultiDex.install(this);
        sMap = SupportMapFragment.newInstance();
        setContentView(R.layout.activity_main);
        checkedOpts = new boolean[]{true, true};

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
        if(mGoogleApiClient!= null) mGoogleApiClient.connect();
        else Toast.makeText(this, "Not connected...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.moveCamera(CameraUpdateFactory.newLatLng((currLocation != null) ? currLocation : new LatLng(40.3440, 74.6514)));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mClusterManager1 = mcClusterManagerSetup(CALL);
        mClusterManager2 = mcClusterManagerSetup(SMS);

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mClusterManager1.onCameraIdle();
                mClusterManager2.onCameraIdle();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mClusterManager1.onMarkerClick(marker);
                mClusterManager2.onMarkerClick(marker);
                return true;
            }
        });
    }

    private ClusterManager<MyItem> mcClusterManagerSetup(int mc) {
        ClusterManager<MyItem> mClusterManager = new ClusterManager<>(this, mMap);
        mClusterManager.setRenderer(new CustomClusterIcon(mc, mClusterManager));
        Collection<MyItem> collection = new ArrayList<>();

        String filePath = Environment.getExternalStorageDirectory() + "/ll7.secondapp/correlated/" +
                ((mc == CALL) ? "call_loc.db" : "sms_loc.db");
        Log.d("", "FILE PATH IS: " + filePath);
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READONLY);
            Log.d("", "DATABASE FOUND");

            Cursor mCur = db.rawQuery("SELECT * FROM data", null);
            int cnt = 0;
            while (mCur.moveToNext()) {
                MyItem offsetItem = new MyItem(mCur.getFloat(4), mCur.getFloat(5), mc);
                collection.add(offsetItem);
                cnt++;
            }
            Log.d("", ""+cnt);
            mCur.close();
        } catch(Exception e) { Log.d("","Database not found.. :("); }

        mClusterManager.addItems(collection);
        if (mc == CALL) callCollection = collection;
        else smsCollection = collection;
        return mClusterManager;
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
        // Handle action bar item clicks here. The action bar will automatically handle clicks on
        // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.action_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            final List<String> list = Arrays.asList(opts);

            builder.setMultiChoiceItems(opts, checkedOpts, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    // Update the current focused item's checked status
                    checkedOpts[which] = isChecked;
                    // Get the current focused item
                    String currentItem = list.get(which);
                    // Notify the current action
                    Toast.makeText(getApplicationContext(), currentItem + " is " + isChecked, Toast.LENGTH_SHORT).show();
                }
            });

            builder.setCancelable(false);
            builder.setTitle("Map Settings");

            // Set the positive/yes button click listener
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mClusterManager1.clearItems();
                    mClusterManager2.clearItems();
                    if (checkedOpts[CALL]) mClusterManager1.addItems(callCollection);
                    if (checkedOpts[SMS]) mClusterManager2.addItems(smsCollection);
                    dialog.dismiss();
                }
            });

            // Set the negative/no button click listener
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { dialog.cancel(); }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        FragmentManager fm = getFragmentManager();
        android.support.v4.app.FragmentManager sfm = getSupportFragmentManager();
        int id = item.getItemId();

        if (sMap.isAdded()) sfm.beginTransaction().hide(sMap).commit();
        FrameLayout layout = (FrameLayout)findViewById(R.id.mainFrame);
        layout.setVisibility(View.VISIBLE);

        if (id == R.id.nav_home) {
            fragmentReplacer(fm, new FragmentHome());
        } else if (id == R.id.nav_loc) {
            layout.setVisibility(View.GONE);
            if(!sMap.isAdded()) sfm.beginTransaction().add(R.id.map, sMap).commit();
            else sfm.beginTransaction().show(sMap).commit();
        } else if (id == R.id.nav_act) {
            fragmentReplacer(fm, new FragmentAct());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void fragmentReplacer(FragmentManager fm, Fragment fragment) {
        fm.beginTransaction().replace(R.id.mainFrame, fragment).commit();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Toast.makeText(this, "Failed to connect...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(Bundle arg0) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        currLocation = (mLastLocation != null) ? new LatLng(mLastLocation.getLatitude(),
                mLastLocation.getLongitude()) : null;
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        Toast.makeText(this, "Connection suspended...", Toast.LENGTH_SHORT).show();
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}