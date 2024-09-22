package com.luitech.abops.network;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://lui.co.zw";
    private static Retrofit retrofit = null;

    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

        }
        return retrofit;
    }

    public static DataInterface getDataInterface() {
        return getRetrofitInstance().create(DataInterface.class);
    }

    public static ControlInterface getControlInterface()
    {
        return getRetrofitInstance().create(ControlInterface.class);
    }

    public static GpioInterface getGpioInterface()
    {
        return getRetrofitInstance().create(GpioInterface.class);
    }

    public static GetStateInterface getStateInterface()
    {
        return getRetrofitInstance().create(GetStateInterface.class);
    }

    public static TokensInterface getTokensInterface() {
        return getRetrofitInstance().create(TokensInterface.class);
    }

    public static NotificationsInterface getNotificationsInterface() {
        return getRetrofitInstance().create(NotificationsInterface.class);
    }

}