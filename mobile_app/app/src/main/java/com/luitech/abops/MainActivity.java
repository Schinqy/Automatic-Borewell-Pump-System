package com.luitech.abops;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ImageSwitcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.luitech.abops.adapters.NotificationAdapter;
import com.luitech.abops.network.ApiClient;
import com.luitech.abops.network.ControlInterface;
import com.luitech.abops.network.DataInterface;
import com.luitech.abops.network.GetStateInterface;
import com.luitech.abops.network.GpioInterface;
import com.luitech.abops.network.NotificationsInterface;
import com.luitech.abops.utils.ConfigUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.BreakIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    private TextView deviceId;
    private SwitchMaterial controlSwitch;
    private LabeledSwitch pumpSwitch;

    private boolean overrideState = false;

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;


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
        deviceId = findViewById(R.id.deviceId);
        controlSwitch = findViewById(R.id.switchOverride); //override Switch
        pumpSwitch = findViewById(R.id.switchPump);

        // Initialize RecyclerView and Adapter
        recyclerView = findViewById(R.id.recyclerViewAlerts);


        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            List<NotificationModel> notificationList = new ArrayList<>();
            adapter = new NotificationAdapter(notificationList);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e("MainActivity", "RecyclerView is null. Check the ID in the XML layout.");
        }

        fetchNotifications(); // Load notifications from API



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
                fetchNotifications();
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        handler.post(updateTask);


        controlSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int state = isChecked ? 1 : 0;

                sendControlRequest(device_id, state);
                overrideState = isChecked;
                if (isChecked) {
                    updateInitialSwitchStates(device_id); // Fetch and update switch states
                }
                updateSwitchState();


            }
        });

        pumpSwitch.setOnToggledListener((buttonView, isOn) -> {
            if (overrideState) {
                updateGpioState(device_id, "relay", isOn ? 1 : 0);
            }
            else
            {
                pumpSwitch.setOn(false); // Reset switch if override is off
                showToast("Override switch is off.");
            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void updateSwitchState() {
        pumpSwitch.setEnabled(overrideState);
    }
    private void updateGpioState(String boardId, String name, int state) {
        GpioInterface gpioInterface = ApiClient.getGpioInterface();

        try {
            // Read API key from file
            String apiKey = ConfigUtils.getApiKey(getApplicationContext());

            // Create request object
            GpioRequest request = new GpioRequest(boardId, name, state, apiKey);

            // Make the request
            Call<ResponseBody> call = gpioInterface.updateGpioState(request);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        showToast("GPIO state updated successfully.");
                    } else {
                        showToast("Failed to update GPIO state: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    showToast("Request failed: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Failed to create request object.");
        }
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
                                   float waterLevelPercentage = (Float.parseFloat(waterLevel) /5000) * 100;


                                        waterLevelTextView.setText(waterLevel + " L (" + waterLevelPercentage + "%)");
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

    private void sendControlRequest(String boardId, int autonomy) {
        ControlInterface controlInterface = ApiClient.getControlInterface();
        ControlRequest request = new ControlRequest(boardId, autonomy);
        Call<ResponseBody> call = controlInterface.updateAutonomy(request);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        Toast.makeText(MainActivity.this, "Success: " + responseBody, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Failed to read response.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Request failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateInitialSwitchStates(String manholeId) {
        GetStateInterface getStateInterface = ApiClient.getStateInterface();

        Call<ResponseBody> call = getStateInterface.getState(manholeId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        Log.d("GET STATES", responseBody);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String name = jsonObject.getString("name");
                            int state = jsonObject.getInt("state");
                            if ("relay".equals(name)) {
                                pumpSwitch.setOn(state == 1);  // Assuming 'state' is either 0 or 1
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        showToast("Failed to parse data.");
                    }
                } else {
                    showToast("Failed to fetch state.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showToast("Request failed: " + t.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }
    private void fetchNotifications() {
        NotificationsInterface notificationsInterface = ApiClient.getNotificationsInterface();
        Call<List<NotificationModel>> call = notificationsInterface.getNotifications();

        call.enqueue(new Callback<List<NotificationModel>>() {
            @Override
            public void onResponse(Call<List<NotificationModel>> call, Response<List<NotificationModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NotificationModel> notifications = response.body();
                    // Log the notifications to check the data
                    for (NotificationModel notification : notifications) {
                        Log.d("Notification", "Heading: " + notification.getBoardId() + ", Message: " + notification.getText() + ", Timestamp: " + notification.getTimestamp());
                    }
                    adapter = new NotificationAdapter(notifications);
                    recyclerView.setAdapter(adapter);
                } else {
                    Log.e("NotificationsActivity", "Error fetching notifications: " + response.message());
                }
            }


            @Override
            public void onFailure(Call<List<NotificationModel>> call, Throwable t) {
                Log.e("NotificationsActivity", "Failure: " + t.getMessage());
            }
        });
    }


}