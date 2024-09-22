package com.luitech.abops.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DataInterface {
    @GET("abops/getData.php")

    Call<ResponseBody> getData(@Query("device_id") String manholeId);
}
