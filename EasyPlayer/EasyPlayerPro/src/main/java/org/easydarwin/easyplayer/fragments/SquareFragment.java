package org.easydarwin.easyplayer.fragments;


import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import org.easydarwin.easyplayer.ProVideoActivity;
import org.easydarwin.easyplayer.TheApp;
import org.easydarwin.easyplayer.data.VideoSource;
import org.esaydarwin.rtsp.player.R;
import org.esaydarwin.rtsp.player.databinding.FragmentSquareBinding;
import org.esaydarwin.rtsp.player.databinding.SquareItemBinding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class SquareFragment extends Fragment implements View.OnClickListener {


    private FragmentSquareBinding binding;
    private AsyncTask<Void, Integer, Integer> task;
    private int colmnCount;

    public SquareFragment() {
        // Required empty public constructor
    }


    JSONArray items;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_square, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int itemWidth = getResources().getDimensionPixelSize(R.dimen.square_item_width);
        colmnCount = screenWidth/itemWidth;

        binding.squareRecycler.setLayoutManager(new GridLayoutManager(getContext(), colmnCount));
        binding.squareRecycler.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                SquareItemBinding binding = DataBindingUtil.inflate(getLayoutInflater(null), R.layout.square_item, parent, false);

                binding.getRoot().getLayoutParams().height = (int) (parent.getWidth() / colmnCount * 0.8);
                binding.getRoot().setOnClickListener(SquareFragment.this);
                return new SquareItemViewHolder(binding);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                SquareItemViewHolder viewHolder = (SquareItemViewHolder) holder;
                try {
                    JSONObject item = items.getJSONObject(position);
                    Glide.with(SquareFragment.this).load(item.getString("SnapURL")).placeholder(R.drawable.placeholder).into(viewHolder.mBinding.squarePhoto);
                    viewHolder.mBinding.squareTitle.setText(item.optString("Name"));
                } catch (JSONException e) {
                    e.printStackTrace();

                    Glide.with(SquareFragment.this).load(R.drawable.snap).into(viewHolder.mBinding.squarePhoto);
                    viewHolder.mBinding.squareTitle.setText(null);
                }

            }

            @Override
            public int getItemCount() {
                return items != null ? items.length():0;
            }
        });
        task = new AsyncTask<Void, Integer, Integer>() {

            JSONArray json;

            @Override
            protected Integer doInBackground(Void... voids) {
                String ip = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_ip), "114.55.107.180");
                int port = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.key_port), "10008"));

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("http://www.easydarwin.org/public/json/squarelist.json")
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        String string = response.body().string();
                        json = new JSONObject(string).getJSONObject("EasyDarwin").getJSONObject("Body").getJSONArray("Lives");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return 0;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                binding.empty.setVisibility(View.GONE);
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                if (json == null || json.length() == 0) {
                    binding.empty.setVisibility(View.VISIBLE);
                    binding.squareRecycler.setVisibility(View.GONE);
                }else{
                    binding.empty.setVisibility(View.GONE);
                    binding.squareRecycler.setVisibility(View.VISIBLE);
                }
                items = json;
                binding.squareRecycler.getAdapter().notifyDataSetChanged();
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }
        }.execute();


    }

    @Override
    public void onClick(View view) {
        SquareItemViewHolder holder = (SquareItemViewHolder) view.getTag();
        int pos = holder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION)
            return;

        JSONObject object = items.optJSONObject(pos);
        if (object == null) return;
        String playUrl = object.optString("PlayUrl");
        String snapUrl = object.optString("SnapURL");
        if (!TextUtils.isEmpty(playUrl)) {

//            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), holder.mBinding.squarePhoto, "snapCover").toBundle();
//            Intent i = new Intent(getActivity(), ProVideoActivity.class);
//            i.putExtra("videoPath", playUrl);
//            i.putExtra("snapPath", snapUrl);
//            ActivityCompat.startActivity(getActivity(), i, bundle);

            Intent i = new Intent(getActivity(), ProVideoActivity.class);
            i.putExtra("videoPath", playUrl);
            startActivity(i);
            return;
        }
    }

    public static class SquareItemViewHolder extends RecyclerView.ViewHolder{

        private final SquareItemBinding mBinding;

        public SquareItemViewHolder(SquareItemBinding binding) {
            super(binding.getRoot());

            binding.getRoot().setTag(this);
            mBinding = binding;
        }
    }


    public static SquareFragment newInstance() {
        return new SquareFragment();
    }


    @Override
    public void onDestroy() {
        if (task != null) task.cancel(true);
        super.onDestroy();
    }
}
