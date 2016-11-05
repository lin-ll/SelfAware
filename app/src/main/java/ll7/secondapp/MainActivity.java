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

    public static final String CALL_PIPE = "call_p", SMS_PIPE = "sms_p", ACT_PIPE = "act_p";
    private FunfManager funfManager;
    private BasicPipeline callpipe, smspipe, actpipe;
    private CallLogProbe callProbe;
    private ActivityProbe actProbe;
    private SmsProbe smsProbe;
    private CheckBox enabledCall, enabledSMS, enabledAct;
    private Button archiveButton, scanCall, scanSMS, scanAct;
    private TextView dataCountView;
    private Handler handler;

    private static final String TOTAL_COUNT_SQL = "SELECT count(*) FROM " + NameValueDatabaseHelper.DATA_TABLE.name;

    public int updateHelper(BasicPipeline pipe) {
        SQLiteDatabase db = pipe.getDb();
        Cursor mcursor = db.rawQuery(TOTAL_COUNT_SQL, null);
        mcursor.moveToFirst();
        return mcursor.getInt(0);
    }
    /**
     * Queries the database of the pipeline to determine how many rows of data we have recorded so far.
     */
    private void updateScanCount() {
        // Query the pipeline db for the count of rows in the data table
        final int count = updateHelper(callpipe) + updateHelper(smspipe) + updateHelper(actpipe);
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
    }

    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        // Re-register to keep listening after probe completes.
        Log.d("", data.toString());
        callProbe.registerPassiveListener(MainActivity.this);
        smsProbe.registerPassiveListener(MainActivity.this);
        actProbe.registerPassiveListener(MainActivity.this);
    }

    public void pipelineHelper(int pipe, String which, boolean isChecked) {
        if (funfManager != null) {
            if (isChecked) {
                funfManager.enablePipeline(which);
                if (pipe == 0) {
                    callpipe = (BasicPipeline) funfManager.getRegisteredPipeline(which);
                } else if (pipe == 1) {
                    smspipe = (BasicPipeline) funfManager.getRegisteredPipeline(which);
                } else if (pipe == 2) {
                    actpipe = (BasicPipeline) funfManager.getRegisteredPipeline(which);
                }
            } else funfManager.disablePipeline(which);
        }
    }

    private ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            funfManager = ((FunfManager.LocalBinder)service).getManager();

            Gson gson = funfManager.getGson();
            callProbe = gson.fromJson(new JsonObject(), CallLogProbe.class);
            smsProbe = gson.fromJson(new JsonObject(), SmsProbe.class);
            actProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);

            callpipe = (BasicPipeline) funfManager.getRegisteredPipeline(CALL_PIPE);
            smspipe = (BasicPipeline) funfManager.getRegisteredPipeline(SMS_PIPE);
            actpipe = (BasicPipeline) funfManager.getRegisteredPipeline(ACT_PIPE);

            callProbe.registerPassiveListener(MainActivity.this);
            smsProbe.registerPassiveListener(MainActivity.this);
            actProbe.registerPassiveListener(MainActivity.this);

            // This checkbox enables or disables the pipeline
            enabledCall.setChecked(callpipe.isEnabled());
            enabledCall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pipelineHelper(0, CALL_PIPE, isChecked);
                }
            });

            enabledSMS.setChecked(smspipe.isEnabled());
            enabledSMS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                pipelineHelper(1, SMS_PIPE, isChecked);
                }
            });

            enabledAct.setChecked(actpipe.isEnabled());
            enabledAct.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    pipelineHelper(2, ACT_PIPE, isChecked);
                }
            });

            // Set UI ready to use, by enabling buttons
            enabledCall.setEnabled(true);
            enabledSMS.setEnabled(true);
            enabledAct.setEnabled(true);
            archiveButton.setEnabled(true);
            scanCall.setEnabled(true);
            scanSMS.setEnabled(true);
            scanAct.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            funfManager = null;
        }
    };

    public void handlerHelper() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
                updateScanCount();
            }
        }, 1000L);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//            .setAction("Action", null).show();
//            }
//        });

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

        // Checkboxes
        enabledCall = (CheckBox) findViewById(R.id.enabledCall);
        enabledCall.setEnabled(false);
        enabledSMS = (CheckBox) findViewById(R.id.enabledSMS);
        enabledSMS.setEnabled(false);
        enabledAct = (CheckBox) findViewById(R.id.enabledAct);
        enabledAct.setEnabled(false);

        // Runs an archive if pipeline is enabled
        archiveButton = (Button) findViewById(R.id.archiveButton);
        archiveButton.setEnabled(false);
        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            if (!callpipe.isEnabled() && !smspipe.isEnabled() && !actpipe.isEnabled()) {
                Toast.makeText(getBaseContext(), "No pipeline is enabled.", Toast.LENGTH_SHORT).show();
            } else {
                if (callpipe.isEnabled()) callpipe.onRun(BasicPipeline.ACTION_ARCHIVE, null);
                if (smspipe.isEnabled()) smspipe.onRun(BasicPipeline.ACTION_ARCHIVE, null);
                if (actpipe.isEnabled()) actpipe.onRun(BasicPipeline.ACTION_ARCHIVE, null);
                handlerHelper();
            }
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
                if (callpipe.isEnabled()) {
                    callProbe.registerListener(callpipe);
                } else Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
            }
        });

        scanSMS = (Button) findViewById(R.id.scanSMS);
        scanSMS.setEnabled(false);
        scanSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (smspipe.isEnabled()) {
                    smsProbe.registerListener(smspipe);
                } else Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
            }
        });

        scanAct = (Button) findViewById(R.id.scanAct);
        scanAct.setEnabled(false);
        scanAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (actpipe.isEnabled()) {
                    actProbe.registerListener(actpipe);
                } else Toast.makeText(getBaseContext(), "Pipeline is not enabled.", Toast.LENGTH_SHORT).show();
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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
