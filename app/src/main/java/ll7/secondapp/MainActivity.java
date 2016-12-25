package ll7.secondapp;

import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.*;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.*;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    SupportMapFragment sMapFragment;
    final private int myLocationId = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sMapFragment = SupportMapFragment.newInstance();
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

        Fragment fragment = new FragmentHome();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.mainFrame, fragment);
        ft.commit();

        sMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        return;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else super.onBackPressed();
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

        if(sMapFragment.isAdded())
            sfm.beginTransaction().hide(sMapFragment).commit();

        FrameLayout layout = (FrameLayout)findViewById(R.id.mainFrame);
        layout.setVisibility(View.VISIBLE); // you can use INVISIBLE also instead of GONE

        if (id == R.id.nav_home) {
            helper(fm, new FragmentHome());
        } else if (id == R.id.nav_call_loc) {
            layout.setVisibility(View.GONE); // you can use INVISIBLE also instead of GONE
            if(!sMapFragment.isAdded())
                sfm.beginTransaction().add(R.id.map, sMapFragment).commit();
            else
                sfm.beginTransaction().show(sMapFragment).commit();
        } else if (id == R.id.nav_sms_loc) {
            layout.setVisibility(View.GONE); // you can use INVISIBLE also instead of GONE
            if(!sMapFragment.isAdded())
                sfm.beginTransaction().add(R.id.map, sMapFragment).commit();
            else
                sfm.beginTransaction().show(sMapFragment).commit();
            helper(fm, new FragmentCallAct());
        } else if (id == R.id.nav_call_act) {
            helper(fm, new FragmentCallAct());
        } else if (id == R.id.nav_sms_act) {
            helper(fm, new FragmentCallAct());
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void helper(FragmentManager fm, Fragment fragment) {
        fm.beginTransaction().replace(R.id.mainFrame, fragment).commit();
    }
}
