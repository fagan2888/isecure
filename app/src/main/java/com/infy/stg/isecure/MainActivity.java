package com.infy.stg.isecure;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements CameraFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(this.getSupportActionBar()).hide();


        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, CameraFragment.newInstance())
                    .commit();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
