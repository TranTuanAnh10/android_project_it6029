package vn.haui.android_project;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.haui.android_project.view.HomeFragment;
import vn.haui.android_project.view.NotificationsFragment;
import vn.haui.android_project.view.OrdersFragment;
import vn.haui.android_project.view.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Intent intent = new Intent(MainActivity.this, SplashScreenActivity.class);
//        startActivity(intent);


        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Mặc định mở HomeFragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new HomeFragment())
                .commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.btn_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.btn_order) {
                selectedFragment = new OrdersFragment();
            } else if (itemId == R.id.btn_notification) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.btn_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, selectedFragment)
                        .commit();
            }

            return true;
        });
    }
}