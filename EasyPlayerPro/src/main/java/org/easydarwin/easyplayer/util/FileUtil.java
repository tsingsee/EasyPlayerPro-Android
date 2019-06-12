package org.easydarwin.easyplayer.util;

import android.os.Environment;

import java.io.File;

public class FileUtil {

    private static String path = Environment.getExternalStorageDirectory() +"/EasyPlayerRro";

    public static String getPicturePath() {
        return path + "/picture";
    }

    public static File getSnapshotFile(String url) {
        File file = new File(getPicturePath() + urlDir(url));
        file.mkdirs();

        file = new File(file, "snapshot.jpg");

        return file;
    }

    public static String getMoviePath() {
        return path + "/movie";
    }

    private static String urlDir(String url) {
        url = url.replace("://", "");
        url = url.replace("/", "");
        url = url.replace(".", "");

        if (url.length() > 64) {
            url.substring(0, 63);
        }

        return url;
    }
}
