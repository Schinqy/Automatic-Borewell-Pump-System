package com.luitech.abops;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceStatusChecker {

    // Method to check if the device is online based on timestamp
    public boolean isDeviceOnline(String receivedTimestamp) {
        long acceptableRange = 15000; // 15 seconds

        // Get the current timestamp
        long currentTimestamp = System.currentTimeMillis();

        // Parse the received timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date receivedDate = dateFormat.parse(receivedTimestamp);
            if (receivedDate != null) {
                long receivedTimestampMillis = receivedDate.getTime();

                // Calculate the difference between the current and received timestamps
                long difference = currentTimestamp - receivedTimestampMillis;

                // Check if the difference is within the acceptable range
                return difference <= acceptableRange;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Return false if timestamp parsing fails or device is out of range
        return false;
    }
}
