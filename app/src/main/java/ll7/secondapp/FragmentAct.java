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

    private static final int CALL = 0;
    private static final int SMS = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_act, container, false);

        BarChart barChart = (BarChart) view.findViewById(R.id.chart);
        ArrayList<BarEntry> entries1 = parseSQL(CALL);
        ArrayList<BarEntry> entries2 = parseSQL(SMS);

        BarDataSet calls = new BarDataSet(entries1, "Number of Calls");
        calls.setColors(ColorTemplate.COLORFUL_COLORS);
        BarDataSet sms = new BarDataSet(entries2, "Number of Texts");
        sms.setColors(ColorTemplate.PASTEL_COLORS);

        List<String> labels = new ArrayList<>();
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

    private ArrayList<BarEntry> parseSQL(int databaseNm) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String filePath = Environment.getExternalStorageDirectory() + "/ll7.secondapp/correlated/" +
                ((databaseNm == CALL) ? "call_act.db" : "sms_act.db");
        Log.d("", "FILE PATH IS: " + filePath);
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READONLY);
            Log.d("", (db == null) ? "NO DATABASE" : "DATABASE FOUND");

            entries.add(new BarEntry(getCount(db, 0), 0));
            entries.add(new BarEntry(getCount(db, 1), 1));
            entries.add(new BarEntry(getCount(db, 2), 2));
        } catch(Exception e) {
            Log.d("","Database not found.. :(");
        }

        return entries;
    }

    private int getCount(SQLiteDatabase db, int type_of_act) {
        Cursor mCur = db.rawQuery("SELECT count(*) FROM data WHERE type_of_act = " + type_of_act, null);
        mCur.moveToFirst();
        int cnt = mCur.getInt(0);
        mCur.close();
        return cnt;
    }
}
