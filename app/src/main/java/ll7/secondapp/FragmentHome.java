package ll7.secondapp;

import android.content.*;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
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
 * Activities that contain this fragment must implement the
 * {@link FragmentHome.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentHome#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentHome extends Fragment{ //implements Probe.DataListener{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
//
//    public static final String DEFAULT_PIPE = "default_p";
//    private FunfManager funfManager;
//    private BasicPipeline mypipe;
//    private CallLogProbe callProbe;
//    private ActivityProbe actProbe;
//    private SmsProbe smsProbe;
//    private SimpleLocationProbe locProbe;
//    private Button archiveButton, scanCall, scanSMS, scanAct, scanLoc;
//    private TextView dataCountView;
//    private Handler handler;
//
//    private static final String TOTAL_COUNT_SQL = "SELECT count(*) FROM " + NameValueDatabaseHelper.DATA_TABLE.name;
//
//    /**
//     * Queries the database of the pipeline to determine how many rows of data we have recorded so far.
//     */
//    private void updateScanCount() {
//        // Query the pipeline db for the count of rows in the data table
//        SQLiteDatabase db = mypipe.getDb();
//        Cursor mcursor = db.rawQuery(TOTAL_COUNT_SQL, null);
//        mcursor.moveToFirst();
//        final int count = mcursor.getInt(0);
//        // Update interface on main thread
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                dataCountView.setText("Data Count: " + count);
//            }
//        });
//    }
//
//    public void onDataCompleted(IJsonObject probeConfig, JsonElement checkpoint) {
//        updateScanCount();
//        // Re-register to keep listening after probe completes.
//        callProbe.registerPassiveListener(FragmentHome.this);
//        smsProbe.registerPassiveListener(FragmentHome.this);
//        actProbe.registerPassiveListener(FragmentHome.this);
//        locProbe.registerPassiveListener(FragmentHome.this);
//    }
//
//    public void onDataReceived(IJsonObject probeConfig, IJsonObject data) {
//        // Re-register to keep listening after probe completes.
//        Log.d("", data.toString());
//        callProbe.registerPassiveListener(FragmentHome.this);
//        smsProbe.registerPassiveListener(FragmentHome.this);
//        actProbe.registerPassiveListener(FragmentHome.this);
//        locProbe.registerPassiveListener(FragmentHome.this);
//    }
//
//    private ServiceConnection funfManagerConn = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            funfManager = ((FunfManager.LocalBinder)service).getManager();
//
//            Gson gson = funfManager.getGson();
//            callProbe = gson.fromJson(new JsonObject(), CallLogProbe.class);
//            smsProbe = gson.fromJson(new JsonObject(), SmsProbe.class);
//            actProbe = gson.fromJson(new JsonObject(), ActivityProbe.class);
//            locProbe = gson.fromJson(new JsonObject(), SimpleLocationProbe.class);
//
//            mypipe = (BasicPipeline) funfManager.getRegisteredPipeline(DEFAULT_PIPE);
//
//            callProbe.registerPassiveListener(FragmentHome.this);
//            smsProbe.registerPassiveListener(FragmentHome.this);
//            actProbe.registerPassiveListener(FragmentHome.this);
//            locProbe.registerPassiveListener(FragmentHome.this);
//
//            if (funfManager != null) {
//                funfManager.enablePipeline(DEFAULT_PIPE);
//                mypipe = (BasicPipeline) funfManager.getRegisteredPipeline(DEFAULT_PIPE);
//            }
//
//            // Set UI ready to use, by enabling buttons
//            archiveButton.setEnabled(true);
//            scanCall.setEnabled(true);
//            scanSMS.setEnabled(true);
//            scanAct.setEnabled(true);
//            scanLoc.setEnabled(true);
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            funfManager = null;
//        }
//    };

    private OnFragmentInteractionListener mListener;

    public FragmentHome() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentHome.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentHome newInstance(String param1, String param2) {
        FragmentHome fragment = new FragmentHome();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

//        // Displays the count of rows in the data
//        dataCountView = (TextView) getActivity().findViewById(R.id.dataCountText);
//
//        // Used to make interface changes on main thread
//        handler = new Handler();
//
//        // Runs an archive if pipeline is enabled
//        archiveButton = (Button) getActivity().findViewById(R.id.archiveButton);
//        archiveButton.setEnabled(false);
//        archiveButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mypipe.onRun(BasicPipeline.ACTION_ARCHIVE, null);
//                handler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getActivity().getBaseContext(), "Archived!", Toast.LENGTH_SHORT).show();
//                        updateScanCount();
//                    }
//                }, 1000L);
//            }
//        });
//
//        // Bind to the service, to create the connection with FunfManager
//        getActivity().bindService(new Intent(this.getActivity(), FunfManager.class), funfManagerConn, getActivity().BIND_AUTO_CREATE);
//
//        // Forces the pipeline to scan now
//        scanCall = (Button) getActivity().findViewById(R.id.scanCall);
//        scanCall.setEnabled(false);
//        scanCall.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callProbe.registerListener(mypipe);
//            }
//        });
//
//        scanSMS = (Button) getActivity().findViewById(R.id.scanSMS);
//        scanSMS.setEnabled(false);
//        scanSMS.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                smsProbe.registerListener(mypipe);
//            }
//        });
//
//        scanAct = (Button) getActivity().findViewById(R.id.scanAct);
//        scanAct.setEnabled(false);
//        scanAct.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                actProbe.registerListener(mypipe);
//            }
//        });
//
//        scanLoc = (Button) getActivity().findViewById(R.id.scanLoc);
//        scanLoc.setEnabled(false);
//        scanLoc.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                locProbe.registerListener(mypipe);
//                Log.d("", "location!!");
//            }
//        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}