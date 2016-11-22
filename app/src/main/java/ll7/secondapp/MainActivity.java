package ll7.secondapp;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.*;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.*;

import com.google.gson.*;

import edu.mit.media.funf.*;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.*;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.*;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Probe.DataListener {

    public static final String DEFAULT_PIPE = "default_p";
    private FunfManager funfManager;
    private BasicPipeline mypipe;
    private CallLogProbe callProbe;
    private ActivityProbe actProbe;
    private SmsProbe smsProbe;
    private SimpleLocationProbe locProbe;
    private Button archiveButton, scanCall, scanSMS, scanAct, scanLoc;
    private TextView dataCountView;
    private Handler handler;

    private static final String TOTAL_COUNT_SQL = "SELECT count(*) FROM " + NameValueDatabaseHelper.DATA_TABLE.name;

    /**
     * Queries the database of the pipeline to determine how many rows of data we have recorded so far.
     */
    private void updateScanCount() {
        // Query the pipeline db for the count of rows in the data table
        SQLiteDatabase db = mypipe.getDb();
        Cursor mcursor = db.rawQuery(TOTAL_COUNT_SQL, null);
        mcursor.moveToFirst();
        final int count = mcursor.getInt(0);
        // Update interface on main thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataCountView.setText("Data Count: " + count);
            }
        });
    }

    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        updateScanCount();
        // Re-register to keep listening after probe completes.
        callProbe.registerPassiveListener(MainActivity.this);
        smsProbe.registerPassiveListener(MainActivity.this);
        actProbe.registerPassiveListener(MainActivity.this);
        locProbe.registerPassiveListener(MainActivity.this);
    }

    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        // Re-register to keep listening after probe completes.
        Log.d("", data.toString());
        callProbe.registerPassiveListener(MainActivity.this);
        smsProbe.registerPassiveListener(MainActivity.this);
        actProbe.registerPassiveListener(MainActivity.this);
        locProbe.registerPassiveListener(MainActivity.this);
    }

    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            funfManager = ((FunfManager.LocalBinder)service).getManager();

            Gson gson = funfManager.getGson();
            callProbe = gson.fromJson(new JsonObject(), CallLogProbe.class);
            smsProbe = gson.fromJson(new JsonObject(), SmsProbe.class);
            actProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
            locProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);

            mypipe = (BasicPipeline) funfManager.getRegisteredPipeline(DEFAULT_PIPE);

            callProbe.registerPassiveListener(MainActivity.this);
            smsProbe.registerPassiveListener(MainActivity.this);
            actProbe.registerPassiveListener(MainActivity.this);
            locProbe.registerPassiveListener(MainActivity.this);

            if (funfManager != null) {
                funfManager.enablePipeline(DEFAULT_PIPE);
                mypipe = (BasicPipeline) funfManager.getRegisteredPipeline(DEFAULT_PIPE);
            }

            // Set UI ready to use, by enabling buttons
            archiveButton.setEnabled(true);
            scanCall.setEnabled(true);
            scanSMS.setEnabled(true);
            scanAct.setEnabled(true);
            scanLoc.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            funfManager = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Displays the count of rows in the data
        dataCountView = (TextView) findViewById(R.id.dataCountText);

        // Used to make interface changes on main thread
        handler = new Handler();

        // Runs an archive if pipeline is enabled
        archiveButton = (Button) findViewById(R.id.archiveButton);
        archiveButton.setEnabled(false);
        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mypipe.onRun(BasicPipeline.ACTION_ARCHIVE, null);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
                        updateScanCount();
                    }
                }, 1000L);
            }
        });

        // Bind to the service, to create the connection with FunfManager
        bindService(new Intent(this, FunfManager.class), funfManagerConn, BIND_AUTO_CREATE);

        // Forces the pipeline to scan now
        scanCall = (Button) findViewById(R.id.scanCall);
        scanCall.setEnabled(false);
        scanCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callProbe.registerListener(mypipe);
            }
        });

        scanSMS = (Button) findViewById(R.id.scanSMS);
        scanSMS.setEnabled(false);
        scanSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smsProbe.registerListener(mypipe);
            }
        });

        scanAct = (Button) findViewById(R.id.scanAct);
        scanAct.setEnabled(false);
        scanAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actProbe.registerListener(mypipe);
            }
        });

        scanLoc = (Button) findViewById(R.id.scanLoc);
        scanLoc.setEnabled(false);
        scanLoc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                locProbe.registerListener(mypipe);
                Log.d("", "location!!");
            }
        });
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
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the home action
        } else if (id == R.id.nav_calls) {

        } else if (id == R.id.nav_sms) {

        } else if (id == R.id.nav_act) {

        } else if (id == R.id.nav_loc) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
