package com.luitech.abops.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface TokensInterface {
    @POST("abops/storeTokens.php")
    Call<Void> storeToken(@Body TokenRequest tokenRequest);

    class TokenRequest {
        private String token;

        public TokenRequest(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
