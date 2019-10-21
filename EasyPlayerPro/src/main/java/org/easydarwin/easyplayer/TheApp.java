package org.easydarwin.easyplayer;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;

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

        Bugly.init(getApplicationContext(), "7f6f2634cc", false);
        setBuglyInit();
    }

    public void setBuglyInit(){
        // 添加可显示弹窗的Activity
        Beta.canShowUpgradeActs.add(PlayListActivity.class);

//        例如，只允许在MainActivity上显示更新弹窗，其他activity上不显示弹窗; 如果不设置默认所有activity都可以显示弹窗。
//        设置是否显示消息通知
        Beta.enableNotification = true;

//        如果你不想在通知栏显示下载进度，你可以将这个接口设置为false，默认值为true。
//        设置Wifi下自动下载
        Beta.autoDownloadOnWifi = false;

//        如果你想在Wifi网络下自动下载，可以将这个接口设置为true，默认值为false。
//        设置是否显示弹窗中的apk信息
        Beta.canShowApkInfo = true;

//        如果你使用我们默认弹窗是会显示apk信息的，如果你不想显示可以将这个接口设置为false。
//        关闭热更新能力
        Beta.enableHotfix = true;

        Beta.largeIconId = R.mipmap.ic_launcher_foreground;
        Beta.smallIconId = R.mipmap.ic_launcher_foreground;

        // 设置是否显示消息通知
        Beta.enableNotification = true;
    }
}
