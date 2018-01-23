package net.davidcie.gyroscopebandaid;

import android.util.Log;

import net.davidcie.gyroscopebandaid.plugins.CalibratingPlugin;
import net.davidcie.gyroscopebandaid.plugins.HistoryUpdaterPlugin;
import net.davidcie.gyroscopebandaid.plugins.IEnginePlugin;
import net.davidcie.gyroscopebandaid.plugins.InvertingPlugin;
import net.davidcie.gyroscopebandaid.plugins.RoundingPlugin;
import net.davidcie.gyroscopebandaid.plugins.SmoothingPlugin;
import net.davidcie.gyroscopebandaid.plugins.ThresholdingPlugin;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class Engine {

    // The values are measured in rad/s, which is standard since Android 2.3.
    // To get values of about 1.0 you have to turn at a speed of almost 60 deg/s.

    private float[][] mHistory;
    private List<IEnginePlugin> mPlugins = new ArrayList<>();
    private EnginePreferences mPreferences;
    private boolean mXposedAvailable;

    public Engine(boolean absoluteMode, boolean xposedAvailable) {
        Log.d(EnginePreferences.LOG_TAG, "Engine.ctor xposedAvailable=" + xposedAvailable + ", isModuleActivated=" + Util.isModuleActivated());
        mPreferences = new EnginePreferences(absoluteMode);
        mXposedAvailable = xposedAvailable && Util.isModuleActivated();

        mPlugins.add(new InvertingPlugin());
        mPlugins.add(new CalibratingPlugin());
        mPlugins.add(new HistoryUpdaterPlugin());
        mPlugins.add(new SmoothingPlugin());
        mPlugins.add(new ThresholdingPlugin());
        mPlugins.add(new RoundingPlugin());
    }

    public void newReading(float[] reading) {
        Log.d(EnginePreferences.LOG_TAG, "Engine: New reading " + Util.printArray(reading));

        // TODO: replace with a listener that changes without polling
        // Refresh preferences in case the user changed a setting
        if (mXposedAvailable) XposedModule.sPrefs.reload();
        mPreferences.reload();

        // If user changed sample size, change the size of history;
        // has to be done here because plugins have no way of returning reference
        mHistory = Util.resizeSecondDimension(mHistory, mPreferences.smoothing_sample);

        // Process the new gyroscope reading using all plugins
        if (mPreferences.enabled) {
            for (IEnginePlugin plugin : mPlugins) {
                Log.v(EnginePreferences.LOG_TAG, "Engine: Running " + plugin.getClass().getName());
                plugin.processReading(mPreferences, mHistory, reading);
                Log.v(EnginePreferences.LOG_TAG, "Engine: Current value " + Util.printArray(reading));
            }
            Log.d(EnginePreferences.LOG_TAG, "Engine: Returning " + Util.printArray(reading));
        }
    }
}
