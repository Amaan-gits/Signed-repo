package com.ultimate.access.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    // ⚠️ YAHAN APNA RENDER URL DALO (LAST ME SLASH / ZAROORI HAI)
    public static String BASE_URL = "https://signed-repo-1.onrender.com/";

    private static Retrofit retrofit = null;
    private static OkHttpClient client = null;

    public static Retrofit getClient() {

        if (retrofit == null) {

            // Logging interceptor — network calls ko log karega
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // OkHttpClient — timeouts and retry settings
            client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(60, TimeUnit.SECONDS)      // 60 sec connect timeout
                    .readTimeout(60, TimeUnit.SECONDS)         // 60 sec read timeout
                    .writeTimeout(60, TimeUnit.SECONDS)        // 60 sec write timeout
                    .retryOnConnectionFailure(true)            // fail pe retry karega
                    .build();

            // Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())  // plain text ke liye
                    .addConverterFactory(GsonConverterFactory.create())     // JSON ke liye
                    .build();
        }

        return retrofit;
    }

    // Agar baad me URL change karna ho to
    public static void setBaseUrl(String url) {
        BASE_URL = url;
        retrofit = null;   // naya Retrofit banega
        client = null;      // naya client banega
    }
}