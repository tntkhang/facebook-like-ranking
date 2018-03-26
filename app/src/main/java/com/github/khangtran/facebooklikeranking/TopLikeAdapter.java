package com.github.khangtran.facebooklikeranking;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import khangtran.preferenceshelper.PreferencesHelper;

/**
 * Created by KhangTran on 3/26/18.
 */

public class TopLikeAdapter  extends RecyclerView.Adapter<TopLikeAdapter.MyViewHolder> {

    private List<Photo> photoList;
    private Context context;
    private List<Photo> topLikeLast;


    public TopLikeAdapter(Context context, List<Photo> list, List<Photo> topLikeLast) {
        this.photoList = list;
        this.context = context;
        this.topLikeLast = topLikeLast;
    }

    public void updateLastLike(List<Photo> data) {
        topLikeLast.clear();
        topLikeLast.addAll(data);
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView likeCount, name, likeCountLastUpdate;

        public MyViewHolder(View view) {
            super(view);
            likeCount = (TextView) view.findViewById(R.id.tv_like_count);
            likeCountLastUpdate = (TextView) view.findViewById(R.id.tv_like_count_last_update);
            name = (TextView) view.findViewById(R.id.name);
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        final Photo photo = photoList.get(position);
        holder.likeCount.setText("Like: " + photo.getTotalCount());
        holder.likeCountLastUpdate.setText(" - " + getLikeCountLastUpdate(photo.getId()));
        holder.name.setText(photo.getName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/" + photo.getId()));
                context.startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoList.size();
    }

    private int getLikeCountLastUpdate(String photoId) {
        for (Photo photo : topLikeLast) {
            if (photo.getId().equals(photoId)) {
                return photo.getTotalCount();
            }
        }
        return 0;
    }
}