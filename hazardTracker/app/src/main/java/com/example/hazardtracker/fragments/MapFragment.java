package com.example.hazardtracker.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hazardtracker.MainActivity;
import com.example.hazardtracker.R;
import com.example.hazardtracker.models.Hazard;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.Priority;

public class MapFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private MapEventsOverlay mapEventsOverlay;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private ListenerRegistration hazardsListener;

    private Marker selectedMarker;
    private org.osmdroid.util.GeoPoint selectedPoint;

    private LinearLayout locationSelectionPanel;
    private TextView tvSelectedLocation;
    private Button btnReportHere;
    private Button btnCancelSelection;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private static final double DEFAULT_LAT = 3.1390;
    private static final double DEFAULT_LNG = 101.6869;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Configuration.getInstance().load(
                requireContext(),
                PreferenceManager.getDefaultSharedPreferences(
                        requireContext()
                )
        );

        View view = inflater.inflate(
                R.layout.fragment_map,
                container,
                false
        );

        mapView = view.findViewById(R.id.mapView);

        locationSelectionPanel =
                view.findViewById(R.id.locationSelectionPanel);

        tvSelectedLocation =
                view.findViewById(R.id.tvSelectedLocation);

        btnReportHere =
                view.findViewById(R.id.btnReportHere);

        btnCancelSelection =
                view.findViewById(R.id.btnCancelSelection);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setMinZoomLevel(3.0);
        mapView.setMaxZoomLevel(20.0);

        mapView.getController().setZoom(15.0);

        mapView.getController().setCenter(
                new org.osmdroid.util.GeoPoint(
                        DEFAULT_LAT,
                        DEFAULT_LNG
                )
        );

        Button btnZoomIn =
                view.findViewById(R.id.btnZoomIn);

        Button btnZoomOut =
                view.findViewById(R.id.btnZoomOut);

        btnZoomIn.setOnClickListener(
                v -> mapView.getController().zoomIn()
        );

        btnZoomOut.setOnClickListener(
                v -> mapView.getController().zoomOut()
        );

        setupMapSelection();

        btnReportHere.setOnClickListener(v -> {
            if (selectedPoint == null) {
                return;
            }

            MainActivity activity =
                    (MainActivity) requireActivity();

            activity.openReportPage(
                    selectedPoint.getLatitude(),
                    selectedPoint.getLongitude()
            );
        });

        btnCancelSelection.setOnClickListener(
                v -> clearSelectedLocation()
        );

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(
                        requireActivity()
                );

        setupMyLocationOverlay();
        checkLocationPermission();

        db = FirebaseFirestore.getInstance();
        loadHazardMarkers();

        return view;
    }

    private void setupMapSelection() {
        mapEventsOverlay = new MapEventsOverlay(
                new MapEventsReceiver() {

                    @Override
                    public boolean singleTapConfirmedHelper(
                            org.osmdroid.util.GeoPoint point
                    ) {
                        InfoWindow.closeAllInfoWindowsOn(mapView);
                        return true;
                    }

                    @Override
                    public boolean longPressHelper(
                            org.osmdroid.util.GeoPoint point
                    ) {
                        selectHazardLocation(point);
                        return true;
                    }
                }
        );

        mapView.getOverlays().add(mapEventsOverlay);
    }

    private void selectHazardLocation(
            org.osmdroid.util.GeoPoint point
    ) {
        selectedPoint = point;

        if (selectedMarker == null) {
            selectedMarker = new Marker(mapView);

            selectedMarker.setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_BOTTOM
            );

            selectedMarker.setTitle(
                    "Selected Hazard Location"
            );

            mapView.getOverlays().add(selectedMarker);
        }

        selectedMarker.setPosition(point);

        selectedMarker.setSnippet(
                point.getLatitude()
                        + ", "
                        + point.getLongitude()
        );

        tvSelectedLocation.setText(
                String.format(
                        "Selected: %.6f, %.6f",
                        point.getLatitude(),
                        point.getLongitude()
                )
        );

        locationSelectionPanel.setVisibility(View.VISIBLE);

        mapView.invalidate();
    }

    private void clearSelectedLocation() {
        selectedPoint = null;

        if (selectedMarker != null) {
            mapView.getOverlays().remove(selectedMarker);
            selectedMarker = null;
        }

        locationSelectionPanel.setVisibility(View.GONE);

        mapView.invalidate();
    }

    private void loadHazardMarkers() {
        hazardsListener = db.collection("hazards")
                .whereEqualTo("status", "Active")
                .addSnapshotListener(
                        (
                                QuerySnapshot snapshots,
                                FirebaseFirestoreException error
                        ) -> {
                            if (error != null) {
                                Toast.makeText(
                                        getContext(),
                                        "Failed to load hazards: "
                                                + error.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (
                                    snapshots == null ||
                                            mapView == null
                            ) {
                                return;
                            }

                            mapView.getOverlays().clear();

                            mapView.getOverlays().add(
                                    mapEventsOverlay
                            );

                            mapView.getOverlays().add(
                                    myLocationOverlay
                            );

                            if (selectedMarker != null) {
                                mapView.getOverlays().add(
                                        selectedMarker
                                );
                            }

                            for (
                                    DocumentSnapshot doc :
                                    snapshots.getDocuments()
                            ) {
                                Hazard hazard =
                                        doc.toObject(
                                                Hazard.class
                                        );

                                if (
                                        hazard != null &&
                                                hazard.getLocation() != null
                                ) {
                                    addHazardMarker(hazard);
                                }
                            }

                            mapView.invalidate();
                        }
                );
    }

    private void addHazardMarker(Hazard hazard) {
        GeoPoint firestoreLocation =
                hazard.getLocation();

        org.osmdroid.util.GeoPoint point =
                new org.osmdroid.util.GeoPoint(
                        firestoreLocation.getLatitude(),
                        firestoreLocation.getLongitude()
                );

        Marker marker = new Marker(mapView);

        marker.setPosition(point);

        marker.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
        );

        marker.setTitle(hazard.getCategory());

        marker.setSnippet(
                hazard.getDescription()
                        + "\n"
                        + hazard.getLocationName()
        );

        if (hazard.getCategory() != null) {
            switch (hazard.getCategory()) {
                case "Road Hazard":
                    marker.setIcon(
                            ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_marker_road
                            )
                    );
                    break;

                case "Environmental Hazard":
                    marker.setIcon(
                            ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_marker_environmental
                            )
                    );
                    break;

                case "Building Hazard":
                    marker.setIcon(
                            ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_marker_building
                            )
                    );
                    break;

                default:
                    marker.setIcon(
                            ContextCompat.getDrawable(
                                    requireContext(),
                                    R.drawable.ic_marker_road
                            )
                    );
                    break;
            }
        }

        mapView.getOverlays().add(marker);
    }

    private void setupMyLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()),
                mapView
        );

        mapView.getOverlays().add(myLocationOverlay);
    }

    private void checkLocationPermission() {
        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            enableCurrentLocation();
        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void enableCurrentLocation() {
        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
        }

        centerOnUserLocation();
    }

    private void centerOnUserLocation() {
        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {
            return;
        }

        CurrentLocationRequest request =
                new CurrentLocationRequest.Builder()
                        .setPriority(
                                Priority.PRIORITY_HIGH_ACCURACY
                        )
                        .build();

        fusedLocationClient
                .getCurrentLocation(request, null)
                .addOnSuccessListener(location -> {
                    if (location != null && mapView != null) {
                        org.osmdroid.util.GeoPoint userPoint =
                                new org.osmdroid.util.GeoPoint(
                                        location.getLatitude(),
                                        location.getLongitude()
                                );

                        mapView.getController().animateTo(userPoint);
                        mapView.getController().setZoom(17.0);

                        if (myLocationOverlay != null) {
                            myLocationOverlay.enableMyLocation();
                        }

                        mapView.invalidate();
                    } else {
                        Toast.makeText(
                                requireContext(),
                                "Current location unavailable. Turn on GPS or set emulator location.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                })
                .addOnFailureListener(error ->
                        Toast.makeText(
                                requireContext(),
                                "Location error: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = false;

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                enableCurrentLocation();
            } else {
                Toast.makeText(
                        requireContext(),
                        "Location permission is required.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mapView != null) {
            mapView.onResume();
        }

        boolean fineGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (myLocationOverlay != null
                && (fineGranted || coarseGranted)) {
            myLocationOverlay.enableMyLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mapView != null) {
            mapView.onPause();
        }

        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (hazardsListener != null) {
            hazardsListener.remove();
            hazardsListener = null;
        }

        mapView = null;
    }
}