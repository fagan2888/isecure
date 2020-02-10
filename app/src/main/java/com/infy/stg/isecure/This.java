package com.infy.stg.isecure;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

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
import java.util.Objects;

import okhttp3.Response;

public class This {

    public static Context CONTEXT = null;

    public static SharedPreferences SHARED_PREFS;
    public static Bitmap WORKING_IMAGE;

    public static final class API_URL {

        public static String URL_BASE = "http://192.168.1.101:5000";
        public static String PATH_VERIFY = "/verify";
        public static String PATH_ENCODE = "/encode";
    }

    public static final class DIALOGS {



    }

    public static final class UTIL {
        public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

            Matrix matrix = new Matrix();
            matrix.setRotate(orientation);
            try {
                Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//            bitmap.recycle();
                return bmRotated;
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                return null;
            }
        }

        public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
            int width = image.getWidth();
            int height = image.getHeight();  // image.getHeight()

            float bitmapRatio = (float) width / (float) height;
            if (bitmapRatio > 1) {
                width = maxSize;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxSize;
                width = (int) (height * bitmapRatio);
            }
            return Bitmap.createScaledBitmap(image, width, height, true);
        }

        public static int getActionBarHeight() {
            final TypedArray ta = CONTEXT.getTheme().obtainStyledAttributes(
                    new int[]{android.R.attr.actionBarSize});
            int actionBarHeight = (int) ta.getDimensionPixelOffset(0, 0) + (int) ta.getDimensionPixelSize(0, 0);
            return actionBarHeight;
        }

        public static String convertImageToString(Bitmap imageBitmap) {
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
}
