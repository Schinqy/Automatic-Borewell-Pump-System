package com.luitech.abops;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.luitech.abops.network.ApiClient;
import com.luitech.abops.network.DataInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WaterUsageActivity extends AppCompatActivity {
    private BarChart barChart;
    private TextView waterUsageTextView;
    private Spinner spinnerInterval;
    private MaterialButton btnSetInterval;
    private Handler handler;
    private Runnable updateTask;
    private String device_id = "ABOPS_ID0001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_usage);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Water Usage");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        barChart = findViewById(R.id.barChart);
        waterUsageTextView = findViewById(R.id.waterUsageTextView);
        spinnerInterval = findViewById(R.id.spinnerInterval);
        btnSetInterval = findViewById(R.id.btnSetInterval);

        handler = new Handler();

        btnSetInterval.setOnClickListener(v -> {
            handler.removeCallbacks(updateTask);
            handler.post(updateTask);
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

    private void populateBarChart(ArrayList<BarEntry> barEntries, ArrayList<String> xAxisLabels) {
        BarDataSet barDataSet = new BarDataSet(barEntries, "Water Usage");
        barDataSet.setColor(ColorTemplate.MATERIAL_COLORS[0]);
        barDataSet.setValueTextColor(getResources().getColor(R.color.white));
        barDataSet.setValueTextSize(12f);

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getResources().getColor(R.color.white));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisLabels));

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.white));

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setTextColor(getResources().getColor(R.color.white));
        barChart.invalidate();
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
                                ArrayList<BarEntry> barEntries = new ArrayList<>();
                                ArrayList<String> xAxisLabels = new ArrayList<>();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                                int intervalPosition = spinnerInterval.getSelectedItemPosition();
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
                                double totalWaterUsage = 0;

                                for (int i = 0; i < dataArray.length(); i++) {
                                    JSONObject data = dataArray.getJSONObject(i);
                                    String timestampStr = data.optString("timestamp");

                                    Date date = dateFormat.parse(timestampStr);
                                    if (date != null && date.after(startDate)) {
                                        float flowRate = (float) data.optDouble("flow_rate", 0);
                                        // Calculate water usage (assuming flow rate is in liters per minute)
                                        double waterUsage = flowRate * 5 / 60.0; // 5 seconds in hours
                                        totalWaterUsage += waterUsage;

                                        barEntries.add(new BarEntry(barEntries.size(), (float) totalWaterUsage));
                                        xAxisLabels.add(new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date));
                                    }
                                }

                                populateBarChart(barEntries, xAxisLabels);

                                // Update water usage text view
                                double finalTotalWaterUsage = totalWaterUsage;
                                runOnUiThread(() -> {
                                    String intervalText = spinnerInterval.getSelectedItem().toString();
                                    waterUsageTextView.setText(String.format(Locale.getDefault(),
                                            "Water Usage (%s): %.2f liters", intervalText, finalTotalWaterUsage));
                                });

                            } else {
                                waterUsageTextView.setText("No data found.");
                            }
                        } else {
                            waterUsageTextView.setText("Error: " + status);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        waterUsageTextView.setText("Failed to parse data.");
                    }
                } else {
                    waterUsageTextView.setText("Request failed.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                waterUsageTextView.setText("Failed to fetch data.");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(updateTask);
        }
    }
}