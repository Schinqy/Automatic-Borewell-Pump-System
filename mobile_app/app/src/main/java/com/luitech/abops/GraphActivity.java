package com.luitech.abops;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.luitech.abops.network.ApiClient;
import com.luitech.abops.network.DataInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GraphActivity extends AppCompatActivity {
    private String graph_type;
    private LineChart lineChart;
    private Handler handler;
    private Runnable updateTask;
    private String device_id = "ABOPS_ID0001";
    private DeviceStatusChecker statusChecker;
    private TextView deviceStatusTextView;
    private AppCompatImageView deviceStatusIcon;
    private TextView deviceId;
    private ArrayList<String> xAxisLabels;
    private Spinner spinnerInterval;
    private MaterialButton btnSetInterval;
    private MaterialButton btnDisplayTable;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "GraphPreferences";
    private static final String PREF_INTERVAL = "SelectedInterval";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_graph);

        lineChart = findViewById(R.id.lineChart);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        spinnerInterval = findViewById(R.id.spinnerInterval);
        btnSetInterval = findViewById(R.id.btnSetInterval);
        btnDisplayTable = findViewById(R.id.btnShowTable);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        Intent intent = getIntent();
        graph_type = intent.getStringExtra("graph_type");

        if ("water".equals(graph_type)) {
            toolbar.setTitle("Water Level vs Time");
        } else {
            toolbar.setTitle("Flow Rate vs Time");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusChecker = new DeviceStatusChecker();
        handler = new Handler();

        // Set up the spinner
        int savedInterval = sharedPreferences.getInt(PREF_INTERVAL, 0);
        spinnerInterval.setSelection(savedInterval);

        spinnerInterval.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Do nothing here, we'll update when the button is clicked
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        btnSetInterval.setOnClickListener(v -> {
            int selectedInterval = spinnerInterval.getSelectedItemPosition();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(PREF_INTERVAL, selectedInterval);
            editor.apply();

            // Restart the data fetching with new interval
            handler.removeCallbacks(updateTask);
            handler.post(updateTask);
        });


        btnDisplayTable.setOnClickListener(v -> {
            Intent intentX = new Intent(GraphActivity.this, WaterUsageActivity.class);
            startActivity(intentX);
        });


        updateTask = new Runnable() {
            @Override
            public void run() {
                fetchData(device_id);
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        handler.post(updateTask);
    }

    // Populate the graph with data and format the datetime axis
    private void populateGraph(ArrayList<Entry> graphEntries) {
        LineDataSet lineDataSet = new LineDataSet(graphEntries, "Graph Data");
        lineDataSet.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        lineDataSet.setValueTextColor(getResources().getColor(R.color.white)); // Set values' text color to white
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleColor(getResources().getColor(R.color.white));
        lineDataSet.setCircleRadius(5f);
        lineDataSet.setDrawValues(false); // Disable value drawing to reduce clutter

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        // Format the X-Axis as datetime
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // Minimum interval for axis labels
        xAxis.setLabelRotationAngle(-45); // Rotate the labels to avoid overlapping
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels)); // Use formatted datetime labels
        xAxis.setTextColor(getResources().getColor(R.color.white)); // Set X-axis label color to white

        // Customize Y-Axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(getResources().getColor(R.color.white)); // Set Y-axis label color to white

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false); // Disable the right Y-axis

        lineChart.setExtraBottomOffset(10f); // Add some extra offset to prevent label cutoffs
        lineChart.getDescription().setEnabled(false); // Disable chart description
        lineChart.getLegend().setTextColor(getResources().getColor(R.color.white)); // Set legend text color to white
        lineChart.invalidate(); // Refresh the chart
    }

    private void fetchData(String manholeId) {
        DataInterface dataInterface = ApiClient.getDataInterface();
        Call<ResponseBody> call = dataInterface.getData(manholeId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String status = jsonObject.optString("status");

                        if ("success".equals(status)) {
                            JSONArray dataArray = jsonObject.optJSONArray("data");
                            if (dataArray != null && dataArray.length() > 0) {
                                ArrayList<Entry> graphEntries = new ArrayList<>();
                                xAxisLabels = new ArrayList<>();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                                int intervalPosition = sharedPreferences.getInt(PREF_INTERVAL, 0);
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);

                                switch (intervalPosition) {
                                    case 0: // Last Hour
                                        calendar.add(Calendar.HOUR, -1);
                                        break;
                                    case 1: // Today
                                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                                        calendar.set(Calendar.MINUTE, 0);
                                        break;
                                    case 2: // Current Week
                                        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                                        calendar.set(Calendar.MINUTE, 0);
                                        break;
                                    case 3: // Current Month
                                        calendar.set(Calendar.DAY_OF_MONTH, 1);
                                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                                        calendar.set(Calendar.MINUTE, 0);
                                        break;
                                    case 4: // This Year
                                        calendar.set(Calendar.DAY_OF_YEAR, 1);
                                        calendar.set(Calendar.HOUR_OF_DAY, 0);
                                        calendar.set(Calendar.MINUTE, 0);
                                        break;
                                    case 5: // All
                                        calendar.setTimeInMillis(0);
                                        break;
                                }

                                Date startDate = calendar.getTime();

                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject data = dataArray.getJSONObject(i);
                                    String timestampStr = data.optString("timestamp");

                                    Date date = dateFormat.parse(timestampStr);
                                    if (date != null && date.after(startDate)) {
                                        float value;
                                        if ("water".equals(graph_type)) {
                                            value = (float) data.optDouble("water_level", 0);
                                        } else {
                                            value = (float) data.optDouble("flow_rate", 0);
                                        }

                                        graphEntries.add(new Entry(graphEntries.size(), value));
                                        xAxisLabels.add(new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date));
                                    }
                                }

                                populateGraph(graphEntries);
                            } else {
                                deviceStatusTextView.setText("No data found.");
                            }
                        } else {
                            deviceStatusTextView.setText("Error: " + status);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        deviceStatusTextView.setText("Failed to parse data.");
                    }
                } else {
                    deviceStatusTextView.setText("Request failed.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                deviceStatusTextView.setText("Failed to fetch data.");
            }
        });
    }
}


