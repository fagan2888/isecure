package com.infy.stg.isecure;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class CameraFragment extends Fragment {

    public static final String TAG = CameraFragment.class.getName();

    private OnFragmentInteractionListener mListener;
    private CameraUtil mCameraUtil;
    private String verifyIP;
    private String encodeIP;
    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;

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
        mCameraUtil = new CameraUtil(getActivity(), getView().findViewById(R.id.camera_view), getView().findViewById(R.id.overlay_view), getView());
        sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        APIClient.URL_VERIFY = sharedPref.getString("verify", "http://192.168.1.101:5000/id");
        APIClient.URL_ENCODE = sharedPref.getString("encode", "http://192.168.1.101:5000/id");

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.push_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "ON CLICK");
                mCameraUtil.clickPicture();
            }
        });
        view.findViewById(R.id.floatingActionButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "ON CLICK");
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // Get the layout inflater
                LayoutInflater inflater = requireActivity().getLayoutInflater();

                View inflate = inflater.inflate(R.layout.dialog_input_id, null);

                final EditText emp_id = inflate.findViewById(R.id.emp_id);

                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                builder.setView(inflate)
                        // Add action buttons
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                APIClient.encode(getActivity(), getView(), emp_id.getText().toString());
                            }
                        })
                        .setNegativeButton("Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        view.findViewById(R.id.push_button).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.d(TAG, "ON LONG CLICK");
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // Get the layout inflater
                LayoutInflater inflater = requireActivity().getLayoutInflater();

                View inflate = inflater.inflate(R.layout.dialog_input_ip, null);

                final EditText ip_verify = inflate.findViewById(R.id.ip_verify);
                final EditText ip_encode = inflate.findViewById(R.id.ip_encode);

                ip_verify.setText(sharedPref.getString("verify", "http://192.168.1.101:5000/verify"));
                ip_encode.setText(sharedPref.getString("encode", "http://192.168.1.101:5000/encode"));

                // Inflate and set the layout for the dialog
                // Pass null as the parent view because its going in the dialog layout
                builder.setView(inflate)
                        // Add action buttons
                        .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {

                                editor.putString("verify", ip_verify.getText().toString());
                                editor.putString("encode", ip_encode.getText().toString());
                                editor.commit();
                                APIClient.URL_VERIFY = sharedPref.getString("verify", "http://192.168.1.101:5000/verify");
                                APIClient.URL_ENCODE = sharedPref.getString("encode", "http://192.168.1.101:5000/encode");
                            }
                        })
                        .setNegativeButton("Cancel", null);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return false;
            }
        });
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
