package ll7.secondapp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentAct extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_act, container, false);

        BarChart barChart = (BarChart) view.findViewById(R.id.chart);
        ArrayList<BarEntry> entries1 = new ArrayList<>();
        ArrayList<BarEntry> entries2 = new ArrayList<>();

        String filePath1 = Environment.getExternalStorageDirectory() + "/ll7.secondapp/correlated/" + "call_act.db";
        Log.d("", "FILE PATH IS: " + filePath1);
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(filePath1, null, SQLiteDatabase.OPEN_READONLY);
            if (db == null) Log.d("", "NO DATABASE");
            else Log.d("", "DATABASE FOUND");

            entries1.add(new BarEntry(getCount(db, 0), 0));
            entries1.add(new BarEntry(getCount(db, 1), 1));
            entries1.add(new BarEntry(getCount(db, 2), 2));
        } catch(Exception e) {
            Log.d("","Database not found.. :(");
        }

        String filePath2 = Environment.getExternalStorageDirectory() + "/ll7.secondapp/correlated/" + "sms_act.db";
        Log.d("", "FILE PATH IS: " + filePath2);
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(filePath2, null, SQLiteDatabase.OPEN_READONLY);
            if (db == null) Log.d("", "NO DATABASE");
            else Log.d("", "DATABASE FOUND");

            entries2.add(new BarEntry(getCount(db, 0), 0));
            entries2.add(new BarEntry(getCount(db, 1), 1));
            entries2.add(new BarEntry(getCount(db, 2), 2));
        } catch(Exception e) {
            Log.d("","Database not found.. :(");
        }

        BarDataSet calls = new BarDataSet(entries1, "Number of Calls");
        calls.setColors(ColorTemplate.COLORFUL_COLORS);
        BarDataSet sms = new BarDataSet(entries2, "Number of Texts");
        sms.setColors(ColorTemplate.PASTEL_COLORS);

        List<String> labels = new ArrayList<String>();
        labels.add("None");
        labels.add("Low");
        labels.add("High");

        List<IBarDataSet> dataset = new ArrayList<>();
        dataset.add(calls);
        dataset.add(sms);

        BarData data = new BarData(labels, dataset);

        barChart.setData(data);
        barChart.animateY(5000);

        barChart.setDescription("Number of Calls and Texts During Physical Activity");

        return view;
    }

    public int getCount(SQLiteDatabase db, int type_of_act) {
        Cursor mCur = db.rawQuery("SELECT count(*) FROM data WHERE type_of_act = " + type_of_act, null);
        mCur.moveToFirst();
        int cnt = mCur.getInt(0);
        mCur.close();
        return cnt;
    }
}
