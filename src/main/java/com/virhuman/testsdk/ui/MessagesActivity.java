package com.virhuman.testsdk.ui;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Rect;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.virhuman.testsdk.R;
import com.bumptech.glide.Glide;
import com.virhuman.testsdk.adapter.CarouselAdapter;
import com.virhuman.testsdk.adapter.DramaAdapter;
import com.virhuman.testsdk.adapter.NewsAdapter;
import com.virhuman.testsdk.model.DramaItem;
import com.virhuman.testsdk.model.NewsItem;
import com.virhuman.testsdk.net.OperaApi;
import com.virhuman.testsdk.net.ArticleApi;
import com.virhuman.testsdk.net.UserApi;
import com.virhuman.testsdk.utils.AuthStore;
import com.virhuman.testsdk.utils.CommentOverlayManager;


import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


public class MessagesActivity extends AppCompatActivity {
    private static final String TAG = "MessagesActivity";

    private View navHome, navFeatures, navMessages, navProfile;

    // Tab related
    private LinearLayout tabOpera, tabMusic, tabNews;
    private ImageView ivOpera, ivMusic, ivNews;
    private TextView tvOpera, tvMusic, tvNews;

    // RecyclerView related
    private RecyclerView recyclerView;
    private DramaAdapter dramaAdapter;
    private List<DramaItem> dramaList;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsList;

    // Carousel related
    private View carouselContainer;
    private ViewPager2 carouselViewPager;
    private LinearLayout carouselIndicator;

    private CarouselAdapter carouselAdapter;
    private List<DramaItem> carouselDramaItems;
    private Handler carouselHandler;
    private Runnable carouselRunnable;
    private static final long CAROUSEL_INTERVAL_MS = 4000L; // 增加间隔减少性能消耗

    // Player related
    private TextView playerTitle, playerCategory;
    private ImageView btnPlayPause;
    private SeekBar seekBar;
    private boolean isPlaying = false;
    private MediaPlayer mediaPlayer;
    // 当前是否处于资讯模式，用于控制播放器行为
    private boolean isNewsMode = false;
    // 当前选中的内容 Tab：opera / music / news
    private String currentTab = "opera";

    // 收藏状态缓存，用于在切换页面/Tab 后保持红心状态
    private final java.util.HashSet<Integer> favoriteOperaIds = new java.util.HashSet<>();
    private final java.util.HashSet<Integer> favoriteMusicIds = new java.util.HashSet<>();
    private final java.util.HashSet<Integer> favoriteArticleIds = new java.util.HashSet<>();



    // Comment input related (similar to HomeActivity's audio interaction)
    private ViewGroup commentOverlayContainer;
    private EditText commentInputText;
    private ImageButton commentSendButton;
    private ImageButton commentCloseButton;
    private int currentCommentPosition = -1;

    // Keyboard handling for comment input
    private View rootViewChild;
    private boolean keyboardVisible = false;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private ViewGroup commentRootForTouch;
    private CommentOverlayManager commentManager;

    private final ViewPager2.OnPageChangeCallback carouselPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            updateIndicator(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
            if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                stopCarouselAutoPlay();
            } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                startCarouselAutoPlay();
            }
        }
    };

    // 进度条更新相关
    private Handler progressHandler;
    private Runnable progressRunnable;
    private static final int PROGRESS_UPDATE_INTERVAL = 1000; // 1秒更新一次

    private final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser && mediaPlayer != null) {
                mediaPlayer.seekTo(progress); // progress是实际的毫秒值，因为seekBar.setMax设置为总时长
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // 用户开始拖动进度条时停止自动更新
            if (progressHandler != null && progressRunnable != null) {
                progressHandler.removeCallbacks(progressRunnable);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 用户停止拖动进度条后恢复自动更新
            startProgressUpdate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure layout resizes when keyboard appears so overlays anchored to bottom move above keyboard
        try {
            getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } catch (Exception ignored) {}
        setContentView(R.layout.activity_messages);

        initViews();
        commentManager = new CommentOverlayManager(this);
        initData();
        setListeners();
        setupKeyboardListener();

        setNavSelected(navMessages, true);
        selectTab(tabOpera);
    }

    private void initViews() {
        navHome = findViewById(R.id.nav_home);
        navFeatures = findViewById(R.id.nav_features);
        navMessages = findViewById(R.id.nav_messages);
        navProfile = findViewById(R.id.nav_profile);

        tabOpera = findViewById(R.id.tab_opera);
        tabMusic = findViewById(R.id.tab_music);
        tabNews = findViewById(R.id.tab_news);
        ivOpera = tabOpera.findViewById(R.id.ic_opera_tab);
        ivMusic = tabMusic.findViewById(R.id.ic_music_tab);
        ivNews = tabNews.findViewById(R.id.ic_news_tab);
        tvOpera = tabOpera.findViewById(R.id.tv_opera);
        tvMusic = tabMusic.findViewById(R.id.tv_music);
        tvNews = tabNews.findViewById(R.id.tv_news);

        carouselContainer = findViewById(R.id.carousel);
        carouselViewPager = findViewById(R.id.carousel_viewpager);
        carouselIndicator = findViewById(R.id.carousel_indicator);


        recyclerView = findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // RecyclerView优化配置
        recyclerView.setHasFixedSize(true); // 固定大小，提升性能
        recyclerView.setItemViewCacheSize(20); // 增加缓存大小
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // 添加滚动监听器，滚动时暂停Glide图片加载
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 停止滚动时恢复图片加载
                    Glide.with(recyclerView.getContext()).resumeRequests();
                } else {
                    // 滚动时暂停图片加载
                    Glide.with(recyclerView.getContext()).pauseRequests();
                }
            }
        });

        playerTitle = findViewById(R.id.player_title);
        playerCategory = findViewById(R.id.player_category);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        seekBar = findViewById(R.id.seek_bar);
    }

    private void initData() {
        // 戏曲/音乐列表
        dramaList = new ArrayList<>();
        dramaAdapter = new DramaAdapter(dramaList);

        // 资讯列表
        newsList = new ArrayList<>();
        newsAdapter = new NewsAdapter(newsList);
        // 点击资讯卡片时，直接更新底部播放器并播放对应音频
        newsAdapter.setOnItemClickListener(this::playNewsItem);
        // 点击资讯卡片右侧红心收藏/取消收藏
        newsAdapter.setOnFavoriteClickListener(this::onNewsLikeClicked);

        // 默认先显示戏曲列表
        recyclerView.setAdapter(dramaAdapter);

        dramaAdapter.setOnPlayButtonClickListener(this::playDrama);
        dramaAdapter.setOnLikeButtonClickListener(this::onDramaLikeClicked);

        setupCarousel();
        initMediaPlayer();
    }

    /**
     * 从后端加载当前用户的收藏列表，用于在进入页面/切换回来时还原红心状态
     */
    private void loadFavoritesState() {
        int userId = AuthStore.getUserId(this);
        if (userId <= 0) {
            return;
        }

        UserApi.getUserFavorites(this, userId, new UserApi.FavoritesCallback() {
            @Override
            public void onSuccess(UserApi.FavoritesResponse response) {
                runOnUiThread(() -> {
                    favoriteOperaIds.clear();
                    favoriteMusicIds.clear();
                    favoriteArticleIds.clear();
                    if (response != null && response.data != null) {
                        for (UserApi.FavoritesResponse.FavoriteRecord record : response.data) {
                            if (record == null || record.targetType == null) continue;
                            String type = record.targetType.toLowerCase(java.util.Locale.ROOT);
                            switch (type) {
                                case "opera":
                                    favoriteOperaIds.add(record.targetId);
                                    break;
                                case "music":
                                    favoriteMusicIds.add(record.targetId);
                                    break;
                                case "article":
                                    favoriteArticleIds.add(record.targetId);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    // 根据最新收藏状态刷新当前列表
                    if ("opera".equals(currentTab)) {
                        applyFavoritesToDramaList(dramaList, false);
                        dramaAdapter.notifyDataSetChanged();
                    } else if ("music".equals(currentTab)) {
                        applyFavoritesToDramaList(dramaList, true);
                        dramaAdapter.notifyDataSetChanged();
                    } else if ("news".equals(currentTab)) {
                        applyFavoritesToNewsList(newsList);
                        newsAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage, Throwable t) {
                Log.e(TAG, "loadFavoritesState failed: " + errorMessage, t);
            }
        });
    }

    /**
     * 根据缓存的收藏 ID 集，更新戏曲/音乐列表中的 favorite 标记
     */
    private void applyFavoritesToDramaList(List<DramaItem> list, boolean isMusic) {
        if (list == null || list.isEmpty()) return;
        for (DramaItem item : list) {
            if (item == null) continue;
            if (isMusic) {
                item.setFavorite(favoriteMusicIds.contains(item.getId()));
            } else {
                item.setFavorite(favoriteOperaIds.contains(item.getId()));
            }
        }
    }

    /**
     * 根据缓存的收藏 ID 集，更新资讯列表中的 favorite 标记
     */
    private void applyFavoritesToNewsList(List<NewsItem> list) {
        if (list == null || list.isEmpty()) return;
        for (NewsItem item : list) {
            if (item == null) continue;
            item.setFavorite(favoriteArticleIds.contains(item.getId()));
        }
    }


    private void initMediaPlayer() {

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        mediaPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            btnPlayPause.setImageResource(R.drawable.ic_play);
            // 停止进度条更新
            stopProgressUpdate();
        });
        
        // 设置进度条监听器
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
    }
    
    private void startProgressUpdate() {
        if (progressHandler == null) {
            progressHandler = new Handler(Looper.getMainLooper());
        }
        if (progressRunnable == null) {
            progressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int totalDuration = mediaPlayer.getDuration();
                        
                        if (totalDuration > 0) {
                            int progress = (int) (((float) currentPosition / totalDuration) * 100);
                            seekBar.setMax(totalDuration);
                            seekBar.setProgress(currentPosition);
                        }
                    }
                    // 每秒更新一次
                    progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL);
                }
            };
        }
        progressHandler.post(progressRunnable);
    }
    
    private void stopProgressUpdate() {
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    private void setListeners() {
        navHome.setOnClickListener(v -> navigateTo(HomeActivity.class));
        navFeatures.setOnClickListener(v -> navigateTo(FeaturesActivity.class));
        navMessages.setOnClickListener(v -> setNavSelected(navMessages, true));
        navProfile.setOnClickListener(v -> navigateTo(ProfileActivity.class));

        tabOpera.setOnClickListener(v -> selectTab(tabOpera));
        tabMusic.setOnClickListener(v -> selectTab(tabMusic));
        tabNews.setOnClickListener(v -> selectTab(tabNews));

        findViewById(R.id.btn_prev).setOnClickListener(v -> playPrevious());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        findViewById(R.id.btn_next).setOnClickListener(v -> playNext());
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void setNavSelected(View selectedView, boolean selected) {
        navHome.setSelected(selectedView == navHome);
        navFeatures.setSelected(selectedView == navFeatures);
        navMessages.setSelected(selectedView == navMessages);
        navProfile.setSelected(selectedView == navProfile);
    }

    private void selectTab(LinearLayout selectedTab) {
        resetAllTabs();
        if (selectedTab.getId() == R.id.tab_opera) {
            selectedTab.setBackgroundResource(R.drawable.bg_enjoy_tab_selected);
            ivOpera.setColorFilter(getResources().getColor(R.color.enjoy_life_tab_text_selected));
            tvOpera.setTextColor(getResources().getColor(R.color.enjoy_life_tab_text_selected));
            tvOpera.setTypeface(null, android.graphics.Typeface.BOLD);
            updateRecyclerViewData("opera");
        } else if (selectedTab.getId() == R.id.tab_music) {
            selectedTab.setBackgroundResource(R.drawable.bg_enjoy_tab_selected);
            ivMusic.setColorFilter(getResources().getColor(R.color.enjoy_life_tab_text_selected));
            tvMusic.setTextColor(getResources().getColor(R.color.enjoy_life_tab_text_selected));
            tvMusic.setTypeface(null, android.graphics.Typeface.BOLD);
            updateRecyclerViewData("music");
        } else if (selectedTab.getId() == R.id.tab_news) {
            selectedTab.setBackgroundResource(R.drawable.bg_enjoy_tab_selected);
            ivNews.setColorFilter(getResources().getColor(R.color.enjoy_life_tab_text_selected));
            tvNews.setTextColor(getResources().getColor(R.color.enjoy_life_tab_text_selected));
            tvNews.setTypeface(null, android.graphics.Typeface.BOLD);
            updateRecyclerViewData("news");
        }
    }

    private void resetAllTabs() {
        // Reset opera tab
        tabOpera.setBackgroundResource(R.drawable.bg_enjoy_tab_unselected);
        ivOpera.setColorFilter(getResources().getColor(R.color.enjoy_life_tab_text_unselected));
        tvOpera.setTextColor(getResources().getColor(R.color.enjoy_life_tab_text_unselected));
        tvOpera.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Reset music tab
        tabMusic.setBackgroundResource(R.drawable.bg_enjoy_tab_unselected);
        ivMusic.setColorFilter(getResources().getColor(R.color.enjoy_life_tab_text_unselected));
        tvMusic.setTextColor(getResources().getColor(R.color.enjoy_life_tab_text_unselected));
        tvMusic.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Reset news tab
        tabNews.setBackgroundResource(R.drawable.bg_enjoy_tab_unselected);
        ivNews.setColorFilter(getResources().getColor(R.color.enjoy_life_tab_text_unselected));
        tvNews.setTextColor(getResources().getColor(R.color.enjoy_life_tab_text_unselected));
        tvNews.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void updateRecyclerViewData(String tabType) {
        currentTab = tabType;
        if ("opera".equals(tabType)) {
            isNewsMode = false;

            if (carouselContainer != null) {
                carouselContainer.setVisibility(View.VISIBLE);
            }
            recyclerView.setAdapter(dramaAdapter);
            dramaList.clear();
            fetchAndDisplayOperas();
        } else if ("music".equals(tabType)) {
            isNewsMode = false;
            if (carouselContainer != null) {
                carouselContainer.setVisibility(View.VISIBLE);
            }
            recyclerView.setAdapter(dramaAdapter);
            dramaList.clear();
            fetchAndDisplayMusic();
        } else if ("news".equals(tabType)) {
            isNewsMode = true;
            if (carouselContainer != null) {
                carouselContainer.setVisibility(View.GONE);
            }
            recyclerView.setAdapter(newsAdapter);
            fetchAndDisplayNews();

        }
    }



    private void fetchAndDisplayOperas() {
        Log.d(TAG, "fetchAndDisplayOperas() called");
        OperaApi.fetchOperaList(MessagesActivity.this, new OperaApi.OperaListCallback() {
            @Override
            public void onSuccess(List<DramaItem> operas) {
                int size = (operas == null ? 0 : operas.size());
                Log.d(TAG, "fetchAndDisplayOperas onSuccess, size=" + size);
                if (operas != null) {
                    for (int i = 0; i < operas.size(); i++) {
                        DramaItem item = operas.get(i);
                        if (item != null) {
                            Log.d(TAG, "Opera item[" + i + "]: id=" + item.getId()
                                    + ", title=" + item.getTitle()
                                    + ", coverUrl=" + item.getCoverUrl()
                                    + ", audioUrl=" + item.getAudioUrl());
                        }
                    }
                    dramaList.addAll(operas);
                }
                dramaAdapter.notifyDataSetChanged();
                updateCarouselWithOperaData(operas);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to fetch operas: " + errorMessage);
                Toast.makeText(MessagesActivity.this, "数据加载失败，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchAndDisplayMusic() {
        Log.d(TAG, "fetchAndDisplayMusic() called");
        OperaApi.fetchMusicList(MessagesActivity.this, new OperaApi.OperaListCallback() {
            @Override
            public void onSuccess(List<DramaItem> musicList) {
                int size = (musicList == null ? 0 : musicList.size());
                Log.d(TAG, "fetchAndDisplayMusic onSuccess, size=" + size);
                dramaList.clear(); // Clear the main list
                if (musicList != null) {
                    for (int i = 0; i < musicList.size(); i++) {
                        DramaItem item = musicList.get(i);
                        if (item != null) {
                            Log.d(TAG, "Music item[" + i + "]: id=" + item.getId()
                                    + ", title=" + item.getTitle()
                                    + ", coverUrl=" + item.getCoverUrl()
                                    + ", audioUrl=" + item.getAudioUrl());
                        }
                    }
                    dramaList.addAll(musicList);
                }
                applyFavoritesToDramaList(dramaList, true);
                dramaAdapter.notifyDataSetChanged();

                updateCarouselWithOperaData(musicList); // Reuse the same carousel logic

            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to fetch music: " + errorMessage);
                Toast.makeText(MessagesActivity.this, "音乐数据加载失败，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // 从后端接口加载资讯列表
    private void fetchAndDisplayNews() {
        ArticleApi.fetchArticleList(MessagesActivity.this, new ArticleApi.ArticleListCallback() {
            @Override
            public void onSuccess(List<NewsItem> articles) {
                newsList.clear();
                if (articles != null) {
                    newsList.addAll(articles);
                }
                applyFavoritesToNewsList(newsList);
                newsAdapter.notifyDataSetChanged();


                // 默认将第一条资讯展示到底部播放器（不自动播放音频）
                if (!newsList.isEmpty()) {
                    NewsItem first = newsList.get(0);
                    playerTitle.setText(first.getTitle());
                    playerCategory.setText(first.getCategory());
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to fetch articles: " + errorMessage);
                Toast.makeText(MessagesActivity.this, "资讯加载失败，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 点击资讯Item时，更新底部播放器并播放该资讯音频
    private void playNewsItem(NewsItem newsItem) {
        if (newsItem == null) return;
        playerTitle.setText(newsItem.getTitle());
        playerCategory.setText(newsItem.getCategory());

        String audioUrl = newsItem.getAudioUrl();
        if (audioUrl == null || audioUrl.isEmpty()) {
            Toast.makeText(this, "该资讯暂无音频", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "资讯音频播放失败", Toast.LENGTH_SHORT).show();
            return;
        }

        mediaPlayer.setOnPreparedListener(mp -> {
            mp.start();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            startProgressUpdate();
        });
    }

    /**
     * 点击资讯卡片右侧红心
     */
    private void onNewsLikeClicked(NewsItem newsItem, int position) {
        if (newsItem == null) return;

        int userId = AuthStore.getUserId(this);
        if (userId <= 0) {
            Toast.makeText(this, "请先登录后再收藏", Toast.LENGTH_SHORT).show();
            return;
        }

        int articleId = newsItem.getId();
        if (articleId <= 0) {
            Toast.makeText(this, "文章数据异常，无法收藏", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean currentlyFavorite = newsItem.isFavorite();
        if (!currentlyFavorite) {
            // 当前未收藏，点击后发起收藏请求
            ArticleApi.favoriteArticle(this, articleId, userId, new ArticleApi.SimpleCallback() {
                @Override
                public void onSuccess() {
                    favoriteArticleIds.add(articleId);
                    newsItem.setFavorite(true);
                    newsAdapter.notifyItemChanged(position);
                    Toast.makeText(MessagesActivity.this, "已收藏", Toast.LENGTH_SHORT).show();
                }


                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MessagesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // 当前已收藏，点击后取消收藏
            ArticleApi.unfavoriteArticle(this, articleId, userId, new ArticleApi.SimpleCallback() {
                @Override
                public void onSuccess() {
                    favoriteArticleIds.remove(articleId);
                    newsItem.setFavorite(false);
                    newsAdapter.notifyItemChanged(position);
                    Toast.makeText(MessagesActivity.this, "已取消收藏", Toast.LENGTH_SHORT).show();
                }


                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(MessagesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    private void setupCarousel() {

        carouselDramaItems = new ArrayList<>();
        carouselAdapter = new CarouselAdapter(carouselDramaItems);
        carouselViewPager.setAdapter(carouselAdapter);
        carouselViewPager.registerOnPageChangeCallback(carouselPageChangeCallback);
    }

    private void updateCarouselWithOperaData(List<DramaItem> operaList) {
        if (operaList == null || operaList.isEmpty()) {
            carouselDramaItems.clear();
            carouselAdapter.notifyDataSetChanged();
            setupIndicators(0);
            stopCarouselAutoPlay();
            return;
        }

        // Shuffle to get random items
        List<DramaItem> shuffledList = new ArrayList<>(operaList);
        Collections.shuffle(shuffledList, new Random(System.nanoTime()));

        // Get up to 3 items
        int count = Math.min(shuffledList.size(), 3);
        List<DramaItem> newCarouselItems = new ArrayList<>(shuffledList.subList(0, count));

        carouselDramaItems.clear();
        carouselDramaItems.addAll(newCarouselItems);
        carouselAdapter.notifyDataSetChanged();

        setupIndicators(carouselDramaItems.size());
        updateIndicator(0);
        startCarouselAutoPlay();
    }

    private void setupIndicators(int count) {
        carouselIndicator.removeAllViews();
        if (count <= 1) return;
        for (int i = 0; i < count; i++) {
            carouselIndicator.addView(createIndicatorView(i == 0));
        }
    }

    private void updateIndicator(int position) {
        if (carouselIndicator == null) return;
        for (int i = 0; i < carouselIndicator.getChildCount(); i++) {
            View indicator = carouselIndicator.getChildAt(i);
            indicator.setBackgroundResource(i == position ? R.drawable.bg_carousel_dot_selected : R.drawable.bg_carousel_dot_unselected);
        }
    }

    private View createIndicatorView(boolean selected) {
        View dot = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        dot.setLayoutParams(params);
        dot.setBackgroundResource(selected ? R.drawable.bg_carousel_dot_selected : R.drawable.bg_carousel_dot_unselected);
        return dot;
    }

    private void startCarouselAutoPlay() {
        stopCarouselAutoPlay(); // Stop any existing handler
        if (carouselAdapter == null || carouselAdapter.getItemCount() <= 1) return;
        if (carouselHandler == null) carouselHandler = new Handler(Looper.getMainLooper());

        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (carouselViewPager == null || carouselAdapter == null || carouselAdapter.getItemCount() == 0) return;
                int nextItem = (carouselViewPager.getCurrentItem() + 1) % carouselAdapter.getItemCount();
                carouselViewPager.setCurrentItem(nextItem, true);
                // 重新调度下一次执行
                carouselHandler.postDelayed(this, CAROUSEL_INTERVAL_MS);
            }
        };
        carouselHandler.postDelayed(carouselRunnable, CAROUSEL_INTERVAL_MS);
    }

    private void stopCarouselAutoPlay() {
        if (carouselHandler != null && carouselRunnable != null) {
            carouselHandler.removeCallbacks(carouselRunnable);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCarouselAutoPlay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCarouselAutoPlay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCarouselAutoPlay();
        if (carouselViewPager != null) {
            carouselViewPager.unregisterOnPageChangeCallback(carouselPageChangeCallback);
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // 清理评论输入框
        hideCommentInput();

        // 移除键盘监听
        try {
            if (rootViewChild != null && keyboardListener != null) {
                rootViewChild.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
            }
        } catch (Exception ignored) {}

        // 清理Glide内存缓存
        Glide.get(this).clearMemory();
    }

    private void playDrama(int position) {
        if (position < 0 || position >= dramaList.size()) return;
        DramaItem selectedDrama = dramaList.get(position);
        if (selectedDrama == null) return;
        playerTitle.setText(selectedDrama.getTitle());
        playerCategory.setText(selectedDrama.getArtist()); // Using artist for the subtitle

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.setDataSource(selectedDrama.getAudioUrl());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "音频播放失败", Toast.LENGTH_SHORT).show();
        }

        mediaPlayer.setOnPreparedListener(mp -> {
            mp.start();
            isPlaying = true;
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            // 开始进度条更新
            startProgressUpdate();
        });
    }

    /**
     * 点击戏曲卡片右侧红心
     */
    private void onDramaLikeClicked(int position) {
        if (position < 0 || position >= dramaList.size()) return;
        DramaItem item = dramaList.get(position);
        if (item == null) return;

        int userId = AuthStore.getUserId(this);
        if (userId <= 0) {
            Toast.makeText(this, "请先登录后再收藏", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean currentlyFavorite = item.isFavorite();

        // 根据当前 Tab 调用不同的收藏接口
        boolean isMusicTab = "music".equals(currentTab);

        if (!currentlyFavorite) {
            // 当前未收藏，点击后发起收藏请求
            if (isMusicTab) {
                OperaApi.favoriteMusic(this, item.getId(), userId, new OperaApi.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        item.setFavorite(true);
                        dramaAdapter.notifyItemChanged(position);
                        Toast.makeText(MessagesActivity.this, "已收藏音乐", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MessagesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                OperaApi.favoriteOpera(this, item.getId(), userId, new OperaApi.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        favoriteOperaIds.add(item.getId());
                        item.setFavorite(true);
                        dramaAdapter.notifyItemChanged(position);
                        Toast.makeText(MessagesActivity.this, "已收藏戏曲", Toast.LENGTH_SHORT).show();
                    }


                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MessagesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            // 当前已收藏，点击后取消收藏
            if (isMusicTab) {
                OperaApi.unfavoriteMusic(this, item.getId(), userId, new OperaApi.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        favoriteMusicIds.remove(item.getId());
                        item.setFavorite(false);
                        dramaAdapter.notifyItemChanged(position);
                        Toast.makeText(MessagesActivity.this, "已取消收藏音乐", Toast.LENGTH_SHORT).show();
                    }


                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MessagesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                OperaApi.unfavoriteOpera(this, item.getId(), userId, new OperaApi.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        item.setFavorite(false);
                        dramaAdapter.notifyItemChanged(position);
                        Toast.makeText(MessagesActivity.this, "已取消收藏戏曲", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(MessagesActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }



    private void playPrevious() {
        // Previous logic not implemented
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                btnPlayPause.setImageResource(R.drawable.ic_play);
                // 停止进度条更新
                stopProgressUpdate();
            } else {
                mediaPlayer.start();
                isPlaying = true;
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                // 开始进度条更新
                startProgressUpdate();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "togglePlayPause failed", e);
        }
    }


    private void playNext() {
        // Next logic not implemented
    }

    /**
     * 显示评论输入框，类似首页的输入框样式，会让界面向上移动
     */
    private void showCommentInput(int position) {
        try {
            currentCommentPosition = position;
            Log.d(TAG, "showCommentInput called for position: " + position);

            ViewGroup root = findViewById(android.R.id.content);
            if (root == null) {
                Log.w(TAG, "showCommentInput: root not found");
                return;
            }

            float density = getResources().getDisplayMetrics().density;

            // 如果 overlay 尚不存在，创建并添加到根视图（位于底部）
            if (commentOverlayContainer == null) {
                FrameLayout overlay = new FrameLayout(this);
                commentOverlayContainer = overlay;
                commentOverlayContainer.setClickable(true);
                FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                overlayLp.gravity = Gravity.BOTTOM | Gravity.START;
                root.addView(commentOverlayContainer, overlayLp);
                commentOverlayContainer.bringToFront();
            } else {
                // 清理旧的视图并调整为底部显示
                commentOverlayContainer.removeAllViews();
                try {
                    ViewGroup.LayoutParams lp = commentOverlayContainer.getLayoutParams();
                    if (lp instanceof FrameLayout.LayoutParams) {
                        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
                        flp.gravity = Gravity.BOTTOM | Gravity.START;
                        commentOverlayContainer.setLayoutParams(flp);
                    }
                } catch (Exception ignored) {}
                commentOverlayContainer.bringToFront();
            }

            // 设置全局点击，点击外部区域收起输入框
            try {
                commentRootForTouch = root;
                commentRootForTouch.setOnTouchListener((v, ev) -> {
                    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                        if (commentOverlayContainer != null && commentOverlayContainer.getVisibility() == View.VISIBLE) {
                            int[] loc = new int[2];
                            commentOverlayContainer.getLocationOnScreen(loc);
                            Rect rect = new Rect(loc[0], loc[1],
                                    loc[0] + commentOverlayContainer.getWidth(),
                                    loc[1] + commentOverlayContainer.getHeight());
                            int x = (int) ev.getRawX();
                            int y = (int) ev.getRawY();
                            if (!rect.contains(x, y)) {
                                hideCommentInput();
                                return true;
                            }
                        }
                    }
                    return false;
                });
            } catch (Exception ignored) {}

            // 创建横向输入条：麦克风 | 圆角输入 | 发送
            android.widget.LinearLayout inputLayout = new android.widget.LinearLayout(this);
            inputLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            inputLayout.setBackgroundColor(Color.WHITE);
            inputLayout.setElevation(6 * density);
            int pad = (int) (8 * density);
            inputLayout.setPadding(pad, pad, pad, pad);
            inputLayout.setGravity(Gravity.CENTER_VERTICAL);

            // 麦克风按钮（左）
            ImageButton micBtn = new ImageButton(this);
            micBtn.setBackgroundResource(R.drawable.btn_voice_background);
            micBtn.setImageResource(R.drawable.ic_mic);
            micBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
            micBtn.setPadding((int)(8*density),(int)(8*density),(int)(8*density),(int)(8*density));
            micBtn.setOnClickListener(v -> {
                // 可以复用Home的录音逻辑，暂时只提示
                Toast.makeText(MessagesActivity.this, "按住说话（未实现）", Toast.LENGTH_SHORT).show();
            });

            // 输入框（中）
            commentInputText = new EditText(this);
            commentInputText.setHint("评论一句...");
            commentInputText.setBackgroundResource(R.drawable.bg_input);
            commentInputText.setPadding((int)(14 * density), (int)(10 * density), (int)(14 * density), (int)(10 * density));
            commentInputText.setTextSize(14);
            commentInputText.setMaxLines(3);
            commentInputText.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

            // 发送按钮（右）
            commentSendButton = new ImageButton(this);
            commentSendButton.setBackgroundResource(R.drawable.bg_icon_button);
            commentSendButton.setImageResource(R.drawable.ic_send);
            commentSendButton.setOnClickListener(v -> sendComment());

            // 布局参数
            android.widget.LinearLayout.LayoutParams micLp = new android.widget.LinearLayout.LayoutParams(
                    (int)(48*density), (int)(48*density));
            android.widget.LinearLayout.LayoutParams inputLp = new android.widget.LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            inputLp.setMargins((int)(8*density), 0, (int)(8*density), 0);
            android.widget.LinearLayout.LayoutParams sendLp = new android.widget.LinearLayout.LayoutParams(
                    (int)(44*density), (int)(44*density));

            inputLayout.addView(micBtn, micLp);
            inputLayout.addView(commentInputText, inputLp);
            inputLayout.addView(commentSendButton, sendLp);

            // 添加底部阴影占位（可选）
            FrameLayout.LayoutParams inputLp2 = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            inputLp2.gravity = Gravity.BOTTOM;
            commentOverlayContainer.addView(inputLayout, inputLp2);

            commentOverlayContainer.setVisibility(View.VISIBLE);
            commentOverlayContainer.bringToFront();

            // 聚焦输入并弹出键盘
            commentInputText.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commentInputText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }

        } catch (Throwable t) {
            Log.w(TAG, "showCommentInput failed", t);
        }
    }

    /**
     * 隐藏评论输入框
     */
    private void hideCommentInput() {
        try {
            if (commentOverlayContainer != null) {
                commentOverlayContainer.removeAllViews();
                commentOverlayContainer.setVisibility(View.GONE);
            }

            // 隐藏键盘
            if (commentInputText != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(commentInputText.getWindowToken(), 0);
                }
            }

            currentCommentPosition = -1;
            commentInputText = null;
            commentSendButton = null;
            commentCloseButton = null;
            // 取消根视图的触摸监听（如果存在）
            try {
                if (commentRootForTouch != null) {
                    commentRootForTouch.setOnTouchListener(null);
                    commentRootForTouch = null;
                }
            } catch (Exception ignored) {}
        } catch (Throwable t) {
            Log.w(TAG, "hideCommentInput failed", t);
        }
    }

    /**
     * 发送评论
     */
    private void sendComment() {
        if (commentInputText == null || currentCommentPosition < 0) return;

        String comment = commentInputText.getText().toString().trim();
        if (TextUtils.isEmpty(comment)) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: 这里应该发送评论到服务器
        // 暂时只显示Toast
        Toast.makeText(this, "评论已发送: " + comment, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "发送评论: " + comment + " for position: " + currentCommentPosition);

        // 清空输入框并隐藏
        commentInputText.setText("");
        hideCommentInput();
    }

    /**
     * 设置键盘监听器，当键盘弹出时移动界面
     */
    private void setupKeyboardListener() {
        final android.view.ViewGroup content = (android.view.ViewGroup) findViewById(android.R.id.content);
        if (content == null) return;

        // 获取需要移动的视图
        final View scrollView = findViewById(R.id.scroll_content);

        keyboardListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                try {
                    android.graphics.Rect r = new android.graphics.Rect();
                    content.getWindowVisibleDisplayFrame(r);
                    int screenHeight = content.getRootView().getHeight();
                    int keypadHeight = screenHeight - (r.bottom - r.top);
                    boolean isNowVisible = keypadHeight > screenHeight * 0.15;
                    if (isNowVisible && !keyboardVisible) {
                        keyboardVisible = true;
                        // 键盘弹出时，如果评论输入框可见，让内容向上移动
                        if (commentOverlayContainer != null && commentOverlayContainer.getVisibility() == View.VISIBLE) {
                            if (scrollView != null) {
                                scrollView.animate().translationY(-keypadHeight * 0.3f).setDuration(220).start();
                                // overlay anchored bottom will be moved by adjustResize; no manual translation
                            }
                        }
                    } else if (!isNowVisible && keyboardVisible) {
                        keyboardVisible = false;
                        // 键盘隐藏时，恢复内容位置
                        if (scrollView != null) {
                            scrollView.animate().translationY(0).setDuration(220).start();
                        }
                        // 键盘隐藏时如果有评论输入框，隐藏它
                        if (commentOverlayContainer != null && commentOverlayContainer.getVisibility() == View.VISIBLE) {
                            hideCommentInput();
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "keyboard global layout failed", e);
                }
            }
        };

        // 使用内容视图来监听键盘变化
        View rootView = content.getChildCount() > 0 ? content.getChildAt(0) : content;
        if (rootView != null) {
            rootViewChild = rootView;
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        }
    }
}
