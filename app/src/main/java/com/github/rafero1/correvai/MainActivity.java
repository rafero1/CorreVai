package com.github.rafero1.correvai;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.kwabenaberko.openweathermaplib.Units;
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper;
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather;

public class MainActivity extends AppCompatActivity {

    // Permission array. Checks for all of these.
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
    };
    public static final int PERMISSION_REQUEST_CODE = 10;

    private OpenWeatherMapHelper weatherMapHelper;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location mPreviousKnownLocation;
    private Location mLastKnownLocation;

    private float mTotalDistance = 0;
    private boolean mCountingDown = false;
    private boolean mRunning = false;
    private float mAvgSpeed = 0;

    private TextView mDistanceView;
    private TextView mSpeedView;
    private TextView mWeatherView;
    private Chronometer mChronometer;
    private Button mButton;
    private CountDownTimer mCountDownTimer;

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
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);

        // Setup location tracking service
        enableLocationServices();

        // Setup OpenWeatherMap helper class
        weatherMapHelper = new OpenWeatherMapHelper();
        weatherMapHelper.setApiKey(getString(R.string.OPEN_WEATHER_MAP_KEY));
        weatherMapHelper.setUnits(Units.METRIC);

        // Chronometer Listener
        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long elapsedSeconds = (SystemClock.elapsedRealtime() - chronometer.getBase()) / 1000;
                if (elapsedSeconds != 0) {/*
                    float avgSpeedInMps = 999999999 / elapsedSeconds;
                    mAvgSpeed = avgSpeedInMps * 3.6f;
                    mSpeedView.setText(String.format("%.2f km/h", mAvgSpeed));*/
                    mSpeedView.setText(String.format("%.2f km/h", mLastKnownLocation.getSpeed()* 3.6f));
                }


                if (elapsedSeconds == 0 || elapsedSeconds % 120 == 0)
                    if (mLastKnownLocation != null)
                        weatherMapHelper.getCurrentWeatherByGeoCoordinates(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude(), getWeatherCallbackListener());
            }
        });

        // Run button timer
        final int timeValue = 10000;
        mCountDownTimer = new CountDownTimer(timeValue, 1000) {
            public void onTick(long millisUntilFinished) {
                if (mCountingDown) {
                    mButton.setText(String.valueOf((millisUntilFinished) / 1000));
                }
            }

            public void onFinish() {
                if (mCountingDown) {
                    resetView();
                    mCountingDown = false;
                    mRunning = true;
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    mButton.setText(R.string.stop);
                }
            }
        };
    }

    private void enableLocationServices(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            mLocationRequest = createLocationRequest();

            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }

                    mLastKnownLocation = locationResult.getLastLocation();

                    if (mPreviousKnownLocation == null)
                        mPreviousKnownLocation = mLastKnownLocation;

                    if (mRunning) {
                        mTotalDistance += mPreviousKnownLocation.distanceTo(mLastKnownLocation);
                        Log.d("LCT", "Distância total: " + String.valueOf(mTotalDistance));
                        mDistanceView.setText(getShortenedTotalDistance(mTotalDistance));

                    }

                    mPreviousKnownLocation = mLastKnownLocation;
                    float speed = (mPreviousKnownLocation.distanceTo(mLastKnownLocation)) / 5;
                    mLastKnownLocation.setSpeed(speed);
                    Log.d("TAG","Speed: "+String.valueOf(locationResult.getLastLocation().getSpeed()));
                }
            };

            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

            mButton.setText(R.string.go);
            mButton.setOnClickListener(getOnButtonClickListener(this, true));

    } else {
            mButton.setText(R.string.location_disabled);
            mButton.setOnClickListener(getOnButtonClickListener(this, false));
        }
    }

    private View.OnClickListener getOnButtonClickListener(final Activity activity, boolean hasPermission){
        if (hasPermission){
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mRunning) {
                        mChronometer.stop();
                        mButton.setText(R.string.go);
                        mRunning = false;

                    } else {
                        if (mCountingDown) {
                            mCountDownTimer.cancel();
                            mButton.setText(R.string.go);
                            mCountingDown = false;

                        } else {
                            mCountDownTimer.start();
                            mCountingDown = true;

                        }
                    }
                }
            };
        } else {
            return new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_REQUEST_CODE);
                }
            };
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Starts location services if permission were granted
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationServices();
            }
        }
    }

    private LocationRequest createLocationRequest() {
        LocationRequest l = new LocationRequest();
        l.setInterval(5000);
        l.setFastestInterval(3000);
        l.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return l;
    }

    private OpenWeatherMapHelper.CurrentWeatherCallback getWeatherCallbackListener(){
        OpenWeatherMapHelper.CurrentWeatherCallback c = new OpenWeatherMapHelper.CurrentWeatherCallback() {
            @Override
            public void onSuccess(CurrentWeather currentWeather) {
                String temperature = getString(R.string.temperature);
                mWeatherView.setText(temperature+ ": " +currentWeather.getMain().getTemp()+ " °C");
            }

            @Override
            public void onFailure(Throwable throwable) {
                mWeatherView.setText(R.string.connection_lost);
            }

        };
        return c;
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

}
