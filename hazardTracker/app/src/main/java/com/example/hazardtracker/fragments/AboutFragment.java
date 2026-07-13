package com.example.hazardtracker.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch;

import com.example.hazardtracker.R;

public class AboutFragment extends Fragment {

    private static final String GITHUB_URL = "https://github.com/your-group/HazardTracker";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView tvGithubLink = view.findViewById(R.id.tvGithubLink);
        tvGithubLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
            startActivity(intent);
        });

        MaterialSwitch switchDarkMode =
                view.findViewById(R.id.switchDarkMode);

        SharedPreferences preferences =
                requireContext().getSharedPreferences(
                        "app_preferences",
                        android.content.Context.MODE_PRIVATE
                );

        boolean darkModeEnabled =
                preferences.getBoolean(
                        "dark_mode",
                        false
                );

        switchDarkMode.setChecked(darkModeEnabled);

        switchDarkMode.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    preferences.edit()
                            .putBoolean(
                                    "dark_mode",
                                    isChecked
                            )
                            .apply();

                    AppCompatDelegate.setDefaultNightMode(
                            isChecked
                                    ? AppCompatDelegate.MODE_NIGHT_YES
                                    : AppCompatDelegate.MODE_NIGHT_NO
                    );
                }
        );

        return view;
    }
}