package org.easydarwin.easyplayer;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
//import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
//import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
//import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.easydarwin.easyplayer.fragments.FixedListFragment;
import org.easydarwin.easyplayer.fragments.SquareFragment;
import org.easydarwin.update.UpdateMgr;
import org.esaydarwin.rtsp.player.BuildConfig;
import org.esaydarwin.rtsp.player.R;
import org.esaydarwin.rtsp.player.databinding.ContentPlaylistBinding;
import org.esaydarwin.rtsp.player.databinding.FragmentFileBinding;
import org.esaydarwin.rtsp.player.databinding.FragmentMediaFileBinding;
import org.esaydarwin.rtsp.player.databinding.ImagePickerItemBinding;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.easydarwin.update.UpdateMgr.MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;

public class PlaylistActivity extends AppCompatActivity {

    private static final int REQUEST_PLAY = 1000;
    private static final String TAG_FIX = "tag_fix";
    private static final String TAG_SQUARE = "tag_square";
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1011;

    private ContentPlaylistBinding mBinding;
    private UpdateMgr update;
    private boolean mShouldOpenLocalFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.content_playlist);
//        setContentView(R.layout.content_playlist);
        setSupportActionBar(mBinding.toolbar);

        if (savedInstanceState == null) {
            startActivity(new Intent(this, SplashActivity.class));
        }

        mBinding.toolbarSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PlaylistActivity.this, SettingsActivity.class));
            }
        });

        mBinding.toolbarAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText edit = new EditText(PlaylistActivity.this);
                edit.setHint("RTSP/RTMP/HTTP/HLS地址");
                if (BuildConfig.DEBUG){
                    edit.setText("rtmp://121.40.200.119:10085/live/stream_1022");
                    edit.setText("rtmp://easydss.com:10085/live/stream_903196");
                }
                final int hori = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
                final int verti = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
                edit.setPadding(hori, verti, hori, verti);
                final AlertDialog dlg = new AlertDialog.Builder(PlaylistActivity.this).setView(edit).setTitle("请输入播放地址").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String mRTSPUrl = String.valueOf(edit.getText());
                        if (TextUtils.isEmpty(mRTSPUrl)) {
                            return;
                        }
                        FixedListFragment fix = (FixedListFragment) getSupportFragmentManager().findFragmentByTag(TAG_FIX);
                        fix.addUrl(mRTSPUrl);
                    }
                }).setNegativeButton("取消", null).create();
                dlg.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialogInterface) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                dlg.show();
            }
        });
        mBinding.toolbarAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PlaylistActivity.this, AboutActivity.class));
            }
        });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = FixedListFragment.newInstance();
        transaction.add(mBinding.fragmentHolder.getId(), fragment, TAG_FIX);
        transaction.hide(fragment);
        fragment = SquareFragment.newInstance();
        transaction.add(mBinding.fragmentHolder.getId(), fragment,TAG_SQUARE);
        transaction.show(fragment);
        transaction.commit();

        mBinding.fragmentTabBottom.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Fragment fix = getSupportFragmentManager().findFragmentByTag(TAG_FIX);
                Fragment square = getSupportFragmentManager().findFragmentByTag(TAG_SQUARE);
                switch (i){
                    case R.id.fragment_tab_btn_fixed:
                        transaction.hide(square).show(fix);
                        break;
                    case R.id.fragment_tab_btn_square:
                        transaction.hide(fix).show(square);
                        break;
                }
                transaction.commit();
            }
        });


//        FFmpeg ffmpeg = FFmpeg.getInstance(this);
//        try {
//            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
//
//                @Override
//                public void onStart() {}
//
//                @Override
//                public void onFailure() {}
//
//                @Override
//                public void onSuccess() {}
//
//                @Override
//                public void onFinish() {}
//            });
//        } catch (FFmpegNotSupportedException e) {
//            // Handle if FFmpeg is not supported by device
//        }

        String url = "http://www.easydarwin.org/versions/easyplayer_pro/version.txt";

        update = new UpdateMgr(this);
        update.checkUpdate(url);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PERMISSION_GRANTED) {
                    update.doDownload();
                }
                break;
            case REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PERMISSION_GRANTED) {
                    mShouldOpenLocalFiles = true;
                }
            }
                break;
        }
    }
    public void onResume() {
        super.onResume();
        if (mShouldOpenLocalFiles)
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                    Fragment.instantiate(this, FileFragment.class.getName())).addToBackStack("root_path").commit();
        mShouldOpenLocalFiles = false;
    }

    public void onPause() {
        super.onPause();
    }

    public void onOpenLocalFiles(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                    Fragment.instantiate(this, FileFragment.class.getName())).addToBackStack("root_path").commit();
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    public static class LocalFileItemHolder extends RecyclerView.ViewHolder
    {

        public final TextView text1;
        public LocalFileItemHolder(View itemView) {
            super(itemView);
            text1 = (TextView) itemView;
        }


    }

    public static class FileFragment extends Fragment implements View.OnClickListener {
        public static final String KEY_IS_RECORD = "key_last_selection";
        private boolean mShowMp4File;
        private FragmentFileBinding mBinding;

        SparseArray<Boolean> mImageChecked;

        private String mSuffix;
        File mRoot = null;
        File[] mSubFiles;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(false);
            String root = getArguments() != null ? getArguments().getString("path"):null;
            mImageChecked = new SparseArray<>();
            mShowMp4File = true;
            mSuffix = mShowMp4File ? ".mp4" : ".jpg";
            if (TextUtils.isEmpty(root)) {
                mRoot = Environment.getExternalStorageDirectory();
            }else {
                mRoot = new File(root);
            }
            File[] subFiles = mRoot.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (mShowMp4File){
                        return pathname.isDirectory() || pathname.getPath().endsWith(".mp4")
                                || pathname.getPath().endsWith(".avi")
                                || pathname.getPath().endsWith(".wmv");
                    }
                    return pathname.isDirectory() || pathname.getPath().endsWith(mSuffix);
                }
            });
            if (subFiles == null) subFiles = new File[0];
            mSubFiles = subFiles;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_file, container, false);
            return mBinding.getRoot();
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mBinding.title.setText(mRoot.getName());
            mBinding.recycler.setLayoutManager(layoutManager);

            mBinding.recycler.setAdapter(new RecyclerView.Adapter() {
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    View inflate = LayoutInflater.from(getContext()).inflate(
                            android.R.layout.simple_list_item_1, parent, false);
                    inflate.setOnClickListener(FileFragment.this);
                    inflate.setBackgroundResource(android.R.drawable.list_selector_background);
                    return new LocalFileItemHolder(inflate);
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
                    LocalFileItemHolder holder = (LocalFileItemHolder) viewHolder;
                    holder.text1.setTag(R.id.click_tag, holder);
                    holder.text1.setText(mSubFiles[position].getName());
                }

                @Override
                public int getItemCount() {
                    return mSubFiles.length;
                }
            });

        }

        @Override
        public void onClick(View v) {
            LocalFileItemHolder holder = (LocalFileItemHolder) v.getTag(R.id.click_tag);
            if (holder.getAdapterPosition() == RecyclerView.NO_POSITION) {
                return;
            }
            File file = mSubFiles[holder.getAdapterPosition()];
            Bundle argu = new Bundle();
            if (file.isDirectory()) {
                argu.putString("path", file.getPath());
                getFragmentManager().beginTransaction().replace(android.R.id.content,
                        Fragment.instantiate(getContext(), FileFragment.class.getName(), argu)).addToBackStack("root_path").commit();
            }else{
                Intent i = new Intent(getActivity(), ProVideoActivity.class);
                i.putExtra("videoPath", file.getPath());
                startActivity(i);
            }
        }
    }
}
