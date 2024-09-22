package com.luitech.abops.network;

import com.luitech.abops.GpioRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface GpioInterface {
    @POST("abops/updateState.php")
    Call<ResponseBody> updateGpioState(@Body GpioRequest request);
}
