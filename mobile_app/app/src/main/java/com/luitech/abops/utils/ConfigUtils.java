package com.luitech.abops.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.luitech.abops.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ConfigUtils {

    public static String getApiKey(@NonNull Context context) {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.config);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.getString("api_key");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
