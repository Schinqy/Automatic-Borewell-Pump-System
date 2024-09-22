package com.luitech.abops;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageSwitcher;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.luitech.abops.network.ApiClient;
import com.luitech.abops.network.DataInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.BreakIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private CardView flowRateCardView;
    private CardView waterLevelCardView;
    private TextView waterLevelTextView;
    private TextView flowRateTextView;
    private DeviceStatusChecker statusChecker;
    private TextView deviceStatusTextView;
    private AppCompatImageView deviceStatusIcon;
    private Handler handler;
    private Runnable updateTask;
    private String device_id = "ABOPS_ID0001";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        flowRateCardView = findViewById(R.id.flowRateCard);
        waterLevelCardView = findViewById(R.id.waterLevelCard);
        waterLevelTextView = findViewById(R.id.waterLevel);
        flowRateTextView = findViewById(R.id.flowRate);
        deviceStatusTextView =  findViewById(R.id.deviceStatus);
        deviceStatusIcon =  findViewById(R.id.deviceStatusIcon);



        flowRateCardView.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, GraphActivity.class);
            intent.putExtra("graph_type", "flow");
            startActivity(intent);
        });

        waterLevelCardView.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, GraphActivity.class);
            intent.putExtra("graph_type", "water");
            startActivity(intent);
        });
        statusChecker = new DeviceStatusChecker(); // Initialize statusChecker
        handler = new Handler();
        updateTask = new Runnable() {
            @Override
            public void run() {
                fetchData(device_id);
                //fetchNotifications();
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        handler.post(updateTask);



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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
                        // Log.d("DATA RECEIVED", "Data Received" + responseBody);
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String status = jsonObject.optString("status");

                        if ("success".equals(status)) {
                            JSONArray dataArray = jsonObject.optJSONArray("data");
                            if (dataArray != null && dataArray.length() > 0) {
                                JSONObject latestData = null;
                                long latestTimestamp = Long.MIN_VALUE;

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

                                    if (timestamp > latestTimestamp) {
                                        latestTimestamp = timestamp;
                                        latestData = data;
                                    }
                                }

                                if (latestData != null) {
                                    String waterLevel = latestData.optString("water_level");
                                    String flowRate = latestData.optString("flow_rate");
                                    String timestamp = latestData.optString("timestamp");


                                        waterLevelTextView.setText(waterLevel + " L");
                                        flowRateTextView.setText((flowRate + " L/s"));

                                    boolean isOnline = statusChecker.isDeviceOnline(timestamp);


                                    if (isOnline) {
                                        deviceStatusTextView.setText("Device Online");
                                        deviceStatusIcon.setImageResource(R.drawable.ic_online);
                                    } else {
                                        deviceStatusTextView.setText("Last Seen: " + timestamp);
                                        deviceStatusIcon.setImageResource(R.drawable.ic_offline);

                                    }
                                } else {
                                    deviceStatusTextView.setText("No data found.");
                                }
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