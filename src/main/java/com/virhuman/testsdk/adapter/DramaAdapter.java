package com.virhuman.testsdk.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.model.DramaItem;
import java.util.ArrayList;
import java.util.List;


public class DramaAdapter extends RecyclerView.Adapter<DramaAdapter.DramaViewHolder> {

    private static final String TAG = "DramaAdapter";

    private List<DramaItem> dramaList;
    private OnPlayButtonClickListener playButtonClickListener;
    private OnLikeButtonClickListener likeButtonClickListener;
    private OnCommentButtonClickListener commentButtonClickListener;

    // DiffUtil callback for efficient updates
    private static final DiffUtil.ItemCallback<DramaItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<DramaItem>() {
        @Override
        public boolean areItemsTheSame(DramaItem oldItem, DramaItem newItem) {
            // Use ID for identity check
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(DramaItem oldItem, DramaItem newItem) {
            // Check if contents are the same
            return oldItem.equals(newItem);
        }
    };


    public DramaAdapter(List<DramaItem> dramaList) {
        this.dramaList = dramaList;
    }

    @Override
    public DramaViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drama, parent, false);
        return new DramaViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(DramaViewHolder holder, int position) {
        DramaItem dramaItem = dramaList.get(position);
        if (dramaItem != null) {
            Log.d(TAG, "onBindViewHolder position=" + position
                    + ", title=" + dramaItem.getTitle()
                    + ", coverUrl=" + dramaItem.getCoverUrl());
        } else {
            Log.w(TAG, "onBindViewHolder position=" + position + " with null DramaItem");
        }

        holder.tvDramaName.setText(dramaItem != null ? dramaItem.getTitle() : "");
        holder.tvArtist.setText(dramaItem != null ? dramaItem.getArtist() : "");
        holder.tvDescription.setText(dramaItem != null ? dramaItem.getDescription() : "");

        String coverUrl = dramaItem != null ? dramaItem.getCoverUrl() : null;
        if (coverUrl == null || coverUrl.isEmpty()) {
            Log.w(TAG, "Cover URL is empty for position=" + position
                    + ", title=" + (dramaItem != null ? dramaItem.getTitle() : "null"));
        }

        // 根据收藏状态切换红心图标
        if (dramaItem != null && dramaItem.isFavorite()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart);
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
        }

        // Set click listeners for buttons in onBindViewHolder to ensure correct position
        holder.btnPlay.setOnClickListener(v -> {
            if (playButtonClickListener != null) {
                playButtonClickListener.onPlayButtonClick(position);
            }
        });

        holder.btnLike.setOnClickListener(v -> {
            if (likeButtonClickListener != null) {
                likeButtonClickListener.onLikeButtonClick(position);
            }
        });

        holder.btnComment.setOnClickListener(v -> {
            if (commentButtonClickListener != null) {
                commentButtonClickListener.onCommentButtonClick(position);
            }
        });

        Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .placeholder(R.drawable.bg_drama_cover) // Optional placeholder
                .error(R.drawable.bg_drama_cover)       // Optional error image
                .into(holder.ivCover);
    }



    @Override
    public int getItemCount() {
        return dramaList.size();
    }

    public class DramaViewHolder extends RecyclerView.ViewHolder {
        public TextView tvDramaName;
        public TextView tvArtist;
        public TextView tvDescription;
        public ImageView ivCover;
        public ImageButton btnPlay;
        public ImageButton btnLike;
        public ImageButton btnComment;

        public DramaViewHolder(View view) {
            super(view);
            tvDramaName = view.findViewById(R.id.tv_drama_name);
            tvArtist = view.findViewById(R.id.tv_drama_category); // Reusing this ID for artist
            tvDescription = view.findViewById(R.id.tv_performer); // Reusing this ID for description
            ivCover = view.findViewById(R.id.drama_cover);
            btnPlay = view.findViewById(R.id.btn_play);
            btnLike = view.findViewById(R.id.btn_like);
            btnComment = view.findViewById(R.id.btn_comment);

            // 调试日志
            if (btnComment == null) {
                android.util.Log.e("DramaAdapter", "评论按钮未找到!");
            } else {
                android.util.Log.d("DramaAdapter", "评论按钮初始化成功");
            }
        }
    }

    public interface OnPlayButtonClickListener {
        void onPlayButtonClick(int position);
    }

    public interface OnLikeButtonClickListener {
        void onLikeButtonClick(int position);
    }

    public interface OnCommentButtonClickListener {
        void onCommentButtonClick(int position);
    }

    public void setOnPlayButtonClickListener(OnPlayButtonClickListener listener) {
        this.playButtonClickListener = listener;
    }

    public void setOnLikeButtonClickListener(OnLikeButtonClickListener listener) {
        this.likeButtonClickListener = listener;
    }

    public void setOnCommentButtonClickListener(OnCommentButtonClickListener listener) {
        this.commentButtonClickListener = listener;
    }

    /**
     * 高效更新数据列表，使用DiffUtil进行增量更新
     * @param newList 新的数据列表
     */
    public void updateDataList(List<DramaItem> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DramaDiffCallback(this.dramaList, newList));
        this.dramaList = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    /**
     * DiffUtil回调类，用于计算差异
     */
    private static class DramaDiffCallback extends DiffUtil.Callback {
        private final List<DramaItem> oldList;
        private final List<DramaItem> newList;

        DramaDiffCallback(List<DramaItem> oldList, List<DramaItem> newList) {
            this.oldList = oldList != null ? oldList : new ArrayList<>();
            this.newList = newList != null ? newList : new ArrayList<>();
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            DramaItem oldItem = oldList.get(oldItemPosition);
            DramaItem newItem = newList.get(newItemPosition);
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DramaItem oldItem = oldList.get(oldItemPosition);
            DramaItem newItem = newList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }
}
