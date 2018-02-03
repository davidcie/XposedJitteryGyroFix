package net.davidcie.gyroscopebandaid.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import net.davidcie.gyroscopebandaid.R;
import net.davidcie.gyroscopebandaid.Util;

import be.rijckaert.tim.animatedvector.FloatingMusicActionButton;

import static net.davidcie.gyroscopebandaid.Util.isXposedInstalled;


public class MainActivity extends Activity {

    private boolean mPaused = false; // TODO: read from settings
    private FloatingMusicActionButton mPlayPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Initialize preferences
        //PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, false);

        // Set up the ViewPager and tabs
        ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(new MyPagerAdapter(getFragmentManager()));
        TabLayout tabLayout = findViewById(R.id.tabs);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));



    }


    public class MyPagerAdapter extends FragmentPagerAdapter {

        MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return new StatusTab();
                case 1: return new SettingsTab();
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
