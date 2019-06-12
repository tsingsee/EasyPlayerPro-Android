package org.easydarwin.easyplayer;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;

import org.easydarwin.easyplayer.data.VideoSource;
import org.easydarwin.easyplayer.databinding.ActivityPlayListBinding;
import org.easydarwin.easyplayer.databinding.VideoSourceItemBinding;
import org.easydarwin.easyplayer.util.FileUtil;
import org.easydarwin.easyplayer.util.SPUtil;
import org.easydarwin.easyplayer.views.ProVideoView;
import org.easydarwin.update.UpdateMgr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.easydarwin.update.UpdateMgr.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;

/**
 * 视频广场
 * */
public class PlayListActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final int REQUEST_PLAY = 1000;
    public static final int REQUEST_CAMERA_PERMISSION = 1001;
    private static final int REQUEST_SCAN_TEXT_URL = 1003;      // 扫描二维码

    public static final String EXTRA_BOOLEAN_SELECT_ITEM_TO_PLAY = "extra-boolean-select-item-to-play";

    private int mPos;
    private ActivityPlayListBinding mBinding;
    private RecyclerView mRecyclerView;
    private EditText edit;

    private Cursor mCursor;

    private UpdateMgr update;

    private long mExitTime;//声明一个long类型变量：用于存放上一点击“返回键”的时刻

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_play_list);

        setSupportActionBar(mBinding.toolbar);
        notifyAboutColorChange();

        // 添加默认地址
        mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
        if (!mCursor.moveToFirst()) {
            List<String> urls = new ArrayList<>();
            urls.add("rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov");
            urls.add("rtmp://live.hkstv.hk.lxdns.com/live/hks2");
            urls.add("http://www.easydarwin.org/public/video/3/video.m3u8");
            urls.add("http://m4.pptvyun.com/pvod/e11a0/ijblO6coKRX6a8NEQgg8LDZcqPY/eyJkbCI6MTUxNjYyNTM3NSwiZXMiOjYwNDgwMCwiaWQiOiIwYTJkbnEtWG82S2VvcTZMNEsyZG9hZmhvNkNjbTY2WXB3IiwidiI6IjEuMCJ9/0a2dnq-Xo6Keoq6L4K2doafho6Ccm66Ypw.mp4");

            for (String url : urls) {
                ContentValues cv = new ContentValues();
                cv.put(VideoSource.URL, url);
                TheApp.sDB.insert(VideoSource.TABLE_NAME, null, cv);

                mCursor.close();
                mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
            }

            SPUtil.setMediaCodec(this, true);
            SPUtil.setUDPMode(this, false);
        }

        mRecyclerView = mBinding.recycler;
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new PlayListViewHolder((VideoSourceItemBinding) DataBindingUtil.inflate(getLayoutInflater(), R.layout.video_source_item, parent, false));
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                PlayListViewHolder plvh = (PlayListViewHolder) holder;
                mCursor.moveToPosition(position);
                String name = mCursor.getString(mCursor.getColumnIndex(VideoSource.NAME));
                String url = mCursor.getString(mCursor.getColumnIndex(VideoSource.URL));

                if (!TextUtils.isEmpty(name)) {
                    plvh.mTextView.setText(name);
                } else {
                    plvh.mTextView.setText(url);
                }

                File file = FileUtil.getSnapshotFile(url);

                Glide.with(PlayListActivity.this)
                        .load(file)
                        .signature(new StringSignature(UUID.randomUUID().toString()))
                        .placeholder(R.drawable.placeholder)
                        .centerCrop()
                        .into(plvh.mImageView);

                int audienceNumber = mCursor.getInt(mCursor.getColumnIndex(VideoSource.AUDIENCE_NUMBER));

                if (audienceNumber > 0) {
                    plvh.mAudienceNumber.setText(String.format("当前观看人数:%d", audienceNumber));
                    plvh.mAudienceNumber.setVisibility(View.VISIBLE);
                } else {
                    plvh.mAudienceNumber.setVisibility(View.GONE);
                }
            }

            @Override
            public int getItemCount() {
                return mCursor.getCount();
            }
        });

        // 如果当前进程挂起，则进入启动页
        if (savedInstanceState == null) {
            if (!getIntent().getBooleanExtra(EXTRA_BOOLEAN_SELECT_ITEM_TO_PLAY, false)) {
                startActivity(new Intent(this, SplashActivity.class));
            }
        }

        mBinding.pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mBinding.pullToRefresh.setRefreshing(false);
            }
        });

        mBinding.toolbarSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PlayListActivity.this, SettingsActivity.class));
            }
        });

        mBinding.toolbarAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayDialog(-1);
            }
        });

        mBinding.toolbarAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PlayListActivity.this, AboutActivity.class));
            }
        });

        /* ==================== 版本更新 ==================== */
        String url = "http://www.easydarwin.org/versions/easyplayer_pro/version.txt";;
        update = new UpdateMgr(this);
        update.checkUpdate(url);
    }

    @Override
    protected void onDestroy() {
        mCursor.close();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    update.doDownload();
                }
            case REQUEST_CAMERA_PERMISSION: {
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    toScanQRActivity();
                }

                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        PlayListViewHolder holder = (PlayListViewHolder) view.getTag();
        int pos = holder.getAdapterPosition();

        if (pos != -1) {
            mCursor.moveToPosition(pos);
            String playUrl = mCursor.getString(mCursor.getColumnIndex(VideoSource.URL));

            if (!TextUtils.isEmpty(playUrl)) {
                mPos = pos;

                Intent i = new Intent(PlayListActivity.this, ProVideoActivity.class);
                i.putExtra("videoPath", playUrl);
                startActivityForResult(i, 999);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        PlayListViewHolder holder = (PlayListViewHolder) view.getTag();
        final int pos = holder.getAdapterPosition();

        if (pos != -1) {
            new AlertDialog.Builder(this).setItems(new CharSequence[]{"修改", "删除"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 0) {
                        displayDialog(pos);
                    } else {
                        new AlertDialog
                                .Builder(PlayListActivity.this)
                                .setMessage("确定要删除该地址吗？")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        mCursor.moveToPosition(pos);
                                        TheApp.sDB.delete(VideoSource.TABLE_NAME, VideoSource._ID + "=?", new String[]{String.valueOf(mCursor.getInt(mCursor.getColumnIndex(VideoSource._ID)))});
                                        mCursor.close();
                                        mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
                                        mRecyclerView.getAdapter().notifyItemRemoved(pos);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    }
                }
            }).show();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        //与上次点击返回键时刻作差
        if ((System.currentTimeMillis() - mExitTime) > 2000) {
            //大于2000ms则认为是误操作，使用Toast进行提示
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            //并记录下本次点击“返回键”的时刻，以便下次进行判断
            mExitTime = System.currentTimeMillis();
        } else {
            super.onBackPressed();
        }
    }

    private void displayDialog(final int pos) {
        String url = "";
        if (pos > -1) {
            mCursor.moveToPosition(pos);
            url = mCursor.getString(mCursor.getColumnIndex(VideoSource.URL));
        }

        View view = getLayoutInflater().inflate(R.layout.new_media_source_dialog, null);
        edit = view.findViewById(R.id.new_media_source_url);
        edit.setText(url);
        edit.setSelection(url.length());

        // 去扫描二维码
        final ImageButton btn = view.findViewById(R.id.new_media_scan);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 动态获取camera和audio权限
                if (ActivityCompat.checkSelfPermission(PlayListActivity.this, Manifest.permission.CAMERA) != PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(PlayListActivity.this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(PlayListActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_CAMERA_PERMISSION);
                } else {
                    toScanQRActivity();
                }
            }
        });

        final AlertDialog dlg = new AlertDialog.Builder(PlayListActivity.this)
                .setView(view)
                .setTitle("请输入播放地址")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String url = String.valueOf(edit.getText());

                        if (TextUtils.isEmpty(url)) {
                            return;
                        }

                        if (url.toLowerCase().indexOf("rtsp://") != 0 && url.toLowerCase().indexOf("rtmp://") != 0 &&
                            url.toLowerCase().indexOf("http://") != 0 && url.toLowerCase().indexOf("hls://") != 0) {
                            Toast.makeText(PlayListActivity.this,"不是合法的地址，请重新添加.",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ContentValues cv = new ContentValues();
                        cv.put(VideoSource.URL, url);

                        if (pos > -1) {
                            final int _id = mCursor.getInt(mCursor.getColumnIndex(VideoSource._ID));
                            TheApp.sDB.update(VideoSource.TABLE_NAME, cv, VideoSource._ID + "=?", new String[]{String.valueOf(_id)});

                            mCursor.close();
                            mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
                            mRecyclerView.getAdapter().notifyItemChanged(pos);
                        } else {
                            TheApp.sDB.insert(VideoSource.TABLE_NAME, null, cv);

                            mCursor.close();
                            mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
                            mRecyclerView.getAdapter().notifyItemInserted(mCursor.getCount() - 1);
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        dlg.show();
    }

    private void toScanQRActivity() {
        Intent intent = new Intent(PlayListActivity.this, ScanQRActivity.class);
        startActivityForResult(intent, REQUEST_SCAN_TEXT_URL);
        overridePendingTransition(R.anim.slide_bottom_in, R.anim.slide_top_out);
    }

    /*
     * 显示key有限期
     * */
    private void notifyAboutColorChange() {
        //// !!!! important to set KEY  !!!!
        ProVideoView.setKey(BuildConfig.PLAYER_KEY);
        long activeDays = ProVideoView.getActiveDays(this,BuildConfig.PLAYER_KEY);

        ImageView iv = findViewById(R.id.toolbar_about);

        if (activeDays >= 9999) {
            iv.setImageResource(R.drawable.new_version1);
        } else if (activeDays > 0) {
            iv.setImageResource(R.drawable.new_version2);
        } else {
            iv.setImageResource(R.drawable.new_version3);
        }
    }

    public void fileList(View view) {
        Intent i = new Intent(this, MediaFilesActivity.class);
        startActivity(i);
    }

    /**
     * 视频源的item
     * */
    class PlayListViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;
        private final TextView mAudienceNumber;
        private final ImageView mImageView;

        public PlayListViewHolder(VideoSourceItemBinding binding) {
            super(binding.getRoot());

            mTextView = binding.videoSourceItemName;
            mAudienceNumber = binding.videoSourceItemAudienceNumber;
            mImageView = binding.videoSourceItemThumb;

            itemView.setOnClickListener(PlayListActivity.this);
            itemView.setOnLongClickListener(PlayListActivity.this);
            itemView.setTag(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SCAN_TEXT_URL) {
            if (resultCode == RESULT_OK) {
                String url = data.getStringExtra("text");
                edit.setText(url);
            }
        } else {
//            mRecyclerView.getAdapter().notifyItemChanged(mPos);
            mRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}
