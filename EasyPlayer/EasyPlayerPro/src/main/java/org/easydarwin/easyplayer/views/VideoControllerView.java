package org.easydarwin.easyplayer.views;

/**
 * Created by jiaozebo on 2017/6/11.
 */

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.esaydarwin.rtsp.player.R;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import tv.danmaku.ijk.media.widget.media.IMediaController;
import tv.danmaku.ijk.media.widget.media.IjkVideoView;

public class VideoControllerView extends FrameLayout implements IMediaController {

    private static final String TAG = "VideoControllerView";

    private MediaController.MediaPlayerControl mPlayer;
    private Context mContext;
    private View mAnchor;
    private View mRoot;
    private SeekBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private static final int sDefaultTimeout = 10000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private boolean mUseFastForward;
    private boolean mFromXml;
    private boolean mListenersSet;
    private View.OnClickListener mNextListener, mPrevListener;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private ImageButton mPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;
    private ImageButton mNextButton;
    private ImageButton mPrevButton;
    private ImageButton mFullscreenButton;
    private ImageButton mRecordButton;

    private ImageButton mFastPlay;
    private ImageButton mSlowPlay;
    private TextView mTVSpeed;
    private TextView mTVRecordDuration;
    private TextView fps, kbps;

    private View takePicture, changePlayMode;
    private Handler mHandler = new MessageHandler(this);
    private long recordBeginTime;
    Runnable mRecordTickTask = new Runnable() {
        @Override
        public void run() {
            long recordSecond = (System.currentTimeMillis() - recordBeginTime) / 1000;

            if (recordSecond >= 300) {  // 分段.

            }
            recordSecond %= 3600;
            mTVRecordDuration.setText(String.format("%02d:%02d", recordSecond / 60, recordSecond % 60));
            mTVRecordDuration.setCompoundDrawablesWithIntrinsicBounds(recordSecond % 2 == 0 ? R.drawable.red_dot : R.drawable.transparent_dot, 0, 0, 0);
            postDelayed(this, 1000);

        }
    };

    private long mReceivedBytes;
    private long mReceivedPackets;
    Runnable fpsbpsTickTask = new Runnable() {
        long firstTimeStamp = 0l;

        @Override
        public void run() {
            if (firstTimeStamp == 0l) firstTimeStamp = System.currentTimeMillis();
            if (mPlayer != null && (mPlayer instanceof IjkVideoView)) {
                IjkVideoView ijk = (IjkVideoView) mPlayer;
                long l = ijk.getReceivedBytes();
                long received = l - mReceivedBytes;


                long packets = ijk.getVideoCachePackets();
                long recvdPackets = packets - mReceivedPackets;
                mReceivedBytes = l;
                mReceivedPackets = packets;
//                if (playing)

                if (ijk.isPlaying()) {
                    long time = System.currentTimeMillis() - firstTimeStamp;
                    if (time < 900) {
                        fps.setText("");
                        kbps.setText("");
                    } else {
                        recvdPackets = Math.min(recvdPackets, 30);
                        fps.setText(String.format("%dfps", recvdPackets));
                        kbps.setText(String.format("%3.01fKB/s", received * 1.0f / 1024));
                    }
                } else {
                    fps.setText("");
                    kbps.setText("");
                }
            }
            postDelayed(this, 1000);
        }
    };

    public VideoControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = null;
        mContext = context;
        mUseFastForward = true;
        mFromXml = true;

        Log.i(TAG, TAG);
    }

    public VideoControllerView(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;

        Log.i(TAG, TAG);
    }

    public VideoControllerView(Context context) {
        this(context, true);

        Log.i(TAG, TAG);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        if (mRoot != null)
            initControllerView(mRoot);
    }

    /**
     * Создает вьюху которая будет находится поверх вашего VideoView или другого контролла
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_controller, null);

        initControllerView(mRoot);

        return mRoot;
    }

    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mFullscreenButton = (ImageButton) v.findViewById(R.id.fullscreen);
        if (mFullscreenButton != null) {
            mFullscreenButton.requestFocus();
            mFullscreenButton.setOnClickListener(mFullscreenListener);
        }
        mRecordButton = (ImageButton) v.findViewById(R.id.action_record);
        if (mRecordButton != null) {
            mRecordButton.requestFocus();
            mRecordButton.setOnClickListener(mRecordingListener);
        }

        mFfwdButton = (ImageButton) v.findViewById(R.id.ffwd);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(mFfwdListener);
            if (!mFromXml) {
                mFfwdButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        mRewButton = (ImageButton) v.findViewById(R.id.rew);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(mRewListener);
            if (!mFromXml) {
                mRewButton.setVisibility(mUseFastForward ? View.VISIBLE : View.GONE);
            }
        }

        // По дефолту вьюха будет спрятана, и показываться будет после события onTouch()
        mNextButton = (ImageButton) v.findViewById(R.id.next);
        if (mNextButton != null && !mFromXml && !mListenersSet) {
            mNextButton.setVisibility(View.GONE);
        }
        mPrevButton = (ImageButton) v.findViewById(R.id.prev);
        if (mPrevButton != null && !mFromXml && !mListenersSet) {
            mPrevButton.setVisibility(View.GONE);
        }

        mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }

        mFastPlay = (ImageButton) v.findViewById(R.id.fast);
        mSlowPlay = (ImageButton) v.findViewById(R.id.slow);
        mEndTime = (TextView) v.findViewById(R.id.time);
        mCurrentTime = (TextView) v.findViewById(R.id.time_current);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        mTVRecordDuration = (TextView) v.findViewById(R.id.tv_record_time);

        fps = (TextView) v.findViewById(R.id.tv_fps);
        kbps = (TextView) v.findViewById(R.id.tv_kbps);

        mFastPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                show(sDefaultTimeout);

                if (mPlayer instanceof FullscreenableMediaPlayerControl) {
                    FullscreenableMediaPlayerControl player = (FullscreenableMediaPlayerControl) mPlayer;
                    float speed = player.getSpeed();
                    if (speed > 2.0) {
                        return;
                    }
                    if (speed >= 1.0f) {
                        mTVSpeed.setText(String.format("%d倍速", (int) (speed * 2)));
                    } else {
                        mTVSpeed.setText(String.format("%.02f倍速", speed * 2));
                    }
                    if (speed == 0.5) {
                        mTVSpeed.setVisibility(GONE);
                    } else {
                        mTVSpeed.setVisibility(VISIBLE);
                    }
                    player.setSpeed(speed * 2);
                } else {
                }
            }
        });

        mSlowPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer instanceof FullscreenableMediaPlayerControl) {
                    FullscreenableMediaPlayerControl player = (FullscreenableMediaPlayerControl) mPlayer;
                    float speed = player.getSpeed();
                    if (speed < 0.5) {
                        return;
                    }
                    if (speed >= 2.0f) {
                        mTVSpeed.setText(String.format("%d倍速", (int) (speed * 0.5)));
                    } else {
                        mTVSpeed.setText(String.format("%.02f倍速", speed * 0.5));
                    }
                    if (speed == 2.0) {
                        mTVSpeed.setVisibility(GONE);
                    } else {
                        mTVSpeed.setVisibility(VISIBLE);
                    }

                    player.setSpeed(speed * 0.5f);
                }
                show(sDefaultTimeout);
            }
        });


        takePicture = v.findViewById(R.id.action_takepicture);
        takePicture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                show(sDefaultTimeout);
                if (mPlayer instanceof FullscreenableMediaPlayerControl) {
                    FullscreenableMediaPlayerControl player = (FullscreenableMediaPlayerControl) mPlayer;
                    player.takePicture();
                }
            }
        });
        changePlayMode = v.findViewById(R.id.action_change_mode);
        changePlayMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                show(sDefaultTimeout);
                if (mPlayer instanceof FullscreenableMediaPlayerControl) {
                    FullscreenableMediaPlayerControl player = (FullscreenableMediaPlayerControl) mPlayer;
                    player.toggleMode();
                }
            }
        });

        if (this.mPlayer instanceof FullscreenableMediaPlayerControl) {
            FullscreenableMediaPlayerControl mPlayer = (FullscreenableMediaPlayerControl) this.mPlayer;

        } else {
            mFastPlay.setVisibility(GONE);
            mSlowPlay.setVisibility(GONE);
        }

        mTVSpeed = (TextView) v.findViewById(R.id.tv_speed);
        installPrevNextListeners();

        if (mPlayer.isPlaying()) {
            post(fpsbpsTickTask);
        }

        if (!mPlayer.canSeekBackward()||!mPlayer.canSeekForward()){
            v.findViewById(R.id.seekbar_container).setVisibility(GONE);
        }else{
            v.findViewById(R.id.seekbar_container).setVisibility(VISIBLE);
        }
    }


    /**
     * Отключить паузу или seek button, если поток не может быть приостановлена
     * Это требует интерфейс управления MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        if (mPlayer == null) {
            return;
        }

        try {
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !mPlayer.canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !mPlayer.canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            //выводите в лог что хотите из ex
        }
    }

    @Override
    public void show() {
        show(sDefaultTimeout);
    }

    @Override
    public void showOnce(View view) {

    }

    @Override
    public void show(int timeout) {
        if (!mShowing && mAnchor != null) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            disableUnsupportedButtons();

            FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );

            if (mAnchor instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) mAnchor;
                vg.addView(this, tlp);
            }
            mShowing = true;
        }
        updatePausePlay();
        updateFullScreen();
        updateRecord();
        updateSpeedCtrl();

        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        Message msg = mHandler.obtainMessage(FADE_OUT);
        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
        if (mPlayer != null && mPlayer.isPlaying()) {
            removeCallbacks(fpsbpsTickTask);
            post(fpsbpsTickTask);
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public void setAnchorView(View view) {
        mAnchor = view;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
    }

    @Override
    public void hide() {
        if (mAnchor == null) {
            return;
        }

        try {
            if (mAnchor instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) mAnchor;
                vg.removeView(this);
            }
            mHandler.removeMessages(SHOW_PROGRESS);
        } catch (IllegalArgumentException ex) {
            Log.w("MediaController", "already removed");
        }
        mShowing = false;
    }


    @Override
    public void setMediaPlayer(MediaController.MediaPlayerControl player) {

        mPlayer = player;
        updatePausePlay();
        updateFullScreen();
        updateRecord();
        updateSpeedCtrl();
    }

    private void updateSpeedCtrl() {
        if (mRoot == null || mRecordButton == null || this.mPlayer == null) {
            return;
        }
        if (this.mPlayer instanceof FullscreenableMediaPlayerControl) {
            FullscreenableMediaPlayerControl mPlayer = (FullscreenableMediaPlayerControl) this.mPlayer;
            if (mPlayer.speedCtrlEnable()) {
                mFastPlay.setVisibility(VISIBLE);
                mSlowPlay.setVisibility(VISIBLE);
                mTVSpeed.setVisibility(VISIBLE);

                kbps.setVisibility(GONE);
                fps.setVisibility(GONE);
            } else {
                mFastPlay.setVisibility(GONE);
                mSlowPlay.setVisibility(GONE);
                mTVSpeed.setVisibility(GONE);


                kbps.setVisibility(VISIBLE);
                fps.setVisibility(VISIBLE);
            }
        }
    }


    /**
     * Устанавливает якорь на ту вьюху на которую вы хотите разместить контролы
     * Это может быть например VideoView или SurfaceView
     */
    public void setAnchorView(ViewGroup view) {

    }


    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
//        float speed = 1.0f;
//        if (mPlayer instanceof FullscreenableMediaPlayerControl){
//            speed = ((FullscreenableMediaPlayerControl) mPlayer).getSpeed();
//        }
        int position = (int) (mPlayer.getCurrentPosition());
        // 非文件流的duration为0.
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        show(sDefaultTimeout);
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mPlayer == null) {
            return true;
        }

        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    private View.OnClickListener mFullscreenListener = new View.OnClickListener() {
        public void onClick(View v) {
            doToggleFullscreen();
            show(sDefaultTimeout);
        }
    };

    private View.OnClickListener mRecordingListener = new View.OnClickListener() {
        public void onClick(View v) {
            doToggleRecord();
            show(sDefaultTimeout);
        }
    };

    public void updatePausePlay() {
        if (mRoot == null || mPauseButton == null || mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.ic_media_pause);
        } else {
            mPauseButton.setImageResource(R.drawable.ic_media_play);
        }
    }

    public void updateFullScreen() {
        if (mRoot == null || mFullscreenButton == null || this.mPlayer == null) {
            return;
        }
        if (this.mPlayer instanceof FullscreenableMediaPlayerControl) {
            FullscreenableMediaPlayerControl mPlayer = (FullscreenableMediaPlayerControl) this.mPlayer;
            if (mPlayer.isFullScreen()) {
                mFullscreenButton.setImageResource(R.drawable.ic_media_fullscreen_shrink);
            } else {
                mFullscreenButton.setImageResource(R.drawable.ic_media_fullscreen_stretch);
            }
        }
    }


    private void updateRecord() {
        if (mRoot == null || mRecordButton == null || this.mPlayer == null) {
            return;
        }
        if (this.mPlayer instanceof FullscreenableMediaPlayerControl) {
            FullscreenableMediaPlayerControl mPlayer = (FullscreenableMediaPlayerControl) this.mPlayer;
            if (mPlayer.isRecording()) {
                mRecordButton.setImageResource(R.drawable.ic_action_record_checked);
                removeCallbacks(mRecordTickTask);
                post(mRecordTickTask);
            } else {
                mRecordButton.setImageResource(R.drawable.ic_action_record_idle);
            }
            if (mPlayer.recordEnable()) {
                mRecordButton.setVisibility(VISIBLE);
            } else {
                mRecordButton.setVisibility(GONE);
            }
        }
    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }
        removeCallbacks(fpsbpsTickTask);
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
            post(fpsbpsTickTask);
            mReceivedBytes = 0;
            mReceivedPackets = 0;
        }
        updatePausePlay();
    }

    private void doToggleFullscreen() {
        if (mPlayer == null) {
            return;
        }
        if (this.mPlayer instanceof FullscreenableMediaPlayerControl) {
            FullscreenableMediaPlayerControl mPlayer = (FullscreenableMediaPlayerControl) this.mPlayer;
            mPlayer.toggleFullScreen();
        }
        updateFullScreen();
    }

    private void doToggleRecord() {
        if (mPlayer == null) {
            return;
        }
        if (this.mPlayer instanceof FullscreenableMediaPlayerControl) {
            FullscreenableMediaPlayerControl mPlayer = (FullscreenableMediaPlayerControl) this.mPlayer;
            mPlayer.toggleRecord();
            if (mPlayer.isRecording()) {
                findViewById(R.id.tv_record_time).setVisibility(VISIBLE);
                recordBeginTime = System.currentTimeMillis();
                post(mRecordTickTask);
            } else {
                findViewById(R.id.tv_record_time).setVisibility(GONE);
                removeCallbacks(mRecordTickTask);
            }
        }
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                return;
            }

            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            mPlayer.seekTo((int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime((int) newposition));
        }

        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);

            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mNextButton != null) {
            mNextButton.setEnabled(enabled && mNextListener != null);
        }
        if (mPrevButton != null) {
            mPrevButton.setEnabled(enabled && mPrevListener != null);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }


    private View.OnClickListener mRewListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos -= 5000; // милисекунд
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };

    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (mPlayer == null) {
                return;
            }

            int pos = mPlayer.getCurrentPosition();
            pos += 15000; // милисекунд
            mPlayer.seekTo(pos);
            setProgress();

            show(sDefaultTimeout);
        }
    };

    private void installPrevNextListeners() {
        if (mNextButton != null) {
            mNextButton.setOnClickListener(mNextListener);
            mNextButton.setEnabled(mNextListener != null);
        }

        if (mPrevButton != null) {
            mPrevButton.setOnClickListener(mPrevListener);
            mPrevButton.setEnabled(mPrevListener != null);
        }
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextListener = next;
        mPrevListener = prev;
        mListenersSet = true;

        if (mRoot != null) {
            installPrevNextListeners();

            if (mNextButton != null && !mFromXml) {
                mNextButton.setVisibility(View.VISIBLE);
            }
            if (mPrevButton != null && !mFromXml) {
                mPrevButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public interface FullscreenableMediaPlayerControl extends MediaController.MediaPlayerControl {
        boolean isFullScreen();

        void toggleFullScreen();

        boolean recordEnable();

        boolean speedCtrlEnable();

        boolean isRecording();

        void toggleRecord();

        float getSpeed();

        void setSpeed(float speed);

        void takePicture();

        void toggleMode();
    }

    private static class MessageHandler extends Handler {
        private final WeakReference<VideoControllerView> mView;

        MessageHandler(VideoControllerView view) {
            mView = new WeakReference<VideoControllerView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoControllerView view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    view.hide();
                    break;
                case SHOW_PROGRESS:
                    pos = view.setProgress();
                    if (!view.mDragging && view.mShowing && view.mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }
}

