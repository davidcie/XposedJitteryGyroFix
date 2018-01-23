package net.davidcie.gyroscopebandaid;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class GyroBandaidPreferenceProvider extends RemotePreferenceProvider {
    public GyroBandaidPreferenceProvider() {
        super("net.davidcie.gyroscopebandaid", new String[] {"preferences"});
    }
}
