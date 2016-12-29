package ll7.secondapp;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ActivityProbe;
import edu.mit.media.funf.probe.builtin.CallLogProbe;
import edu.mit.media.funf.probe.builtin.SimpleLocationProbe;
import edu.mit.media.funf.probe.builtin.SmsProbe;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentHome extends Fragment implements Probe.DataListener {
    private static final String DEFAULT_PIPE = "default_p";
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
        mcursor.close();
        // Update interface on main thread
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataCountView.setText("Data Count: " + count);
            }
        });
    }

    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
        updateScanCount();
        // Re-register to keep listening after probe completes.
        callProbe.registerPassiveListener(FragmentHome.this);
        smsProbe.registerPassiveListener(FragmentHome.this);
        actProbe.registerPassiveListener(FragmentHome.this);
        locProbe.registerPassiveListener(FragmentHome.this);
    }

    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
        // Re-register to keep listening after probe completes.
        callProbe.registerPassiveListener(FragmentHome.this);
        smsProbe.registerPassiveListener(FragmentHome.this);
        actProbe.registerPassiveListener(FragmentHome.this);
        locProbe.registerPassiveListener(FragmentHome.this);
    }

    private final ServiceConnection funfManagerConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            funfManager = ((FunfManager.LocalBinder)service).getManager();

            Gson gson = funfManager.getGson();
            callProbe = gson.fromJson(new JsonObject(), CallLogProbe.class);
            smsProbe = gson.fromJson(new JsonObject(), SmsProbe.class);
            actProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
            locProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);

            mypipe = (BasicPipeline) funfManager.getRegisteredPipeline(DEFAULT_PIPE);

            callProbe.registerPassiveListener(FragmentHome.this);
            smsProbe.registerPassiveListener(FragmentHome.this);
            actProbe.registerPassiveListener(FragmentHome.this);
            locProbe.registerPassiveListener(FragmentHome.this);

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Displays the count of rows in the data
        dataCountView = (TextView) view.findViewById(R.id.dataCountText);

        // Used to make interface changes on main thread
        handler = new Handler();

        // Runs an archive if pipeline is enabled
        archiveButton = (Button) view.findViewById(R.id.archiveButton);
        archiveButton.setEnabled(false);
        archiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("","archive");
                mypipe.onRun(BasicPipeline.ACTION_ARCHIVE, null);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity().getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
                        updateScanCount();
                    }
                }, 1000L);
            }
        });

        // Bind to the service, to create the connection with FunfManager
        getActivity().bindService(new Intent(this.getActivity(), FunfManager.class), funfManagerConn, Context.BIND_AUTO_CREATE);

        // Forces the pipeline to scan now
        scanCall = (Button) view.findViewById(R.id.scanCall);
        scanCall.setEnabled(false);
        scanCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callProbe.registerListener(mypipe);
            }
        });

        scanSMS = (Button) view.findViewById(R.id.scanSMS);
        scanSMS.setEnabled(false);
        scanSMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                smsProbe.registerListener(mypipe);
            }
        });

        scanAct = (Button) view.findViewById(R.id.scanAct);
        scanAct.setEnabled(false);
        scanAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actProbe.registerListener(mypipe);
            }
        });

        scanLoc = (Button) view.findViewById(R.id.scanLoc);
        scanLoc.setEnabled(false);
        scanLoc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) { locProbe.registerListener(mypipe); }
        });

        return view;
    }
}
