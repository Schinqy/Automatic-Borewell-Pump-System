package com.luitech.abops.network;

import com.luitech.abops.AutonomyRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OverrideInterface {
    @POST("abops/auto_app.php") // Specify your PHP script name for updating the autonomy value
    Call<ResponseBody> postOverrideState(@Body AutonomyRequest autonomyRequest);
}
