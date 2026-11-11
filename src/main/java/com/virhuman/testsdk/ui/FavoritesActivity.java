package com.virhuman.testsdk.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.model.DramaItem;
import com.virhuman.testsdk.model.NewsItem;
import com.virhuman.testsdk.net.ArticleApi;
import com.virhuman.testsdk.net.OperaApi;
import com.virhuman.testsdk.net.UserApi;
import com.virhuman.testsdk.utils.AuthStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

/**
 * 我的收藏页面：从 /api/user/{userId}/favorites 读取收藏记录，并根据类型拼接戏曲/音乐/资讯详情展示。
 */
public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View emptyView;
    private ProgressBar progressBar;
    private ImageButton btnBack;
    private FavoritesAdapter adapter;

    private final List<UserApi.FavoritesResponse.FavoriteRecord> favoriteRecords = new ArrayList<>();
    private final List<FavoriteDisplayItem> displayItems = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerView = findViewById(R.id.recycler_favorites);
        emptyView = findViewById(R.id.view_empty);
        progressBar = findViewById(R.id.progress_loading);
        btnBack = findViewById(R.id.btn_back);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritesAdapter(displayItems);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadFavorites();
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void updateEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(displayItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void loadFavorites() {
        int userId = AuthStore.getUserId(this);
        if (userId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        UserApi.getUserFavorites(this, userId, new UserApi.FavoritesCallback() {
            @Override
            public void onSuccess(UserApi.FavoritesResponse response) {
                runOnUiThread(() -> {
                    favoriteRecords.clear();
                    if (response != null && response.data != null) {
                        favoriteRecords.addAll(response.data);
                    }
                    if (favoriteRecords.isEmpty()) {
                        displayItems.clear();
                        adapter.notifyDataSetChanged();
                        showLoading(false);
                        updateEmptyView();
                    } else {
                        loadDetailLists();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage, Throwable t) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(FavoritesActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    updateEmptyView();
                });
            }
        });
    }

    /**
     * 依次请求戏曲列表、音乐列表、资讯列表，用来补全收藏记录的标题和图片等信息。
     */
    private void loadDetailLists() {
        // 先拉取戏曲列表
        OperaApi.fetchOperaList(this, new OperaApi.OperaListCallback() {
            @Override
            public void onSuccess(List<DramaItem> operas) {
                // 再拉取音乐列表
                OperaApi.fetchMusicList(FavoritesActivity.this, new OperaApi.OperaListCallback() {
                    @Override
                    public void onSuccess(List<DramaItem> musics) {
                        // 最后拉取资讯列表
                        ArticleApi.fetchArticleList(FavoritesActivity.this, new ArticleApi.ArticleListCallback() {
                            @Override
                            public void onSuccess(List<NewsItem> articles) {
                                runOnUiThread(() -> buildDisplayItems(operas, musics, articles));
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                runOnUiThread(() -> buildDisplayItems(operas, musics, null));
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // 只用戏曲和资讯
                        ArticleApi.fetchArticleList(FavoritesActivity.this, new ArticleApi.ArticleListCallback() {
                            @Override
                            public void onSuccess(List<NewsItem> articles) {
                                runOnUiThread(() -> buildDisplayItems(operas, null, articles));
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                runOnUiThread(() -> buildDisplayItems(operas, null, null));
                            }
                        });
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                // 如果戏曲列表也失败了，就只展示基础信息
                ArticleApi.fetchArticleList(FavoritesActivity.this, new ArticleApi.ArticleListCallback() {
                    @Override
                    public void onSuccess(List<NewsItem> articles) {
                        runOnUiThread(() -> buildDisplayItems(null, null, articles));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        runOnUiThread(() -> buildDisplayItems(null, null, null));
                    }
                });
            }
        });
    }

    private void buildDisplayItems(@Nullable List<DramaItem> operas,
                                   @Nullable List<DramaItem> musics,
                                   @Nullable List<NewsItem> articles) {
        Map<Integer, DramaItem> operaMap = new HashMap<>();
        Map<Integer, DramaItem> musicMap = new HashMap<>();
        Map<Integer, NewsItem> articleMap = new HashMap<>();

        if (operas != null) {
            for (DramaItem item : operas) {
                operaMap.put(item.getId(), item);
            }
        }
        if (musics != null) {
            for (DramaItem item : musics) {
                musicMap.put(item.getId(), item);
            }
        }
        if (articles != null) {
            for (NewsItem item : articles) {
                articleMap.put(item.getId(), item);
            }
        }

        displayItems.clear();

        for (UserApi.FavoritesResponse.FavoriteRecord record : favoriteRecords) {
            if (record == null) continue;
            FavoriteDisplayItem item = new FavoriteDisplayItem();
            item.targetId = record.targetId;
            item.targetType = record.targetType;
            item.createdAt = record.createdAt;

            String type = record.targetType != null ? record.targetType.toLowerCase(Locale.ROOT) : "";
            switch (type) {
                case "opera":
                    item.typeLabel = "戏曲";
                    DramaItem opera = operaMap.get(record.targetId);
                    if (opera != null) {
                        item.title = opera.getTitle();
                        item.subTitle = opera.getArtist();
                        item.coverUrl = opera.getCoverUrl();
                    } else {
                        item.title = "戏曲 ID " + record.targetId;
                    }
                    break;
                case "music":
                    item.typeLabel = "音乐";
                    DramaItem music = musicMap.get(record.targetId);
                    if (music != null) {
                        item.title = music.getTitle();
                        item.subTitle = music.getArtist();
                        item.coverUrl = music.getCoverUrl();
                    } else {
                        item.title = "音乐 ID " + record.targetId;
                    }
                    break;
                case "article":
                    item.typeLabel = "资讯";
                    NewsItem article = articleMap.get(record.targetId);
                    if (article != null) {
                        item.title = article.getTitle();
                        item.subTitle = article.getSummary();
                        item.coverUrl = article.getCoverUrl();
                    } else {
                        item.title = "资讯 ID " + record.targetId;
                    }
                    break;
                default:
                    item.typeLabel = "其他";
                    item.title = (record.targetType != null ? record.targetType : "未知") + " ID " + record.targetId;
                    break;
            }

            displayItems.add(item);
        }

        // 按创建时间倒序排列（ISO 字符串直接比较即可）
        Collections.sort(displayItems, new Comparator<FavoriteDisplayItem>() {
            @Override
            public int compare(FavoriteDisplayItem o1, FavoriteDisplayItem o2) {
                if (o1.createdAt == null && o2.createdAt == null) return 0;
                if (o1.createdAt == null) return 1;
                if (o2.createdAt == null) return -1;
                return o2.createdAt.compareTo(o1.createdAt);
            }
        });

        adapter.notifyDataSetChanged();
        showLoading(false);
        updateEmptyView();
    }

    private static class FavoriteDisplayItem {
        String targetType;
        int targetId;
        String typeLabel;
        String title;
        String subTitle;
        String coverUrl;
        String createdAt;
        boolean isFavorite = true; // 在收藏页面中，所有项目默认都是已收藏状态
    }

    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavVH> {

        private final List<FavoriteDisplayItem> items;

        FavoritesAdapter(List<FavoriteDisplayItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public FavVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_favorite, parent, false);
            return new FavVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FavVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        private void onFavoriteClick(FavoriteDisplayItem item, int position) {
            if (item == null) return;
            
            int userId = AuthStore.getUserId(FavoritesActivity.this);
            if (userId <= 0) {
                Toast.makeText(FavoritesActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            // 取消收藏
            String type = item.targetType != null ? item.targetType.toLowerCase(Locale.ROOT) : "";
            
            switch (type) {
                case "opera":
                    OperaApi.unfavoriteOpera(FavoritesActivity.this, item.targetId, userId, new OperaApi.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                // 从列表中移除该项
                                items.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, items.size());
                                // 同时从favoriteRecords中移除
                                favoriteRecords.removeIf(record -> record.targetId == item.targetId && "opera".equals(record.targetType));
                                Toast.makeText(FavoritesActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                                updateEmptyView();
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(FavoritesActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
                        }
                    });
                    break;
                case "music":
                    OperaApi.unfavoriteMusic(FavoritesActivity.this, item.targetId, userId, new OperaApi.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                items.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, items.size());
                                favoriteRecords.removeIf(record -> record.targetId == item.targetId && "music".equals(record.targetType));
                                Toast.makeText(FavoritesActivity.this, "已取消收藏音乐", Toast.LENGTH_SHORT).show();
                                updateEmptyView();
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(FavoritesActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
                        }
                    });
                    break;
                case "article":
                    ArticleApi.unfavoriteArticle(FavoritesActivity.this, item.targetId, userId, new ArticleApi.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                items.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, items.size());
                                favoriteRecords.removeIf(record -> record.targetId == item.targetId && "article".equals(record.targetType));
                                Toast.makeText(FavoritesActivity.this, "已取消收藏资讯", Toast.LENGTH_SHORT).show();
                                updateEmptyView();
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            runOnUiThread(() -> Toast.makeText(FavoritesActivity.this, errorMessage, Toast.LENGTH_SHORT).show());
                        }
                    });
                    break;
                default:
                    Toast.makeText(FavoritesActivity.this, "未知类型，无法取消收藏", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

        class FavVH extends RecyclerView.ViewHolder {
            TextView tvType;
            TextView tvTitle;
            TextView tvSubTitle;
            TextView tvTime;
            ImageView ivCover;
            ImageView ivFavorite;

            FavVH(@NonNull View itemView) {
                super(itemView);
                tvType = itemView.findViewById(R.id.tv_fav_type);
                tvTitle = itemView.findViewById(R.id.tv_fav_title);
                tvSubTitle = itemView.findViewById(R.id.tv_fav_subtitle);
                tvTime = itemView.findViewById(R.id.tv_fav_time);
                ivCover = itemView.findViewById(R.id.iv_fav_cover);
                ivFavorite = itemView.findViewById(R.id.iv_fav_favorite);
            }

            void bind(FavoriteDisplayItem item) {
                tvType.setText(item.typeLabel != null ? item.typeLabel : "");
                tvTitle.setText(item.title != null ? item.title : "");
                tvSubTitle.setText(item.subTitle != null ? item.subTitle : "");
                tvTime.setText(item.createdAt != null ? item.createdAt.replace("T", " ") : "");

                if (ivCover != null) {
                    if (item.coverUrl != null && !item.coverUrl.trim().isEmpty()) {
                        Glide.with(ivCover.getContext())
                                .load(item.coverUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_launcher_background)
                                .centerCrop()
                                .into(ivCover);
                    } else {
                        ivCover.setImageResource(R.drawable.ic_launcher_background);
                    }
                }

                // 设置收藏图标状态
                if (ivFavorite != null) {
                    ivFavorite.setImageResource(item.isFavorite ? R.drawable.ic_heart : R.drawable.ic_heart_outline);
                    ivFavorite.setOnClickListener(v -> {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            onFavoriteClick(items.get(position), position);
                        }
                    });
                }
            }
        }
    }
}
