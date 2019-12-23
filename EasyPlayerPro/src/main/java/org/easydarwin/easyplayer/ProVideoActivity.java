package org.easydarwin.easyplayer;

/*
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.easydarwin.easyplayer.databinding.ActivityMainProBinding;
import org.easydarwin.easyplayer.util.FileUtil;
import org.easydarwin.easyplayer.util.SPUtil;
import org.easydarwin.easyplayer.views.ProVideoView;
import org.easydarwin.easyplayer.views.VideoControllerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class ProVideoActivity extends AppCompatActivity {
    private static final String TAG = "ProVideoActivity";

    public static final int REQUEST_WRITE_STORAGE = 111;

    private String mVideoPath;
    private Uri mVideoUri;

    private ActivityMainProBinding mBinding;
    private ProVideoView mVideoView;        // 播放器View
    private View mProgress;

    private GestureDetector detector;

    private VideoControllerView mediaController;
    private MediaScannerConnection mScanner;

    private Runnable mSpeedCalcTask;

    private int mMode;                      // 画面模式

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main_pro);

        // handle arguments
        mVideoPath = getIntent().getStringExtra("videoPath");

        String mSnapPath = getIntent().getStringExtra("snapPath");
        if (TextUtils.isEmpty(mSnapPath)) {
            Glide.with(this).load(mSnapPath).into(mBinding.surfaceCover);
            ViewCompat.setTransitionName(mBinding.surfaceCover, "snapCover");
        }

        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (!TextUtils.isEmpty(intentAction)) {
            if (intentAction.equals(Intent.ACTION_VIEW)) {
                mVideoPath = intent.getDataString();
            } else if (intentAction.equals(Intent.ACTION_SEND)) {
                mVideoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    String scheme = mVideoUri.getScheme();

                    if (TextUtils.isEmpty(scheme)) {
                        Log.e(TAG, "Null unknown scheme\n");
                        finish();
                        return;
                    }

                    if (scheme.equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
                        mVideoPath = mVideoUri.getPath();
                    } else if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
                        Log.e(TAG, "Can not resolve content below Android-ICS\n");
                        finish();
                        return;
                    } else {
                        Log.e(TAG, "Unknown scheme " + scheme + "\n");
                        finish();
                        return;
                    }
                }
            }
        }

        SPUtil.setDefaultParams(this);

        if (BuildConfig.DEBUG) {
            IjkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);// init player
        }

        mediaController = new VideoControllerView(this);
        mediaController.setMediaPlayer(mBinding.videoView);
        mVideoView = mBinding.videoView;
        mVideoView.setMediaController(mediaController);

        mProgress = findViewById(android.R.id.progress);
        mVideoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int arg1, int arg2) {
                switch (arg1) {
                    case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                        Log.i(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                        break;
                    case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        Log.i(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                        mProgress.setVisibility(View.GONE);
                        mBinding.surfaceCover.setVisibility(View.GONE);
                        mBinding.playerContainer.setVisibility(View.GONE);
                        mBinding.videoView.setVisibility(View.VISIBLE);
                        mBinding.videoView2.setVisibility(View.VISIBLE);

                        // 快照
                        File file = FileUtil.getSnapshotFile(mVideoPath);
                        mVideoView.takePicture(file.getPath());
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        Log.i(TAG, "MEDIA_INFO_BUFFERING_START");
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        Log.i(TAG, "MEDIA_INFO_BUFFERING_END");
                        break;
                    case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                        Log.i(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH");
                        break;
                    case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                        Log.i(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
                        break;
                    case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                        Log.i(TAG, "MEDIA_INFO_NOT_SEEKABLE");
                        break;
                    case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                        Log.i(TAG, "MEDIA_INFO_METADATA_UPDATE");
                        break;
                    case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                        Log.i(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE");
                        break;
                    case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                        Log.i(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT");
                        break;
                    case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                        Log.i(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED");
                        break;
                    case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                        Log.i(TAG, "MEDIA_INFO_AUDIO_RENDERING_START");
                        break;
                }

                return false;
            }
        });

        mVideoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                Log.i(TAG, "播放错误");
                mBinding.videoView.setVisibility(View.GONE);
                mBinding.videoView2.setVisibility(View.GONE);
                mProgress.setVisibility(View.GONE);
                mBinding.playerContainer.setVisibility(View.VISIBLE);

                mVideoView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mVideoView.reStart();
                    }
                }, 5000);

                return true;
            }
        });

        mVideoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer iMediaPlayer) {
                Log.i(TAG, "播放完成");
                mProgress.setVisibility(View.GONE);

                if (mVideoPath.toLowerCase().startsWith("rtsp") ||
                        mVideoPath.toLowerCase().startsWith("rtmp") ||
                        mVideoPath.toLowerCase().startsWith("http")) {
                    mVideoView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mVideoView.reStart();
                        }
                    }, 5000);
                }
            }
        });

        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                Log.i(TAG, String.format("onPrepared"));
            }
        });

        if (mVideoPath != null) {
            mVideoView.setVideoPath(mVideoPath);
        } else if (mVideoUri != null) {
            mVideoView.setVideoURI(mVideoUri);
        } else {
            Log.e(TAG, "Null Data Source\n");
            finish();
            return;
        }

        mVideoView.start();

        GestureDetector.SimpleOnGestureListener listener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
//                if (!isLandscape() && mMode == 3) {
//                    if (mFullScreenMode) {
//                        mMode = mVideoView.toggleAspectRatio();
//                        mBinding.playerContainer.setVisibility(View.VISIBLE);
//                        mFullScreenMode = false;
//                    } else {
//                        mFullScreenMode = true;
//                        mBinding.playerContainer.setVisibility(View.GONE);
//                    }
//                } else {
//                    mMode = mVideoView.toggleAspectRatio();
//                }

                if (mVideoView.isInPlaybackState()) {
                    mVideoView.toggleMediaControlsVisibility();
                    return true;
                }

                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
//                setRequestedOrientation(isLandscape() ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                return true;
            }
        };

        detector = new GestureDetector(this, listener);

        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                detector.onTouchEvent(event);

                return true;
            }
        });

        mSpeedCalcTask = new Runnable() {
            private long mReceivedBytes;

            @Override
            public void run() {
                long l = mVideoView.getReceivedBytes();
                long received = l - mReceivedBytes;
                mReceivedBytes = l;

                TextView view = (TextView) findViewById(R.id.loading_speed);
                view.setText(String.format("%3.01fKB/s", received * 1.0f / 1024));

                if (findViewById(android.R.id.progress).getVisibility() == View.VISIBLE){
                    mVideoView.postDelayed(this,1000);
                }
            }
        };

        mVideoView.post(mSpeedCalcTask);

        if (BuildConfig.DEBUG) {
            mBinding.videoView2.setVideoPath("rtmp://13088.liveplay.myqcloud.com/live/13088_65829b3d3e");
            mBinding.videoView2.setShowing(false);
            mBinding.videoView2.start();
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        if (mVideoView != null) {
//            mVideoView.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mVideoView.reStart();
//                }
//            }, 5000);
//        }
//    }

    @Override
    protected void onStop() {
        super.onStop();

        mVideoView.stopPlayback();

        if (BuildConfig.DEBUG) {
            mBinding.videoView2.stopPlayback();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mScanner != null) {
            mScanner.disconnect();
            mScanner = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (REQUEST_WRITE_STORAGE == requestCode){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doTakePicture();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LinearLayout container = mBinding.playerContainer;

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(false);
            // 横屏情况 播放窗口横着排开
            container.setOrientation(LinearLayout.HORIZONTAL);
        } else {
            // 竖屏,取消全屏状态
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavVisibility(true);
            // 竖屏情况 播放窗口竖着排开
            container.setOrientation(LinearLayout.VERTICAL);
        }
    }

    public boolean isLandscape() {
        int orientation = getResources().getConfiguration().orientation;
        return orientation == ORIENTATION_LANDSCAPE;
    }

    public void setNavVisibility(boolean visible) {
        if (!ViewConfigurationCompat.hasPermanentMenuKey(ViewConfiguration.get(this))) {
            int newVis = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

            if (!visible) {
                // } else {
                // newVis &= ~(View.SYSTEM_UI_FLAG_LOW_PROFILE |
                // View.SYSTEM_UI_FLAG_FULLSCREEN |
                // View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                newVis |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE;
            }

            // If we are now visible, schedule a timer for us to go invisible. Set the new desired visibility.
            getWindow().getDecorView().setSystemUiVisibility(newVis);
        }
    }

    public void onChangeOrientation(View view) {
        setRequestedOrientation(isLandscape() ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void onChangePlayMode(View view) {
        mMode = mVideoView.toggleAspectRatio();
        Log.i(TAG, "画面模式：" + mMode);
    }

    /*
    * 截图
    * */
    public void onTakePicture(View view) {
        if (mVideoView.isInPlaybackState()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            } else {
                doTakePicture();
            }
        }
    }

    private void doTakePicture() {
        File file = new File(FileUtil.getPicturePath());
        file.mkdirs();

        file = new File(file, "pic_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg");
        final String picture = mVideoView.takePicture(file.getPath());

        if (!TextUtils.isEmpty(picture)) {
            Toast.makeText(ProVideoActivity.this,"图片已保存", Toast.LENGTH_SHORT).show();

            if (mScanner == null) {
                MediaScannerConnection connection = new MediaScannerConnection(ProVideoActivity.this, new MediaScannerConnection.MediaScannerConnectionClient() {
                    public void onMediaScannerConnected() {
                        mScanner.scanFile(picture, "image/jpeg");
                    }

                    public void onScanCompleted(String path1, Uri uri) {

                    }
                });

                try {
                    connection.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mScanner = connection;
            } else {
                mScanner.scanFile(picture, "image/jpeg");
            }
        }
    }
}
