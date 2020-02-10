package com.infy.stg.isecure;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.Objects;

public class CameraFragment extends Fragment {

    public static final String TAG = CameraFragment.class.getName();

    private OnFragmentInteractionListener mListener;
    private CameraUtil mCameraUtil;


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
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
                bitmap = This.UTIL.getResizedBitmap(bitmap, 1024);
                bitmap = This.UTIL.rotateBitmap(bitmap, orientation);
                APIClient.verify(getActivity(), getView(), bitmap);

            }
        }, new CameraUtil.OverlayResizeCallback() {
            @Override
            public void onOverlayResized(int width, int height) {
                View view = getView().findViewById(R.id.card);
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.height = height + This.UTIL.getActionBarHeight() * 9 / 10;
                view.setLayoutParams(params);
                view.invalidate();
                view.requestLayout();
                view.animate().alpha(1).setDuration(2000);
            }
        });


    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_verify).setOnClickListener(v -> mCameraUtil.clickPicture());

        This.DIALOGS.SETUP_BASE_URL_UPDATER(view.findViewById(R.id.btn_verify), getActivity(), requireActivity(), view);
        This.DIALOGS.SETUP_EMP_REG_INPUT(view.findViewById(R.id.btn_register), getActivity(), requireActivity(), view);
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


}
