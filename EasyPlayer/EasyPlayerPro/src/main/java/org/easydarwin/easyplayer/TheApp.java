package org.easydarwin.easyplayer;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.easydarwin.easyplayer.data.EasyDBHelper;
import org.esaydarwin.rtsp.player.R;

/**
 * Created by afd on 8/13/16.
 */
public class TheApp extends Application {

    public static final String DEFAULT_SERVER_IP = "cloud.easydarwin.org";
    public static SQLiteDatabase sDB;
    public static final String sPicturePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/EasyPlayer";
    public static final String sMoviePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/EasyPlayer";

    @Override
    public void onCreate() {
        super.onCreate();
        sDB = new EasyDBHelper(this).getWritableDatabase();
        resetServer();
    }

    public void resetServer(){
        String ip = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.key_ip), DEFAULT_SERVER_IP);
        if ("114.55.107.180".equals(ip)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString(getString(R.string.key_ip), DEFAULT_SERVER_IP).apply();
        }
    }
}
