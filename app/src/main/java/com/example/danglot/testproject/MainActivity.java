package com.example.danglot.testproject;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.UUID;


public class MainActivity  extends Activity implements
        LocationListener {

    private static final String url = "http://172.18.13.123:8080/";
    private static final String TAG = "ERROR_LOCATION_SECURITY";
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 23;
    private static final int NOTIFICATION_ID = 32;
    private static final int time = 1000;

    private LocationManager lm;
    private boolean hasBeenNotify = false;
    private double latitude;
    private double longitude;
    private Handler handlerCheckDanger;
    private Handler handlerHelpPosition;
    private UUID id;

    private void checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        int hasGpsPermissionFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        int hasGpsPermissionCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasGpsPermissionFine != PackageManager.PERMISSION_GRANTED && hasGpsPermissionCoarse != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        id = new UUID(androidId.hashCode(), ( (long) tmDevice.hashCode() << 32 ) | tmSerial.hashCode() );

        setContentView(R.layout.activity_main);

        hasBeenNotify = false;

        handlerCheckDanger = new Handler();
        dangerChecker.run();

        handlerHelpPosition = new Handler();

        ToggleButton toggle = (ToggleButton) findViewById(R.id.button);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    checkPermission();
                    try {
                        helpPosition.run();
                        //Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        sendHttpRequestLocation(longitude, latitude);
                    } catch (SecurityException e) {
                        Log.d(TAG, "");
                        e.printStackTrace();
                    }
                } else {
                    handlerHelpPosition.removeCallbacks(helpPosition);
                }
            }
        });
    }

    Runnable helpPosition = new Runnable() {
        @Override
        public void run() {
                sendHttpRequestLocation(longitude, latitude);
                handlerHelpPosition.postDelayed(helpPosition, time);
        }
    };

    Runnable dangerChecker = new Runnable() {
        @Override
        public void run() {
            if (!hasBeenNotify) {
                sendHttpRequestCheckDanger(longitude, latitude);
                handlerCheckDanger.postDelayed(dangerChecker, time);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        lm = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        checkPermission();
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0,
                    this);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 0,
                this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkPermission();
        lm.removeUpdates(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        String msg = String.format("Longitude : %1$2s ; Latitude : %2$2s",latitude,longitude);
        final TextView t = (TextView)findViewById(R.id.TextView);
        t.setText(msg);
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void sendHttpRequestLocation(final double longitude, final double latitude) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url+"location?longitude="+longitude+"&latitude="+latitude+"&id="+id.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                final TextView t = (TextView) findViewById(R.id.TextViewErr);
                t.setText("That didn't work : " + error.toString());
            }
        });
        String msg = String.format("(long:%1$2s ; lat:%2$2s)", longitude, latitude);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        queue.add(stringRequest);
    }

    public void sendHttpRequestCheckDanger(final double longitude, final double latitude) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url+"check?longitude="+longitude+"&latitude="+latitude+"&id="+id.toString(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        System.out.println(response);
                        if (response.equals("0"))
                            sendNotification();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                final TextView t = (TextView) findViewById(R.id.TextViewErr);
                t.setText("That didn't work : " + error.toString());
            }
        });
        queue.add(stringRequest);
    }

    private void sendNotification() {
        hasBeenNotify = true;
        Context applicationContext = getApplicationContext();
        Notification.Builder b = new Notification.Builder(applicationContext);
        b.setContentTitle("Alerte");
        b.setContentText("Vous êtes actuellement à proximité d'une zone à risque!");
        b.setSmallIcon(R.drawable.warning);
        b.setVibrate(new long[]{1000, 1000, 1000, 0, 1000, 0, 0, 1000, 1000, 1000, 0, 1000});
        b.setLights(Color.RED, 3000, 3000);
        Intent notIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder t = TaskStackBuilder.create(applicationContext);
        t.addParentStack(MainActivity.class);
        t.addNextIntent(notIntent);
        PendingIntent p = t.getPendingIntent(NOTIFICATION_ID,PendingIntent.FLAG_UPDATE_CURRENT);
        b.setContentIntent(p);
        String notifService = Context.NOTIFICATION_SERVICE;
        NotificationManager mgr =
                (NotificationManager) getSystemService(notifService);
        mgr.notify(NOTIFICATION_ID, b.build());
    }
}