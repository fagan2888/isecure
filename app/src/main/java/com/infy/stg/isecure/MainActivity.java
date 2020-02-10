package com.infy.stg.isecure;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements CameraFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(this.getSupportActionBar()).hide();

        This.SHARED_PREFS = getPreferences(Context.MODE_PRIVATE);
        This.CONTEXT = getApplicationContext();

        This.API_URL.URL_BASE = This.SHARED_PREFS.getString("BASE_URL", This.API_URL.URL_BASE);

        if (null == savedInstanceState)
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, CameraFragment.newInstance()).commit();

    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d(TAG, uri.toString());
    }
}
