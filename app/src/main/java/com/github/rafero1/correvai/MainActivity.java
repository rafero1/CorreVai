package com.github.rafero1.correvai;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.kwabenaberko.openweathermaplib.Units;
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper;
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather;

public class MainActivity extends AppCompatActivity {
    //TODO: change to google play gps service
    // Permission array. Checks for all of these during onCreate.
    static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
    };

    LocationManager locationManager;
    String locationProvider;
    Criteria criteria;
    Location lastKnownLocation;
    OpenWeatherMapHelper weatherMapHelper;

    float mTotalDistance = 0;
    boolean mRunning = false;
    float mWalkedDistance = 0;
    float mAvgSpeed = 0;

    TextView mDistanceView;
    TextView mSpeedView;
    TextView mWeatherView;
    Chronometer mChronometer;
    Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize View objects
        mDistanceView = findViewById(R.id.distanceView);
        mSpeedView = findViewById(R.id.speedView);
        mWeatherView = findViewById(R.id.weatherView);
        mChronometer = findViewById(R.id.chronometer);
        mButton = findViewById(R.id.button);

        // Request permissions on start
        ActivityCompat.requestPermissions(this, PERMISSIONS, 10);

        // Setup location tracking service
        criteria = new Criteria();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            locationProvider = locationManager.getBestProvider(criteria, true);

            // Get last known location
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

            // Start requesting updates with locationProvider and set listener. minTime and minDistance choose minimum time/distance between updates
            locationManager.requestLocationUpdates(locationProvider, 0, 0, getLocationListener());
        }

        // Setup OpenWeatherMap helper class
        weatherMapHelper = new OpenWeatherMapHelper();
        weatherMapHelper.setApiKey(getString(R.string.OPEN_WEATHER_MAP_KEY));
        weatherMapHelper.setUnits(Units.METRIC);
        weatherMapHelper.getCurrentWeatherByGeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), new OpenWeatherMapHelper.CurrentWeatherCallback() {

            @Override
            public void onSuccess(CurrentWeather currentWeather) {
                mWeatherView.setText(currentWeather.getMain().getTemp() + " °C");
            }

            @Override
            public void onFailure(Throwable throwable) {
                mWeatherView.setText(R.string.connection_lost);
            }

        });

        // View listeners
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mRunning) {
                    resetView();
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

        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long secondsPassed = (SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
                if (secondsPassed != 0) {
                    float avgSpeedInMps = mWalkedDistance / secondsPassed;
                    mAvgSpeed = avgSpeedInMps * 3.6f;
                    mSpeedView.setText(String.format("%.2f km/h", mAvgSpeed));
                }
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /*if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            }
        }*/
    }

    public String getShortenedTotalDistance(float d) {
        String suffix = "m";
        float shortenedDistance = d;
        if (d >= 1000) {
            shortenedDistance = d / 1000;
            suffix = "km";
        }
        return String.format("%.2f%s", shortenedDistance, suffix);
    }


    private void resetView() {
        mTotalDistance = 0;
        mAvgSpeed = 0;
        mDistanceView.setText("0m");
        mSpeedView.setText("0 km/h");
    }


    // Listeners
    private LocationListener getLocationListener() {
        LocationListener l = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mRunning) {
                    mWalkedDistance = lastKnownLocation.distanceTo(location);
                    mTotalDistance += mWalkedDistance;
                    Log.d("LCT", "Distância total: " + String.valueOf(mTotalDistance));
                    mDistanceView.setText(getShortenedTotalDistance(mTotalDistance));
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                locationProvider = locationManager.getBestProvider(criteria, true);
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
        return l;
    }

    //TODO: private Chronometer.OnChronometerTickListener

    //TODO: private Button.OnClickListener

}
