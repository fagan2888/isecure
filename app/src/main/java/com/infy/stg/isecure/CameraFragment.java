package com.infy.stg.isecure;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import okhttp3.Response;

public class CameraFragment extends Fragment {

    public static final String TAG = CameraFragment.class.getName();

    private OnFragmentInteractionListener mListener;
    private CameraUtil mCameraUtil;
    private TextView tv_emp_id;
    private TextView tv_emp_name;
    private FloatingActionButton btn_register;
    private ImageView iv_emp_face;
    private View btn_verify;


    public CameraFragment() {
    }

    public static Fragment newInstance() {
        return new CameraFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCameraUtil = new CameraUtil(getActivity(), Objects.requireNonNull(getView()).findViewById(R.id.camera_view), getView().findViewById(R.id.overlay_view),
                new CameraUtil.FaceDetectionCallback() {
                    @Override
                    public void onFace(boolean detected) {
                        if (detected)
                            getView().findViewById(R.id.btn_verify).setBackground(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.button_bg_round_enabled));
                        else
                            getView().findViewById(R.id.btn_verify).setBackground(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.button_bg_round_disabled));
                    }
                }, new CameraUtil.FaceCaptureCallback() {
            @Override
            public void onFaceCaptured(byte[] bytes, int orientation) {
                onPostFaceCapture(bytes, orientation);
            }
        }, new CameraUtil.OverlayResizeCallback() {
            @Override
            public void onOverlayResized(int width, int height) {
                onPostOverlayResized(height);
            }
        });
    }

    private void onPostFaceCapture(byte[] bytes, int orientation) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
        bitmap = This.UTIL.getResizedBitmap(bitmap, 1024);
        bitmap = This.UTIL.rotateBitmap(bitmap, orientation);
        Bitmap bmp = bitmap;
        APIClient.verify(bitmap, new APIClient.APICallback() {
            @Override
            public void onResponse(Response response) {
                if (response != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Map<String, Object> map = This.UTIL.jsonString2Map(response.body().string());
                                iv_emp_face.setImageBitmap(bmp);
                                String identity = (String) map.get("identity");
                                if (identity.contains("not-found")) {
                                    btn_register.setVisibility(View.VISIBLE);
                                    tv_emp_id.setText("-");
                                    tv_emp_name.setText("-");
                                } else {
                                    btn_register.setVisibility(View.INVISIBLE);
                                    tv_emp_id.setText("Employee #");
                                    tv_emp_name.setText(identity);
                                }
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    private void onPostOverlayResized(int height) {
        View view = getView().findViewById(R.id.card);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = height + This.UTIL.getActionBarHeight() * 9 / 10;
        view.setLayoutParams(params);
        view.invalidate();
        view.requestLayout();
        view.animate().alpha(1).setDuration(2000);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btn_register = getView().findViewById(R.id.btn_register);
        tv_emp_id = getView().findViewById(R.id.tv_emp_id);
        tv_emp_name = ((TextView) getView().findViewById(R.id.tv_emp_name));
        iv_emp_face = ((ImageView) getView().findViewById(R.id.iv_emp_face));
        btn_verify = view.findViewById(R.id.btn_verify);

        view.findViewById(R.id.btn_verify).setOnClickListener(v -> mCameraUtil.clickPicture());

        setupSettingsAction();
        setuoRegistrationAction();
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCameraUtil.resume();
    }

    public void onPause() {
        super.onPause();
        mCameraUtil.pause();
    }

    public void setupSettingsAction() {
        btn_verify.setOnLongClickListener(view1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
            View inflate = requireActivity().getLayoutInflater().inflate(R.layout.dialog_input_base_url, null);
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

    public void setuoRegistrationAction() {
        btn_register.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
            View layout = requireActivity().getLayoutInflater().inflate(R.layout.dialog_input_employee_id, null);
            EditText et_emp_id = layout.findViewById(R.id.et_emp_id);
            ImageView iv_emp_face = layout.findViewById(R.id.iv_emp_reg);
            iv_emp_face.setImageBitmap(This.WORKING_IMAGE);
            builder.setView(layout).setPositiveButton("Register", (dialog, id) -> APIClient.encode(et_emp_id.getText().toString().trim(), new APIClient.APICallback() {
                @Override
                public void onResponse(Response response) {
                    if (response != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Map<String, Object> map = This.UTIL.jsonString2Map(response.body().string());
                                    iv_emp_face.setImageBitmap(This.WORKING_IMAGE);
                                    String identity = (String) map.get("identity");
                                    if (!identity.contains("saved")) {
                                        getView().findViewById(R.id.btn_register).setVisibility(View.VISIBLE);
                                        tv_emp_id.setText("-");
                                        tv_emp_name.setText("-");
                                    } else {
                                        btn_register.setVisibility(View.INVISIBLE);
                                        tv_emp_id.setText("Employee #");
                                        tv_emp_name.setText(identity);
                                    }
                                } catch (JSONException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            }))
                    .setNegativeButton("Cancel", null).create().show();
        });
    }


}
