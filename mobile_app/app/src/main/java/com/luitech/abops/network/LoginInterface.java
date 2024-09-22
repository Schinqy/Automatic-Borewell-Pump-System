package com.luitech.abops.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface LoginInterface {
    @FormUrlEncoded
    @POST("abops/login.php") // Specify your PHP script name
    Call<ResponseBody> loginUser(
            @Field("id") String studentId,
            @Field("password") String password
    );
}
