package com.example.hazardtracker.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.hazardtracker.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.example.hazardtracker.MainActivity;

import java.util.HashMap;
import java.util.Map;

public class ReportFragment extends Fragment {

    private EditText etUserName, etDescription, etLocationName, etLatitude, etLongitude;
    private RadioGroup rgCategory;
    private TextView tvCoordinates;
    private Button btnSubmit;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 200;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        etUserName = view.findViewById(R.id.etUserName);
        etDescription = view.findViewById(R.id.etDescription);
        etLocationName = view.findViewById(R.id.etLocationName);
        etLatitude = view.findViewById(R.id.etLatitude);
        etLongitude = view.findViewById(R.id.etLongitude);
        rgCategory = view.findViewById(R.id.rgCategory);
        tvCoordinates = view.findViewById(R.id.tvCoordinates);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        Bundle arguments = getArguments();

        if (
                arguments != null &&
                        arguments.containsKey("selectedLatitude") &&
                        arguments.containsKey("selectedLongitude")
        ) {
            double selectedLatitude =
                    arguments.getDouble("selectedLatitude");

            double selectedLongitude =
                    arguments.getDouble("selectedLongitude");

            etLatitude.setText(
                    String.valueOf(selectedLatitude)
            );

            etLongitude.setText(
                    String.valueOf(selectedLongitude)
            );

            tvCoordinates.setText(
                    "Location selected from the map."
            );
        }



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        db = FirebaseFirestore.getInstance();

        if (
                getArguments() == null ||
                        !getArguments().containsKey("selectedLatitude")
        ) {
            fetchCurrentLocation();
        }

        btnSubmit.setOnClickListener(v -> submitReport());

        return view;
    }

    private void fetchCurrentLocation() {
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
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );

            return;
        }

        tvCoordinates.setText(
                "Getting current location..."
        );

        CurrentLocationRequest request =
                new CurrentLocationRequest.Builder()
                        .setPriority(
                                Priority.PRIORITY_HIGH_ACCURACY
                        )
                        .build();

        fusedLocationClient
                .getCurrentLocation(request, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (location != null) {
                        etLatitude.setText(
                                String.valueOf(
                                        location.getLatitude()
                                )
                        );

                        etLongitude.setText(
                                String.valueOf(
                                        location.getLongitude()
                                )
                        );

                        tvCoordinates.setText(
                                "Current location detected successfully."
                        );
                    } else {
                        tvCoordinates.setText(
                                "Unable to detect location. Turn on GPS or select a location from the map."
                        );
                    }
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }

                    tvCoordinates.setText(
                            "Location error. Select a location from the map."
                    );
                });
    }

    private void submitReport() {
        clearPreviousErrors();

        String userName =
                etUserName.getText().toString().trim();

        String description =
                etDescription.getText().toString().trim();

        String locationName =
                etLocationName.getText().toString().trim();

        String latitudeText =
                etLatitude.getText().toString().trim();

        String longitudeText =
                etLongitude.getText().toString().trim();

        if (TextUtils.isEmpty(userName)) {
            etUserName.setError("Please enter your name");
            etUserName.requestFocus();
            return;
        }

        if (rgCategory.getCheckedRadioButtonId() == -1) {
            Toast.makeText(
                    requireContext(),
                    "Please select a hazard category",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            etDescription.setError("Please describe the hazard");
            etDescription.requestFocus();
            return;
        }

        if (description.length() < 5) {
            etDescription.setError(
                    "Description must contain at least 5 characters"
            );
            etDescription.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(locationName)) {
            etLocationName.setError(
                    "Please enter a location name"
            );
            etLocationName.requestFocus();
            return;
        }

        if (
                TextUtils.isEmpty(latitudeText) ||
                        TextUtils.isEmpty(longitudeText)
        ) {
            Toast.makeText(
                    requireContext(),
                    "Please select a location from the map or enable GPS",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        double latitude;
        double longitude;

        try {
            latitude = Double.parseDouble(latitudeText);
            longitude = Double.parseDouble(longitudeText);
        } catch (NumberFormatException error) {
            Toast.makeText(
                    requireContext(),
                    "Latitude and longitude must be valid numbers",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (latitude < -90 || latitude > 90) {
            etLatitude.setError(
                    "Latitude must be between -90 and 90"
            );
            etLatitude.requestFocus();
            return;
        }

        if (longitude < -180 || longitude > 180) {
            etLongitude.setError(
                    "Longitude must be between -180 and 180"
            );
            etLongitude.requestFocus();
            return;
        }

        String category = getSelectedCategory();

        Map<String, Object> hazard = new HashMap<>();

        hazard.put("userName", userName);
        hazard.put("category", category);
        hazard.put("description", description);

        hazard.put(
                "deviceModel",
                Build.MANUFACTURER + " " + Build.MODEL
        );

        hazard.put(
                "location",
                new GeoPoint(latitude, longitude)
        );

        hazard.put("locationName", locationName);

        // Trusted Firebase server time
        hazard.put(
                "reportedAt",
                FieldValue.serverTimestamp()
        );

        hazard.put("status", "Active");

        setSubmittingState(true);

        db.collection("hazards")
                .add(hazard)
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(
                            requireContext(),
                            "Hazard reported successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    clearForm();

                    MainActivity activity =
                            (MainActivity) requireActivity();

                    activity.clearSelectedLocation();
                    activity.openMapPage();
                })
                .addOnFailureListener(error -> {
                    if (!isAdded()) {
                        return;
                    }

                    String message = error.getMessage();

                    if (message == null || message.trim().isEmpty()) {
                        message = "Unknown Firebase error";
                    }

                    Toast.makeText(
                            requireContext(),
                            "Failed to submit report: " + message,
                            Toast.LENGTH_LONG
                    ).show();
                })
                .addOnCompleteListener(task -> {
                    if (isAdded()) {
                        setSubmittingState(false);
                    }
                });
    }

    private void clearPreviousErrors() {
        etUserName.setError(null);
        etDescription.setError(null);
        etLocationName.setError(null);
        etLatitude.setError(null);
        etLongitude.setError(null);
    }

    private void setSubmittingState(boolean submitting) {
        btnSubmit.setEnabled(!submitting);

        btnSubmit.setText(
                submitting
                        ? "Submitting..."
                        : "Submit Report"
        );

        etUserName.setEnabled(!submitting);
        etDescription.setEnabled(!submitting);
        etLocationName.setEnabled(!submitting);
        etLatitude.setEnabled(!submitting);
        etLongitude.setEnabled(!submitting);

        for (int index = 0;
             index < rgCategory.getChildCount();
             index++) {

            rgCategory
                    .getChildAt(index)
                    .setEnabled(!submitting);
        }
    }

    private String getSelectedCategory() {
        int id = rgCategory.getCheckedRadioButtonId();
        if (id == R.id.rbRoad) return "Road Hazard";
        if (id == R.id.rbEnvironmental) return "Environmental Hazard";
        if (id == R.id.rbBuilding) return "Building Hazard";
        return "";
    }

    private void clearForm() {
        etUserName.setText("");
        etDescription.setText("");
        etLocationName.setText("");
        etLatitude.setText("");
        etLongitude.setText("");

        rgCategory.clearCheck();

        tvCoordinates.setText(
                "No location selected."
        );

        clearPreviousErrors();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                tvCoordinates.setText("Location permission denied. Please enter coordinates manually below.");
            }
        }
    }
}