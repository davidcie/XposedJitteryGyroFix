package net.davidcie.gyroscopebandaid.extensions;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.util.Log;

import java.util.Objects;

public class EditFloatPreference extends EditTextPreference {

    public EditFloatPreference(final Context context) {
        super(context);
    }

    public EditFloatPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public EditFloatPreference(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EditFloatPreference(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected String getPersistedString(final String defaultReturnValue) {
        Log.d("GyroBandaid", "getPersistedString defaultReturnValue=" + Objects.toString(defaultReturnValue));
        Float defaultAsFloat;
        try {
            if (defaultReturnValue != null)
                defaultAsFloat = Float.parseFloat(defaultReturnValue);
            else
                defaultAsFloat = 0.0f;
        } catch (NumberFormatException e) {
            // No default is set
            defaultAsFloat = 0.0f;
        }

        final Float floatValue = getPersistedFloat(defaultAsFloat);
        return Float.toString(floatValue);
    }

    @Override
    protected boolean persistString(final String value) {
        // TODO: check for empty string and clear preference?
        Log.d("GyroBandaid", "persistString value=" + Objects.toString(value));
        try {
            return persistFloat(Float.parseFloat(value));
        } catch (NumberFormatException e) {
            // This shouldn't happen as long as it has inputType="number"
            return false;
        }
    }

}
