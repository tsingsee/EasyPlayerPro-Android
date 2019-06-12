package org.easydarwin.easyplayer;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import org.easydarwin.easyplayer.data.EasyDBHelper;

/**
 * Created by afd on 8/13/16.
 */
public class TheApp extends Application {

    public static final String DEFAULT_SERVER_IP = "cloud.easydarwin.org";
    public static SQLiteDatabase sDB;

    @Override
    public void onCreate() {
        super.onCreate();

        sDB = new EasyDBHelper(this).getWritableDatabase();
    }
}
