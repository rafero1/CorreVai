package com.github.rafero1.correvai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;
    LocationListener locationListener;
    String locationProvider;
    Criteria criteria;
    Location lastKnownLocation;

    float mTotalDistance = 0;
    boolean mRunning = false;
    float mAvgSpeed;
    float mTime;

    TextView mDistanceView;
    Chronometer mChronometer;
    Button mButton;
    ImageButton mShareButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }

        mDistanceView = (TextView) findViewById(R.id.distanceView);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mButton = (Button) findViewById(R.id.button);
        mShareButton = (ImageButton) findViewById(R.id.shareButton);


        criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null)
            locationProvider = locationManager.getBestProvider(criteria, true);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mRunning) {
                    mTotalDistance += lastKnownLocation.distanceTo(location);
                    Log.d("LCT", "Distância total: " + String.valueOf(mTotalDistance));
                    mDistanceView.setText(getShortenedTotalDistance(mTotalDistance));
                }
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
        locationManager.requestLocationUpdates(locationProvider, 0, 0, locationListener);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mRunning) {
                    mTotalDistance = 0;
                    mDistanceView.setText("0m");
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    mButton.setText(R.string.stop);
                } else {
                    mChronometer.stop();
                    mButton.setText(R.string.go);
                }
                mRunning = !mRunning;
            }
        });

        //TODO: Speed calculation.

        // Code referring to the share button listener.
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendMessage = new Intent();
                sendMessage.setAction(Intent.ACTION_SEND);
                sendMessage.putExtra(Intent.EXTRA_TEXT, "Acabei de correr cerca de " +getShortenedTotalDistance(mTotalDistance)+ ". Quero ver alguem fazer mais!");
                sendMessage.setType("text/plain");
                startActivity(Intent.createChooser(sendMessage, getResources().getText(R.string.send_to)));
            }
        });

    }

    public String getShortenedTotalDistance(float d){
        String suffix = "m";
        float shortenedDistance = d;
        if (d >= 1000){
            shortenedDistance = d/1000;
            suffix = "km";
        }
        return String.format("%.2f%s", shortenedDistance, suffix);
    }
}
