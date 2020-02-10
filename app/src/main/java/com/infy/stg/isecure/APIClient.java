package com.infy.stg.isecure;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    public static String URL_VERIFY = "";
    public static String URL_ENCODE = "";

    public static Bitmap bitmap;


    public static void verify(Activity activity, View view, Bitmap bitmap) {
        APIClient.bitmap = bitmap;
        String img = convertImageToStringForServer(bitmap);
        Log.d(TAG, img);
        String json = String.format("{\"image\":\"%s\"}", img);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(logging).build();

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(URL_VERIFY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                Log.d(TAG, json);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.findViewById(R.id.include).setVisibility(View.VISIBLE);
                        try {
                            Map<String, Object> map = jsonString2Map(json);
                            Log.d(TAG, map.toString());
                            ((ImageView) view.findViewById(R.id.imageView)).setImageBitmap(bitmap);
                            String identity = (String) map.get("identity");
                            if (identity.contains("not-found")) {
                                view.findViewById(R.id.floatingActionButton).setVisibility(View.VISIBLE);
                                ((TextView) view.findViewById(R.id.textView)).setText("-");
                                ((TextView) view.findViewById(R.id.textView2)).setText("-");
                            } else {
                                view.findViewById(R.id.floatingActionButton).setVisibility(View.INVISIBLE);
                                ((TextView) view.findViewById(R.id.textView)).setText("Employee #");
                                ((TextView) view.findViewById(R.id.textView2)).setText(identity);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

//        assertThat(response.code(), equalTo(200));

    }

    public static void encode(Activity activity, View view, String id) {
        String img = convertImageToStringForServer(bitmap);
        Log.d(TAG, img);
        String json = String.format("{\"image\":\"%s\", \"emp_id\":\"%s\"}", img, id.trim());

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(logging).build();

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(URL_ENCODE)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                Log.d(TAG, json);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.findViewById(R.id.include).setVisibility(View.VISIBLE);
                        try {
                            Map<String, Object> map = jsonString2Map(json);
                            Log.d(TAG, map.toString());
                            ((ImageView) view.findViewById(R.id.imageView)).setImageBitmap(bitmap);
                            String identity = (String) map.get("identity");
                            if (!identity.contains("saved")) {
                                view.findViewById(R.id.floatingActionButton).setVisibility(View.VISIBLE);
                                ((TextView) view.findViewById(R.id.textView)).setText("-");
                                ((TextView) view.findViewById(R.id.textView2)).setText("-");
                            } else {
                                view.findViewById(R.id.floatingActionButton).setVisibility(View.INVISIBLE);
                                ((TextView) view.findViewById(R.id.textView)).setText("Employee #");
                                ((TextView) view.findViewById(R.id.textView2)).setText(identity);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

//        assertThat(response.code(), equalTo(200));

    }

    public static String convertImageToStringForServer(Bitmap imageBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (imageBitmap != null) {
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT).replaceAll("\\s", "");
        } else {
            return null;
        }
    }

    public static Map<String, Object> jsonString2Map(String jsonString) throws JSONException {
        Map<String, Object> keys = new HashMap<String, Object>();

        org.json.JSONObject jsonObject = new org.json.JSONObject(jsonString); // HashMap
        Iterator<?> keyset = jsonObject.keys(); // HM

        while (keyset.hasNext()) {
            String key = (String) keyset.next();
            Object value = jsonObject.get(key);
            System.out.print("\n Key : " + key);
            if (value instanceof org.json.JSONObject) {
                System.out.println("Incomin value is of JSONObject : ");
                keys.put(key, jsonString2Map(value.toString()));
            } else if (value instanceof org.json.JSONArray) {
                org.json.JSONArray jsonArray = jsonObject.getJSONArray(key);
                //JSONArray jsonArray = new JSONArray(value.toString());
                keys.put(key, jsonArray2List(jsonArray));
            } else {
                keys.put(key, value);
            }
        }
        return keys;
    }

    public static List<Object> jsonArray2List(JSONArray arrayOFKeys) throws JSONException {
        System.out.println("Incoming value is of JSONArray : =========");
        List<Object> array2List = new ArrayList<Object>();
        for (int i = 0; i < arrayOFKeys.length(); i++) {
            if (arrayOFKeys.opt(i) instanceof JSONObject) {
                Map<String, Object> subObj2Map = jsonString2Map(arrayOFKeys.opt(i).toString());
                array2List.add(subObj2Map);
            } else if (arrayOFKeys.opt(i) instanceof JSONArray) {
                List<Object> subarray2List = jsonArray2List((JSONArray) arrayOFKeys.opt(i));
                array2List.add(subarray2List);
            } else {
                array2List.add(arrayOFKeys.opt(i));
            }
        }
        return array2List;
    }

}
