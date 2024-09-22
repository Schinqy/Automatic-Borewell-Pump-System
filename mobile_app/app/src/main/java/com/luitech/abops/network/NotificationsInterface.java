package com.luitech.abops.network;

import com.luitech.abops.NotificationModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface NotificationsInterface {
    @GET("abops/getNotifications.php")
    Call<List<NotificationModel>> getNotifications();
}
