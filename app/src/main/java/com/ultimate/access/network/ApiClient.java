package com.ultimate.access.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    // ⚠️⚠️⚠️ IMPORTANT - RENDER URL YAHAN DALO (DEPLOY KE BAAD) ⚠️⚠️⚠️
    // Example: "https://your-app-name.onrender.com/"
    public static String BASE_URL = "https://amaan-git.onrender.com/";

    private static Retrofit retrofit = null;
    private static OkHttpClient client = null;

    public static Retrofit getClient() {
        if (retrofit == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static void setBaseUrl(String url) {
        BASE_URL = url;
        retrofit = null;
        client = null;
    }
}
