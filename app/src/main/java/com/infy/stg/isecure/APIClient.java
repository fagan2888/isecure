package com.infy.stg.isecure;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class APIClient {

    private static final String TAG = APIClient.class.getName();
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build();

    public static void verify(Bitmap bitmap, APICallback callback) {
        String img = This.UTIL.convertImageToString(This.WORKING_IMAGE = bitmap);
        String json = String.format("{\"image\":\"%s\"}", img);

        OK_HTTP_CLIENT.newCall(new Request.Builder()
                .url(This.API_URL.URL_BASE + This.API_URL.PATH_VERIFY)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200)
                    callback.onResponse(response);
                else
                    callback.onResponse(null);
            }
        });

    }

    public static void encode(String id, APICallback callback) {
        String img = This.UTIL.convertImageToString(This.WORKING_IMAGE);
        String json = String.format("{\"image\":\"%s\", \"emp_id\":\"%s\"}", img, id.trim());

        OK_HTTP_CLIENT.newCall(new Request.Builder()
                .url(This.API_URL.URL_BASE + This.API_URL.PATH_ENCODE)
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code()==200)
                    callback.onResponse(response);
                else
                    callback.onResponse(null);
            }
        });

//        assertThat(response.code(), equalTo(200));

    }



    public interface APICallback {
        void onResponse(Response response);
    }
}
