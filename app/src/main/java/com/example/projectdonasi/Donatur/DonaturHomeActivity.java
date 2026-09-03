package com.example.projectdonasi.Donatur;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.view.MenuItem;

import com.example.projectdonasi.NavigationVisibilityListener;
import com.example.projectdonasi.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;


public class DonaturHomeActivity extends AppCompatActivity implements NavigationVisibilityListener {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donatur_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new DonaturHomeFragment();
                } else if (itemId == R.id.nav_transaction) {
                    selectedFragment = new DonaturTransaksiFragment();
                } else if (itemId == R.id.nav_setting) {
                    selectedFragment = new DonaturSettingFragment();
                }

                return loadFragment(selectedFragment);
            }
        });

        loadFragment(new DonaturHomeFragment());
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    public void setNavigationBarVisibility(int visibility) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(visibility);
        }
    }


    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}