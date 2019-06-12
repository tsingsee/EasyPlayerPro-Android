package org.easydarwin.easyplayer.util;

import android.content.Context;
import android.preference.PreferenceManager;

public class SPUtil {

    /* ============================ 使用MediaCodec解码 ============================ */
    private static final String KEY_SW_CODEC = "pref.using_media_codec";

    public static boolean getMediaCodec(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SW_CODEC, false);
    }

    public static void setMediaCodec(Context context, boolean isChecked) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_SW_CODEC, isChecked)
                .apply();
    }

    /* ============================ KEY_UDP_MODE ============================ */
    private static final String KEY_UDP_MODE = "USE_UDP_MODE";

    public static boolean getUDPMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_UDP_MODE, false);
    }

    public static void setUDPMode(Context context, boolean isChecked) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_UDP_MODE, isChecked)
                .apply();
    }

    public static void setDefaultParams(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt("timeout", 5)
                .putLong("analyzeduration", 21000000L)
                .apply();
    }
}
