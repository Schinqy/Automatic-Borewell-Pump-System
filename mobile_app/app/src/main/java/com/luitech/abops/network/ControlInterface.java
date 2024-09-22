package com.luitech.abops.network;

import com.luitech.abops.ControlRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ControlInterface {
    @POST("abops/ctrl_app.php") // Update to the correct PHP script name
    Call<ResponseBody> updateAutonomy(@Body ControlRequest request);
}
