package com.tuankiet.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.tuankiet.myapplication.data.OAuthManager;
import com.tuankiet.myapplication.databinding.ActivityMainBinding;
import com.tuankiet.myapplication.model.SocialPlatform;
import com.tuankiet.myapplication.repository.MockDataRepository;
import com.tuankiet.myapplication.ui.analytics.AnalyticsFragment;
import com.tuankiet.myapplication.ui.create.CreatePostFragment;
import com.tuankiet.myapplication.ui.home.HomeFragment;
import com.tuankiet.myapplication.ui.inbox.InboxFragment;
import com.tuankiet.myapplication.ui.settings.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final HomeFragment homeFragment = new HomeFragment();
    private final CreatePostFragment createPostFragment = new CreatePostFragment();
    private final InboxFragment inboxFragment = new InboxFragment();
    private final AnalyticsFragment analyticsFragment = new AnalyticsFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Initialize Mock Data Repository on launch
        MockDataRepository.getInstance(this);

        // Khởi động bộ quét bình luận Threads tự động mỗi 12 giây & AI Agent tự động trả lời
        com.tuankiet.myapplication.util.RealSocialPublisher.startAutoCommentScanner(this);

        setupBottomNavigation();

        // Default to Home Fragment
        if (savedInstanceState == null) {
            switchFragment(homeFragment);
        }

        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && intent.hasExtra("target_tab")) {
            int targetTab = intent.getIntExtra("target_tab", 0);
            if (targetTab != 0) {
                navigateToTab(targetTab);
            }
        }

        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            OAuthManager.getInstance(this).handleDeepLinkCallback(data, new OAuthManager.OAuthCallback() {
                @Override
                public void onSuccess(SocialPlatform platform, String message) {
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    homeFragment.loadData();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_create) {
                switchFragment(createPostFragment);
                return true;
            } else if (itemId == R.id.nav_inbox) {
                switchFragment(inboxFragment);
                return true;
            } else if (itemId == R.id.nav_analytics) {
                switchFragment(analyticsFragment);
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });
    }

    public void navigateToTab(int navItemId) {
        binding.bottomNavigation.setSelectedItemId(navItemId);
    }

    private void switchFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }
}
