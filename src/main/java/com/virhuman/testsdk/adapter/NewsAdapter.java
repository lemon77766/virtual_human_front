package com.virhuman.testsdk.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.model.NewsItem;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<NewsItem> newsList;
    private OnItemClickListener onItemClickListener;
    private OnFavoriteClickListener onFavoriteClickListener;


    public NewsAdapter(List<NewsItem> newsList) {
        this.newsList = newsList;
    }


    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = newsList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvSummary.setText(item.getSummary());
        holder.tvCategory.setText(item.getCategory());
        holder.tvTime.setText(item.getPublishTime());

        // 加载封面图片，如果没有图片则使用彩色方块背景
        String coverUrl = item.getCoverUrl();
        if (coverUrl != null && !coverUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(coverUrl)
                    .placeholder(R.drawable.bg_drama_cover)
                    .error(R.drawable.bg_drama_cover)
                    .centerCrop()
                    .into(holder.ivNewsCover);
        } else {
            // 如果没有图片，使用彩色方块背景
            holder.ivNewsCover.setImageResource(item.getBgColorResId());
        }

        // 根据收藏状态切换红心图标
        if (item.isFavorite()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart);
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });

        holder.btnLike.setOnClickListener(v -> {
            if (onFavoriteClickListener != null) {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION) {
                    onFavoriteClickListener.onFavoriteClick(item, adapterPos);
                }
            }
        });
    }



    @Override
    public int getItemCount() {
        return newsList == null ? 0 : newsList.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivNewsCover;
        TextView tvTitle;
        TextView tvSummary;
        TextView tvCategory;
        TextView tvTime;
        ImageButton btnLike;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivNewsCover = itemView.findViewById(R.id.iv_news_cover);
            tvTitle = itemView.findViewById(R.id.tv_news_title);
            tvSummary = itemView.findViewById(R.id.tv_news_summary);
            tvCategory = itemView.findViewById(R.id.tv_news_category);
            tvTime = itemView.findViewById(R.id.tv_news_time);
            btnLike = itemView.findViewById(R.id.btn_news_like);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(NewsItem item);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(NewsItem item, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.onFavoriteClickListener = listener;
    }
}


