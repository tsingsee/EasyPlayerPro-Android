package org.easydarwin.easyplayer.fragments;


import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;

import org.easydarwin.easyplayer.PlaylistActivity;
import org.easydarwin.easyplayer.ProVideoActivity;
import org.easydarwin.easyplayer.TheApp;
import org.easydarwin.easyplayer.data.VideoSource;
import org.esaydarwin.rtsp.player.R;
import org.esaydarwin.rtsp.player.databinding.ContentPlaylistBinding;
import org.esaydarwin.rtsp.player.databinding.FixedFragmentBinding;
import org.esaydarwin.rtsp.player.databinding.VideoSourceItemBinding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FixedListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FixedListFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {


    private static final String TAG = "TAG";
    private RecyclerView mRecyclerView;
    private int mPos;
    private FixedFragmentBinding mBinding;
    private Cursor mCursor;
    public static FixedListFragment newInstance() {
        FixedListFragment fragment = new FixedListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fixed_fragment, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);

        mRecyclerView = mBinding.recycler;
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new PlayListViewHolder((VideoSourceItemBinding) DataBindingUtil.inflate(getLayoutInflater(null), R.layout.video_source_item, parent, false));
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
                File file = url2localPosterFile(getContext(), url);
                Glide.with(FixedListFragment.this).load(file).signature(new StringSignature(UUID.randomUUID().toString())).placeholder(R.drawable.placeholder).centerCrop().into(plvh.mImageView);

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


        mBinding.pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doLoadData(true);
            }
        });
        doLoadData(false);
    }



    private void showOrHideEmptyView() {
        if (mCursor.getCount() > 0) {
            mBinding.emptyView.setVisibility(View.GONE);
        } else {
            mBinding.emptyView.setVisibility(View.VISIBLE);
        }
    }

    class PlayListViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTextView;
        private final TextView mAudienceNumber;
        private final ImageView mImageView;

        public PlayListViewHolder(VideoSourceItemBinding binding) {
            super(binding.getRoot());
            mTextView = binding.videoSourceItemName;
            mAudienceNumber = binding.videoSourceItemAudienceNumber;
            mImageView = binding.videoSourceItemThumb;
            itemView.setOnClickListener(FixedListFragment.this);
            itemView.setOnLongClickListener(FixedListFragment.this);
            itemView.setTag(this);
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
                Intent i = new Intent(getActivity(), ProVideoActivity.class);
                i.putExtra("videoPath", playUrl);
                startActivity(i);
                return;
            }
        }
    }


    @Override
    public boolean onLongClick(View view) {
        PlayListViewHolder holder = (PlayListViewHolder) view.getTag();
        final int pos = holder.getAdapterPosition();
        if (pos != -1) {

            new AlertDialog.Builder(getActivity()).setItems(new CharSequence[]{"修改", "删除"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == 0) {
                        mCursor.moveToPosition(pos);
                        String playUrl = mCursor.getString(mCursor.getColumnIndex(VideoSource.URL));
                        final int _id = mCursor.getInt(mCursor.getColumnIndex(VideoSource._ID));
                        final EditText edit = new EditText(getActivity());
                        edit.setHint("RTSP/RTMP/HLS地址");
                        final int hori = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
                        final int verti = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
                        edit.setPadding(hori, verti, hori, verti);
                        edit.setText(playUrl);
                        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).setView(edit).setTitle("请输入播放地址").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String mRTSPUrl = String.valueOf(edit.getText());
                                if (TextUtils.isEmpty(mRTSPUrl)) {
                                    return;
                                }
                                ContentValues cv = new ContentValues();
                                cv.put(VideoSource.URL, mRTSPUrl);
                                TheApp.sDB.update(VideoSource.TABLE_NAME, cv, VideoSource._ID + "=?", new String[]{String.valueOf(_id)});

                                mCursor.close();
                                mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
                                mRecyclerView.getAdapter().notifyItemChanged(pos);
                            }
                        }).setNegativeButton("取消", null).create();
                        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialogInterface) {
                                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
                            }
                        });
                        alertDialog.show();
                    } else {
                        new AlertDialog.Builder(getActivity()).setMessage("确定要删除该地址吗？").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mCursor.moveToPosition(pos);
                                TheApp.sDB.delete(VideoSource.TABLE_NAME, VideoSource._ID + "=?", new String[]{String.valueOf(mCursor.getInt(mCursor.getColumnIndex(VideoSource._ID)))});
                                mCursor.close();
                                mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
                                mRecyclerView.getAdapter().notifyItemRemoved(pos);
                                showOrHideEmptyView();
                            }
                        }).setNegativeButton("取消", null).show();
                    }
                }
            }).show();
        }
        return true;
    }



    private void doLoadData(final boolean refresh) {
        new AsyncTask<Void, Void, Cursor>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (refresh) {
                    mBinding.pullToRefresh.setRefreshing(true);
                }
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                super.onPostExecute(cursor);
                mCursor.close();
                mCursor = cursor;
                showOrHideEmptyView();
                mRecyclerView.getAdapter().notifyDataSetChanged();
                if (refresh) {
                    mBinding.pullToRefresh.setRefreshing(false);
                }
            }

            @Override
            protected Cursor doInBackground(Void... voids) {
                try {
                    String ip = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_ip), TheApp.DEFAULT_SERVER_IP);
                    int port = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_port), "10008"));

                    OkHttpClient client = new OkHttpClient();

                    Request request = new Request.Builder()
                            .url(String.format("http://%s:%d/api/v1/getrtsplivesessions", ip, port))
                            .build();

                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        /**
                         * {
                         "EasyDarwin" : {
                         "Body" : {
                         "SessionCount" : "1",
                         "Sessions" : [
                         {
                         "AudienceNum" : 1,
                         "index" : 0,
                         "name" : "9",
                         "url" : "rtsp://121.40.50.44:554/9.sdp"
                         }
                         ]
                         },
                         "Header" : {
                         "CSeq" : "1",
                         "MessageType" : "MSG_SC_RTSP_PUSH_SESSION_LIST_ACK",
                         "Version" : "1.0"
                         }
                         }
                         }
                         */

                        TheApp.sDB.delete(VideoSource.TABLE_NAME, VideoSource.INDEX + "!=?", new String[]{String.valueOf(-1)});
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray array = json.getJSONObject("EasyDarwin").getJSONObject("Body").getJSONArray("Sessions");

//                    "AudienceNum" : 1,
//                            "index" : 0,
//                            "name" : "9",
//                            "url" : "rtsp://121.40.50.44:554/9.sdp"

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject item = array.getJSONObject(i);
                            ContentValues cv = new ContentValues();
                            cv.put(VideoSource.INDEX, item.optInt("index"));
                            cv.put(VideoSource.URL, item.optString("url"));
                            cv.put(VideoSource.NAME, item.optString("name"));
                            cv.put(VideoSource.AUDIENCE_NUMBER, item.optInt("AudienceNum"));
//                            TheApp.sDB.replace(VideoSource.TABLE_NAME, null, cv);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
            }
        }.execute();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mRecyclerView.getAdapter().notifyItemChanged(mPos);
    }

    @Override
    public void onDestroy() {
        mCursor.close();
        super.onDestroy();
    }


    public static File url2localPosterFile(Context context, String url) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            byte[] result = messageDigest.digest(url.getBytes());
            return new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), Base64.encodeToString(result, Base64.NO_WRAP | Base64.URL_SAFE));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }


    public void addUrl(String mRTSPUrl){

        ContentValues cv = new ContentValues();
        cv.put(VideoSource.URL, mRTSPUrl);
        TheApp.sDB.insert(VideoSource.TABLE_NAME, null, cv);

        mCursor.close();
        mCursor = TheApp.sDB.query(VideoSource.TABLE_NAME, null, null, null, null, null, null);
        mRecyclerView.getAdapter().notifyItemInserted(mCursor.getCount() - 1);
        showOrHideEmptyView();
    }


}
