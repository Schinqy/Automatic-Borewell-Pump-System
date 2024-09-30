package com.luitech.abops;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.luitech.abops.network.ApiClient;
import com.luitech.abops.network.DataInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private ArrayList<String> xAxisLabels; // To hold formatted datetime labels

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_graph);
        lineChart = findViewById(R.id.lineChart);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        Intent intent = getIntent();
        graph_type = intent.getStringExtra("graph_type");

        // Set title based on graph_type
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

        statusChecker = new DeviceStatusChecker(); // Initialize statusChecker
        handler = new Handler();
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


    // Fetch data and format datetime for the X-axis
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
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject data = dataArray.getJSONObject(i);
                                    String timestampStr = data.optString("timestamp");

                                    long timestamp;
                                    try {
                                        Date date = dateFormat.parse(timestampStr);
                                        if (date != null) {
                                            timestamp = date.getTime();
                                        } else {
                                            continue;
                                        }
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        continue;
                                    }

                                    float value;
                                    if ("water".equals(graph_type)) {
                                        value = (float) data.optDouble("water_level", 0);
                                    } else {
                                        value = (float) data.optDouble("flow_rate", 0);
                                    }

                                    graphEntries.add(new Entry(timestamp, value));
                                }

                                populateGraph(graphEntries);
                                lineChart.invalidate();  // This refreshes the chart every 5 seconds
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
