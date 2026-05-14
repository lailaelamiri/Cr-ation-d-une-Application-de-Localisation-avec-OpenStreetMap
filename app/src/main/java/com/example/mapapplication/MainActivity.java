package com.example.mapapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ── UI ────────────────────────────────────────────────────────────────────
    private Button mapLaunchBtn;

    // ── Live GPS coordinates (updated on every location fix) ──────────────────
    private double currentLat  = 0.0;
    private double currentLon  = 0.0;
    private double currentAlt  = 0.0;
    private float  fixAccuracy = 0f;

    // ── Networking ────────────────────────────────────────────────────────────
    private RequestQueue httpQueue;
    // 10.0.2.2 resolves to localhost on the host machine from inside the emulator
    private static final String BACKEND_ENDPOINT = "http://10.0.2.2/map_project/createPosition.php";

    // ── Location ──────────────────────────────────────────────────────────────
    private LocationManager gpsManager;
    private static final long   GPS_INTERVAL_MS  = 60_000L; // refresh every 60 s
    private static final float  GPS_MIN_DISTANCE = 150f;    // only fire if moved 150 m+

    // ── Permissions ───────────────────────────────────────────────────────────
    private static final int PERM_REQUEST_ID = 42;
    private static final String[] NEEDED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
    };

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Boot the Volley queue — reused for all outbound HTTP calls
        httpQueue  = Volley.newRequestQueue(getApplicationContext());
        gpsManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Wire up the launch button (ID matches our custom layout)
        mapLaunchBtn = findViewById(R.id.btn_launch_map_view);
        mapLaunchBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GoogleMapActivity.class))
        );

        // Gate: ask for permissions if any are missing, otherwise go straight to tracking
        if (permissionsAlreadyGranted()) {
            beginTracking();
        } else {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, PERM_REQUEST_ID);
        }
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    /** Returns true only when every required permission has been granted. */
    private boolean permissionsAlreadyGranted() {
        for (String perm : NEEDED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERM_REQUEST_ID) return;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            beginTracking();
        } else {
            Toast.makeText(this,
                    "Location permission denied — tracking unavailable.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ── Location tracking ─────────────────────────────────────────────────────

    /**
     * Registers a GPS listener with the system location manager.
     * Fires at most once per minute OR when the device moves ≥150 m.
     */
    private void beginTracking() {
        // Double-check at runtime — Android lint requires this guard before requestLocationUpdates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        gpsManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                GPS_INTERVAL_MS,
                GPS_MIN_DISTANCE,
                coordinateWatcher
        );
    }

    /**
     * Receives each new GPS fix, stores the coordinates,
     * pushes them to the backend, and notifies the user via Toast.
     */
    private final LocationListener coordinateWatcher = new LocationListener() {

        @Override
        public void onLocationChanged(@NonNull Location fix) {
            currentLat  = fix.getLatitude();
            currentLon  = fix.getLongitude();
            currentAlt  = fix.getAltitude();
            fixAccuracy = fix.getAccuracy();

            // Build a human-readable summary of the new fix
            String summary = String.format(
                    getResources().getString(R.string.new_location),
                    currentLat, currentLon, currentAlt, fixAccuracy
            );

            pushCoordinatesToServer(currentLat, currentLon);
            Toast.makeText(getApplicationContext(), summary, Toast.LENGTH_LONG).show();
        }

        // Kept for API < 29 compatibility; no-op on newer devices
        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled(@NonNull String provider) {}
        @Override public void onProviderDisabled(@NonNull String provider) {}
    };

    // ── Network ───────────────────────────────────────────────────────────────

    /**
     * Fires a POST request to the backend with the latest coordinates,
     * a formatted timestamp, and a privacy-safe device identifier.
     *
     * ANDROID_ID is used instead of IMEI — it requires no permission,
     * persists across reboots, and resets only on factory reset.
     */
    private void pushCoordinatesToServer(final double lat, final double lon) {

        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                BACKEND_ENDPOINT,
                response -> { /* server acknowledged — nothing extra needed */ },
                error   -> { /* silent fail — could log or retry here */ }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new Date());

                // Privacy-safe device ID (no READ_PHONE_STATE needed at runtime)
                String deviceId = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );

                Map<String, String> payload = new HashMap<>();
                payload.put("latitude",  String.valueOf(lat));
                payload.put("longitude", String.valueOf(lon));
                payload.put("date",      timestamp);
                payload.put("imei",      deviceId); // key kept as 'imei' to match backend schema
                return payload;
            }
        };

        httpQueue.add(postRequest);
    }
}