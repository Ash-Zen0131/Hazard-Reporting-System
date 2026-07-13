package com.example.hazardtracker;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.hazardtracker.fragments.AboutFragment;
import com.example.hazardtracker.fragments.MapFragment;
import com.example.hazardtracker.fragments.ReportFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    private Double selectedLatitude = null;
    private Double selectedLongitude = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences =
                getSharedPreferences(
                        "app_preferences",
                        MODE_PRIVATE
                );

        boolean darkModeEnabled =
                preferences.getBoolean(
                        "dark_mode",
                        false
                );

        AppCompatDelegate.setDefaultNightMode(
                darkModeEnabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bottomNavigationView =
                findViewById(R.id.bottomNavigationView);

        if (savedInstanceState == null) {
            loadFragment(new MapFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(
                this::onNavItemSelected
        );
    }

    public void openMapPage() {
        bottomNavigationView.setSelectedItemId(
                R.id.nav_map
        );
    }

    private boolean onNavItemSelected(
            @NonNull android.view.MenuItem item
    ) {
        Fragment selectedFragment = null;

        int itemId = item.getItemId();

        if (itemId == R.id.nav_map) {
            selectedFragment = new MapFragment();

        } else if (itemId == R.id.nav_report) {
            selectedFragment = createReportFragment();

        } else if (itemId == R.id.nav_about) {
            selectedFragment = new AboutFragment();
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
            return true;
        }

        return false;
    }

    private ReportFragment createReportFragment() {
        ReportFragment reportFragment = new ReportFragment();

        if (
                selectedLatitude != null &&
                        selectedLongitude != null
        ) {
            Bundle bundle = new Bundle();

            bundle.putDouble(
                    "selectedLatitude",
                    selectedLatitude
            );

            bundle.putDouble(
                    "selectedLongitude",
                    selectedLongitude
            );

            reportFragment.setArguments(bundle);
        }

        return reportFragment;
    }

    public void openReportPage(
            double latitude,
            double longitude
    ) {
        selectedLatitude = latitude;
        selectedLongitude = longitude;

        bottomNavigationView.setSelectedItemId(
                R.id.nav_report
        );
    }

    public void clearSelectedLocation() {
        selectedLatitude = null;
        selectedLongitude = null;
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager =
                getSupportFragmentManager();

        FragmentTransaction transaction =
                fragmentManager.beginTransaction();

        transaction.replace(
                R.id.fragmentContainer,
                fragment
        );

        transaction.commit();
    }
}