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

    Location mStartLocation;
    Location mPointA;
    Location mPointB;
    Location mEndLocation;

    float mTotalDistance = 0;

    float mAvgSpeed;
    float mTime;

    TextView mTextView;
    boolean mRunning = false;
    Chronometer mChronometer;
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mPointA = mPointB;
                mPointB = location;
                mTotalDistance += mPointA.distanceTo(mPointB);
                Log.d("LCT", "Distância total: "+String.valueOf(mTotalDistance));
                mTextView.setText("Distância total: "+String.valueOf(mTotalDistance));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                //
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
        Criteria criteria = new Criteria();

        String locationProvider = LocationManager.GPS_PROVIDER;
        locationProvider = locationManager.getBestProvider(criteria, true);
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);

        mStartLocation = lastKnownLocation;
        mPointA = mStartLocation;
        mPointB = mPointA;

        mTextView = (TextView) findViewById(R.id.textView);
        mChronometer = (Chronometer) findViewById(R.id.chronometer2);
        mButton = (Button) findViewById(R.id.button);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mRunning) {
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    mButton.setText("Parar");
                } else {
                    mChronometer.stop();
                    mTotalDistance = 0;
                    mButton.setText("Correr");
                }
                mRunning = !mRunning;
            }
        });

        //TODO: Create timed logic for speed and distance calculations.

    }
}
