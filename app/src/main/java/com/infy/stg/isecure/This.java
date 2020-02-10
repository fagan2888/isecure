package com.infy.stg.isecure;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.EventListener;
import java.util.Objects;

public class This {

    public static Context CONTEXT = null;

    public static SharedPreferences SHARED_PREFS;

    public static final class API_URL {

        public static String URL_BASE = "http://192.168.1.101:5000";
        public static String PATH_VERIFY = "/verify";
        public static String PATH_ENCODE = "/encode";
    }

    public static final class DIALOGS {
        public static void SETUP_BASE_URL_UPDATER(View button, Activity activity, FragmentActivity fragmentActivity, View view) {
            button.setOnLongClickListener(view1 -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(activity));
                View inflate = fragmentActivity.getLayoutInflater().inflate(R.layout.dialog_input_base_url, null);
                EditText ip_verify = inflate.findViewById(R.id.base_url);
                ip_verify.setText(This.API_URL.URL_BASE);
                builder.setView(inflate)
                        .setPositiveButton("Ok", (dialog, id) -> This.SHARED_PREFS.edit()
                                .putString("BASE_URL", This.API_URL.URL_BASE = ip_verify.getText()
                                        .toString()).apply())
                        .setNegativeButton("Cancel", null).create().show();
                return false;
            });
        }

        public static void SETUP_EMP_REG_INPUT(View button, Activity activity, FragmentActivity fragmentActivity, View view) {
            button.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(activity));
                View layout = fragmentActivity.getLayoutInflater().inflate(R.layout.dialog_input_employee_id, null);
                EditText et_emp_id = layout.findViewById(R.id.et_emp_id);
                ImageView iv_emp_face = layout.findViewById(R.id.iv_emp_face);
                iv_emp_face.setImageBitmap(APIClient.bitmap);
                builder.setView(layout)
                        .setPositiveButton("Register", (dialog, id) -> APIClient.encode(activity, view, et_emp_id.getText().toString()))
                        .setNegativeButton("Cancel", null).create().show();
            });
        }
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


    }
}
