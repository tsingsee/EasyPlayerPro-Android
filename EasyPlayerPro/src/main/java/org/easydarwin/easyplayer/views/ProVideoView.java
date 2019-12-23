package org.easydarwin.easyplayer.views;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.easydarwin.easyplayer.ProVideoActivity;
import org.easydarwin.easyplayer.util.FileUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.widget.media.IjkVideoView;

import static tv.danmaku.ijk.media.player.IjkMediaPlayer.native_active_days;

/**
 * 播放器
 *
 * Created by apple on 2017/2/11.
 */
public class ProVideoView extends IjkVideoView implements VideoControllerView.FullScreenAbleMediaPlayerControl {

    private String mRecordPath;

    public ProVideoView(Context context) {
        super(context);
    }

    public ProVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ProVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public static void setKey(String key) {
        Player_KEY = key;
    }

    public static long getActiveDays(Context context, String key) {
        return native_active_days(context, key);
    }

    /** ================ super override ================ */

    public void startRecord(String path, int seconds) {
        if (mMediaPlayer == null) {
            return;
        }

        super.startRecord(path,seconds);
        mRecordPath = path;
    }

    public void stopRecord() {
        if (mMediaPlayer == null){
            return;
        }

        super.stopRecord();
        mRecordPath = null;
    }

    /** ================ FullScreenAbleMediaPlayerControl ================ */

    @Override
    public boolean isFullScreen() {
        if (getContext() instanceof ProVideoActivity) {
            ProVideoActivity pro = (ProVideoActivity) getContext();
            return pro.isLandscape();
        }

        return false;
    }

    @Override
    public void toggleFullScreen() {
        if (getContext() instanceof ProVideoActivity) {
            ProVideoActivity pro = (ProVideoActivity) getContext();
            pro.onChangeOrientation(null);
        }
    }

    @Override
    public boolean recordEnable() {
        Uri uri = mUri;

        if (uri == null)
            return false;

        if (uri.getScheme() == null)
            return false;

        return !uri.getScheme().equals("file");
    }

    @Override
    public boolean speedCtrlEnable() {
        Uri uri = mUri;

        if (uri == null)
            return false;

        if (uri.getScheme() == null)
            return true;

        return uri.getScheme().equals("file");
    }

    @Override
    public boolean isRecording() {
        if (mMediaPlayer == null) {
            return false;
        }

        return !TextUtils.isEmpty(mRecordPath);
    }

    @Override
    public void reStart() {
        super.reStart();
        if (mRecordPath != null){
            toggleRecord();
            toggleRecord();
        }
    }

    @Override
    public void toggleRecord() {
        if (getContext() instanceof ProVideoActivity) {
            ProVideoActivity pro = (ProVideoActivity) getContext();

            if (ActivityCompat.checkSelfPermission(pro, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(pro, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, ProVideoActivity.REQUEST_WRITE_STORAGE +1);
                return;
            }
        }

        if (!isRecording()) {
            Uri uri = mUri;
            if (uri == null)
                return;

            mRecordPath = "record_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".mp4";
            File directory = new File(FileUtil.getMoviePath());

            try {
                directory.mkdirs();
                startRecord(directory + "/" + mRecordPath, 30);
            } catch (Exception ex) {
                ex.printStackTrace();
                mRecordPath = null;
            }
        } else {
            stopRecord();
        }
    }

    @Override
    public float getSpeed() {
        if (mMediaPlayer == null) {
            return 1.0f;
        }

        if (mMediaPlayer instanceof IjkMediaPlayer) {
            IjkMediaPlayer player = (IjkMediaPlayer) mMediaPlayer;
            return player.getSpeed();
        }

        return 1.0f;
    }

    @Override
    public void setSpeed(float speed) {
        if (mMediaPlayer == null ) {
            return ;
        }

        if (mMediaPlayer instanceof IjkMediaPlayer) {
            IjkMediaPlayer player = (IjkMediaPlayer) mMediaPlayer;
            player.setSpeed(speed);
        }
    }

    @Override
    public void takePicture() {
        if (getContext() instanceof ProVideoActivity){
            ProVideoActivity pro = (ProVideoActivity) getContext();
            pro.onTakePicture(null);
        }
    }

    @Override
    public void toggleMode() {
        if (getContext() instanceof ProVideoActivity) {
            ProVideoActivity pro = (ProVideoActivity) getContext();
            pro.onChangePlayMode(null);
        }
    }

    @Override
    public boolean isCompleted() {
        if (mMediaPlayer instanceof IjkMediaPlayer) {
            IjkMediaPlayer player = (IjkMediaPlayer) mMediaPlayer;
            return player.isCompleted();
        }

        return false;
    }
}
