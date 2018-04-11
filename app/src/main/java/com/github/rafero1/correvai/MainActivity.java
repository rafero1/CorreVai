package com.github.rafero1.correvai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;
    LocationListener locationListener;
    String locationProvider;
    Criteria criteria;
    Location lastKnownLocation;

    Location mPointA;
    Location mPointB;

    float mTotalDistance = 0;
    boolean mRunning;
    float mAvgSpeed;
    float mTime;

    TextView mTextView;
    Chronometer mChronometer;
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        mTextView = (TextView) findViewById(R.id.textView);
        mChronometer = (Chronometer) findViewById(R.id.chronometer2);
        mButton = (Button) findViewById(R.id.button);
        mRunning = false;


        criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null)
            locationProvider = locationManager.getBestProvider(criteria, true);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mPointA = mPointB;
                mPointB = location;
                mTotalDistance += mPointA.distanceTo(mPointB);
                Log.d("LCT", "Distância total: " + String.valueOf(mTotalDistance));
                mTextView.setText(String.format("%sm", String.valueOf((int) mTotalDistance)));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                locationProvider = locationManager.getBestProvider(criteria, true);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            }

            @Override
            public void onProviderEnabled(String s) {
                //
            }

            @Override
            public void onProviderDisabled(String s) {
                //
            }
        };

        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        mPointA = lastKnownLocation;
        mPointB = mPointA;
        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mRunning) {
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    mButton.setText(R.string.stop);
                } else {
                    mChronometer.stop();
                    mTotalDistance = 0;
                    mPointA = null;
                    mPointB = null;
                    mButton.setText(R.string.go);
                }
                mRunning = !mRunning;
            }
        });

        //TODO: Create timed logic for speed and distance calculations.

    }
}
