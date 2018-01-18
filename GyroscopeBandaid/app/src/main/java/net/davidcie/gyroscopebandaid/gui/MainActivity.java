package net.davidcie.gyroscopebandaid.gui;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;

import net.davidcie.gyroscopebandaid.R;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

import be.rijckaert.tim.animatedvector.FloatingMusicActionButton;
import de.robv.android.xposed.XposedBridge;


public class MainActivity extends AppCompatActivity {

    private boolean mPaused = false;
    private FloatingMusicActionButton mPlayPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        mPlayPauseButton = findViewById(R.id.playPauseButton);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPaused = !mPaused;
                updatePlayPause();
                //XposedBridge.log("GyroBandaid: Pausing at user's request");
            }
        });
    }

    private void updatePlayPause() {
        FloatingMusicActionButton.Mode transition = mPaused
                ? FloatingMusicActionButton.Mode.PLAY_TO_PAUSE
                : FloatingMusicActionButton.Mode.PAUSE_TO_PLAY;
        mPlayPauseButton.changeMode(transition);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
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

        /*@Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "Status";
                case 1: return "Settings";
                default: return null;
            }
        }*/
    }
}
