package com.example.mapapplication;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class GoogleMapActivity extends AppCompatActivity {

    // ── Map surface ───────────────────────────────────────────────────────────
    private MapView osmMapSurface;

    // ── Networking ────────────────────────────────────────────────────────────
    private RequestQueue httpQueue;
    private static final String FETCH_PINS_ENDPOINT = "http://10.0.2.2/map_project/getPosition.php";

    // ── Default map center (shown before any pins load) ───────────────────────
    private static final double DEFAULT_LAT  = 37.272525;
    private static final double DEFAULT_LON  = -122.12106;
    private static final double DEFAULT_ZOOM = 15.0;

    // ── Marker display size in pixels ─────────────────────────────────────────
    private static final int PIN_SIZE_PX = 80;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid MUST be configured before setContentView touches the MapView.
        // This loads cached tile preferences from SharedPreferences.
        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osm_prefs", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_google_map);

        bootMapSurface();

        httpQueue = Volley.newRequestQueue(getApplicationContext());
        fetchAndPlotPins();
    }

    // ── Map setup ─────────────────────────────────────────────────────────────

    /**
     * Wires up the OSMDroid MapView:
     *   • MAPNIK tile layer  (standard OpenStreetMap rendering)
     *   • Built-in zoom buttons + pinch-to-zoom gestures
     *   • Default center + zoom level so the map isn't blank on first load
     */
    private void bootMapSurface() {
        osmMapSurface = findViewById(R.id.osm_map_surface);

        osmMapSurface.setTileSource(TileSourceFactory.MAPNIK);
        osmMapSurface.setBuiltInZoomControls(true);
        osmMapSurface.setMultiTouchControls(true);

        osmMapSurface.getController().setZoom(DEFAULT_ZOOM);
        osmMapSurface.getController().setCenter(new GeoPoint(DEFAULT_LAT, DEFAULT_LON));
    }

    // ── Data fetching ─────────────────────────────────────────────────────────

    /**
     * Hits the backend PHP endpoint, parses the returned JSON array of
     * {latitude, longitude} objects, and drops a scaled pin on each coordinate.
     *
     * JSON shape expected:
     * {
     *   "positions": [
     *     { "latitude": 37.27, "longitude": -122.12 },
     *     ...
     *   ]
     * }
     */
    private void fetchAndPlotPins() {
        JsonObjectRequest pinRequest = new JsonObjectRequest(
                Request.Method.POST,
                FETCH_PINS_ENDPOINT,
                null,                       // no JSON body — server reads query params
                this::handlePinResponse,    // success
                error -> error.printStackTrace()  // failure — log and move on
        );

        httpQueue.add(pinRequest);
    }

    /**
     * Parses the server response and renders one marker per position entry.
     */
    private void handlePinResponse(JSONObject serverData) {
        try {
            JSONArray pinArray = serverData.getJSONArray("positions");

            for (int idx = 0; idx < pinArray.length(); idx++) {
                JSONObject entry = pinArray.getJSONObject(idx);

                double pinLat = entry.getDouble("latitude");
                double pinLon = entry.getDouble("longitude");

                dropMarkerAt(pinLat, pinLon, idx + 1);

                // Quick coordinate readout — useful during development
                Toast.makeText(
                        getApplicationContext(),
                        "Pin #" + (idx + 1) + "  →  " + pinLat + ", " + pinLon,
                        Toast.LENGTH_SHORT
                ).show();
            }

            // Trigger a redraw so all newly added overlays appear immediately
            osmMapSurface.invalidate();

        } catch (JSONException parseError) {
            parseError.printStackTrace();
        }
    }

    // ── Marker factory ────────────────────────────────────────────────────────

    /**
     * Creates a single map pin at the given coordinates and attaches it
     * to the map's overlay list.
     *
     * The drawable is scaled to PIN_SIZE_PX × PIN_SIZE_PX so pins look
     * consistent regardless of the source image dimensions.
     * ANCHOR_CENTER / ANCHOR_BOTTOM aligns the tip of the pin with the
     * exact geographic point.
     *
     * @param lat   Latitude of the pin
     * @param lon   Longitude of the pin
     * @param index Human-readable index shown in the marker title
     */
    private void dropMarkerAt(double lat, double lon, int index) {
        Marker pin = new Marker(osmMapSurface);
        pin.setPosition(new GeoPoint(lat, lon));
        pin.setTitle("Location #" + index);

        // Built-in system location icon — no external drawable required
        Drawable scaledPin = getResources().getDrawable(
                android.R.drawable.ic_menu_mylocation
        );
        pin.setIcon(scaledPin);
        pin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        osmMapSurface.getOverlays().add(pin);
    }
}