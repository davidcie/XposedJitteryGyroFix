package net.davidcie.gyroscopebandaid.extensions;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class EditIntegerPreference extends EditTextPreference {

    public EditIntegerPreference(final Context context) {
        super(context);
    }

    public EditIntegerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public EditIntegerPreference(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public EditIntegerPreference(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected String getPersistedString(final String defaultReturnValue) {
        int defaultAsInt;
        try {
            defaultAsInt = Integer.parseInt(defaultReturnValue);
        } catch (NumberFormatException e) {
            // No default is set
            defaultAsInt = 0;
        }

        final int intValue = getPersistedInt(defaultAsInt);
        return Integer.toString(intValue);
    }

    @Override
    protected boolean persistString(final String value) {
        // TODO: check for empty string
        try {
            return persistInt(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            // This shouldn't happen as long as it has inputType="number"
            return false;
        }
    }

}
