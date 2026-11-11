package com.virhuman.testsdk.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.utils.CommentOverlayManager;

import java.util.ArrayList;
import java.util.List;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 社区交互页 - 使用 RecyclerView 展示帖子；实现：跳转到新建的我的主页、用户信息、发帖界面；点赞/收藏/评论交互。
 */
public class FeaturesActivity extends AppCompatActivity {
    private View navHome;
    private View navFeatures;
    private View navMessages;
    private View navProfile;
    private ImageView ivNotifications;
    private ImageView ivCommunityHome;
    private TextView btnAddPost;
    private RecyclerView rvPosts;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private PostsAdapter postsAdapter;
    // Comment overlay fields (for bottom input)
    private ViewGroup commentOverlayContainer;
    private EditText commentInputText;
    private ImageButton commentSendButton;
    private View rootViewChild;
    private boolean keyboardVisible = false;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private ViewGroup commentRootForTouch;
    private CommentOverlayManager commentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure layout resizes when keyboard appears so bottom overlay moves above keyboard
        try {
            getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } catch (Exception ignored) {}
        setContentView(R.layout.activity_features);

        initViews();
        commentManager = new CommentOverlayManager(this);
        setupRecycler();
        setListeners();
        // 请求后端帖子列表（需要 token）
        fetchPostsFromServer();
        // 设置当前页面为选中状态
        setNavSelected(navFeatures, true);
    }

    private void initViews() {
        navHome = findViewById(R.id.nav_home);
        navFeatures = findViewById(R.id.nav_features);
        navMessages = findViewById(R.id.nav_messages);
        navProfile = findViewById(R.id.nav_profile);
        ivNotifications = findViewById(R.id.iv_notifications);
        ivCommunityHome = findViewById(R.id.iv_community_home);
        btnAddPost = findViewById(R.id.btn_add_post);
        rvPosts = findViewById(R.id.rv_posts);
        swipeRefresh = findViewById(R.id.swipe_refresh);
    }

    private void setupRecycler() {
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        List<Post> samples = new ArrayList<>();
        postsAdapter = new PostsAdapter(samples);
        rvPosts.setAdapter(postsAdapter);
    }

    private void fetchPostsFromServer() {
        String token = com.virhuman.testsdk.utils.AuthStore.getToken(this);
        if (token == null || token.trim().isEmpty()) {
            // 未登录，保持本地示例
            runOnUiThread(() -> {
                List<Post> samples = createSamplePosts();
                postsAdapter.items.clear();
                postsAdapter.items.addAll(samples);
                postsAdapter.notifyDataSetChanged();
            });
            return;
        }

        new Thread(() -> {
            try {
                Request req = new Request.Builder()
                        .url(com.virhuman.testsdk.net.ApiConfig.POSTS_URL)
                        .header("Authorization", "Bearer " + token)
                        .get()
                        .build();
                Response resp = com.virhuman.testsdk.net.ApiClient.get().newCall(req).execute();
                if (!resp.isSuccessful()) {
                    // fallback to samples
                    runOnUiThread(() -> {
                        List<Post> samples = createSamplePosts();
                        postsAdapter.items.clear();
                        postsAdapter.items.addAll(samples);
                        postsAdapter.notifyDataSetChanged();
                    });
                    return;
                }
                String body = resp.body() != null ? resp.body().string() : null;
                if (body == null) {
                    runOnUiThread(() -> {
                        List<Post> samples = createSamplePosts();
                        postsAdapter.items.clear();
                        postsAdapter.items.addAll(samples);
                        postsAdapter.notifyDataSetChanged();
                    });
                    return;
                }
                JSONObject root = new JSONObject(body);
                JSONArray data = root.optJSONArray("data");
                List<Post> posts = new ArrayList<>();
                if (data != null) {
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject p = data.getJSONObject(i);
                        JSONObject author = p.optJSONObject("author");
                        String name = author != null ? author.optString("realname", author.optString("username", "用户")) : "用户";
                        String content = p.optString("content", "");
                        String createTime = p.optString("createTime", "");
                        int authorId = p.optInt("authorId", 0);
                        int avatarRes = R.drawable.laoren1;
                        if (authorId % 4 == 1) avatarRes = R.drawable.laoren1;
                        else if (authorId % 4 == 2) avatarRes = R.drawable.laoren2;
                        else if (authorId % 4 == 3) avatarRes = R.drawable.laoren3;
                        else avatarRes = R.drawable.background1;

                        Post post = new Post(name, createTime, content, avatarRes);
                        post.title = p.optString("title", "");
                        post.postTags = p.optString("postTags", "");
                        post.locationTags = p.optString("locationTags", "");
                        // images may be comma separated string or empty
                        String imgs = p.optString("images", "");
                        post.images = imgs != null ? imgs : "";
                        post.audioUrl = p.optString("audioUrl", "");
                        posts.add(post);
                    }
                }
                runOnUiThread(() -> {
                    postsAdapter.items.clear();
                    postsAdapter.items.addAll(posts);
                    postsAdapter.notifyDataSetChanged();
                    if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            } catch (Throwable t) {
                t.printStackTrace();
                runOnUiThread(() -> {
                    List<Post> samples = createSamplePosts();
                    postsAdapter.items.clear();
                    postsAdapter.items.addAll(samples);
                    postsAdapter.notifyDataSetChanged();
                    if (swipeRefresh != null && swipeRefresh.isRefreshing()) swipeRefresh.setRefreshing(false);
                });
            }
        }).start();
    }

    private List<Post> createSamplePosts() {
        List<Post> list = new ArrayList<>();
        list.add(new Post("张大爷", "10分钟前", "今天公园里的花开得真好，心情舒畅！拍了几张大家看看。", R.drawable.laoren1));
        list.add(new Post("李奶奶", "30分钟前", "小雨真是太贴心了，今天提醒我吃药，还给我讲了个笑话！", R.drawable.laoren2));
        list.add(new Post("王叔叔", "1小时前", "新学了一段京剧《空城计》，大伙儿给指点指点！", R.drawable.laoren3));
        list.add(new Post("赵阿姨", "2小时前", "孙子教我跳的广场舞，这首曲子真带劲！", R.drawable.background1));
        return list;
    }

    private void setListeners() {
        navHome.setOnClickListener(v -> {
            setNavSelected(navHome, true);
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        navFeatures.setOnClickListener(v -> {
            // 当前页面，不需要跳转
            setNavSelected(navFeatures, true);
        });

        navMessages.setOnClickListener(v -> {
            setNavSelected(navMessages, true);
            Intent intent = new Intent(this, MessagesActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // 导航栏右侧提醒按钮 -> 新的通知页面
        ivNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
        });

        // 顶部左侧社区我的图标 -> 社区帖子主页（新的界面）
        ivCommunityHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyHomeActivity.class);
            startActivity(intent);
        });

        // 底部“我的”图标：使用自定义图标（shequwode.png）并跳转到新的我的主页
        navProfile.setOnClickListener(v -> {
            // 恢复为原始个人中心行为，跳转到已有的 ProfileActivity
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // 悬浮发帖按钮
        btnAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreatePostActivity.class);
            startActivity(intent);
        });
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                // refresh posts
                fetchPostsFromServer();
            });
        }

        // 点击 search 区域时聚焦并弹出键盘（确保点击整个 SearchView 区域都可唤起键盘）
        try {
            SearchView searchView = findViewById(R.id.search_view);
            View tagContainer = findViewById(R.id.tag_container);
            if (searchView != null) {
                // Make clicking anywhere on the SearchView behave like clicking the left search icon.
                View leftIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
                if (leftIcon == null) leftIcon = searchView.findViewById(androidx.appcompat.R.id.search_button);
                final View resolvedLeftIcon = leftIcon;
                searchView.setOnClickListener(v -> {
                    // ensure SearchView is expanded (not iconified) so input becomes visible immediately
                    try {
                        searchView.setIconified(false);
                    } catch (Exception ignored) {}
                    // then focus input and show keyboard
                    searchView.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        View inner = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                        if (inner != null) {
                            inner.requestFocus();
                        }
                        imm.showSoftInput(inner != null ? inner : searchView, InputMethodManager.SHOW_IMPLICIT);
                    }
                });
                // also ensure touching the text area triggers same behavior
                View inner = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
                if (inner != null) {
                    inner.setFocusable(true);
                    inner.setFocusableInTouchMode(true);
                    inner.setOnTouchListener((vv, ev) -> {
                        if (ev.getAction() == MotionEvent.ACTION_UP) {
                            try {
                                searchView.setIconified(false);
                            } catch (Exception ignored) {}
                            vv.requestFocus();
                            InputMethodManager imm2 = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm2 != null) imm2.showSoftInput(vv, InputMethodManager.SHOW_IMPLICIT);
                        }
                        return false;
                    });
                }
            }

            // 根视图点击：若点击不在 search 或 tag 区域，则隐藏键盘
            ViewGroup root = (ViewGroup) getWindow().getDecorView();
            if (root != null) {
                root.setOnTouchListener((v, ev) -> {
                    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                        int x = (int) ev.getRawX();
                        int y = (int) ev.getRawY();
                        boolean inSearch = false;
                        boolean inTag = false;
                        if (searchView != null) {
                            int[] loc = new int[2];
                            searchView.getLocationOnScreen(loc);
                            Rect r = new Rect(loc[0], loc[1], loc[0] + searchView.getWidth(), loc[1] + searchView.getHeight());
                            inSearch = r.contains(x, y);
                        }
                        if (tagContainer != null) {
                            int[] loc2 = new int[2];
                            tagContainer.getLocationOnScreen(loc2);
                            Rect r2 = new Rect(loc2[0], loc2[1], loc2[0] + tagContainer.getWidth(), loc2[1] + tagContainer.getHeight());
                            inTag = r2.contains(x, y);
                        }
                        if (!inSearch && !inTag) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
                            }
                            // 清除焦点
                            View current = getCurrentFocus();
                            if (current != null) current.clearFocus();
                        }
                    }
                    return false;
                });
            }
        } catch (Throwable ignored) {}
    }

    /**
     * 设置导航栏选中状态
     */
    private void setNavSelected(View selectedView, boolean selected) {
        navHome.setSelected(selectedView == navHome);
        navFeatures.setSelected(selectedView == navFeatures);
        navMessages.setSelected(selectedView == navMessages);
        navProfile.setSelected(selectedView == navProfile);
    }

    /* ----------------- 简单的数据模型和适配器实现 ----------------- */
    private static class Post {
        String userName;
        String time;
        String text;
        int avatarRes;
        String audioUrl;
        String title;
        String postTags;
        String locationTags;
        String images; // comma-separated URLs
        int likeCount = 0;
        int favCount = 0;
        boolean liked = false;
        boolean favored = false;

        Post(String userName, String time, String text, int avatarRes) {
            this.userName = userName;
            this.time = time;
            this.text = text;
            this.avatarRes = avatarRes;
            this.title = "";
            this.postTags = "";
            this.locationTags = "";
            this.images = "";
            this.audioUrl = null;
        }
    }

    private class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostVH> {
        private final List<Post> items;

        PostsAdapter(List<Post> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public PostVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
            return new PostVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PostVH holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class PostVH extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvName;
            TextView tvTime;
            TextView tvText;
            TextView tvTags;
            TextView tvLocation;
            LinearLayout imgContainer;
            ImageView ivLike;
            TextView tvLikeCount;
            ImageView ivFav;
            TextView tvFavCount;
            ImageView ivComment;
            LinearLayout commentsContainer;
            // audio
            ImageButton ivAudioPlay;
            TextView tvAudioLabel;
            android.media.MediaPlayer audioPlayer;
            boolean audioPlaying = false;

            PostVH(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.iv_post_avatar);
                tvName = itemView.findViewById(R.id.tv_post_name);
                tvTime = itemView.findViewById(R.id.tv_post_time);
                tvText = itemView.findViewById(R.id.tv_post_text);
                tvTags = itemView.findViewById(R.id.tv_post_tags);
                tvLocation = itemView.findViewById(R.id.tv_post_location);
                imgContainer = itemView.findViewById(R.id.img_container);
                ivLike = itemView.findViewById(R.id.iv_like);
                tvLikeCount = itemView.findViewById(R.id.tv_like_count);
                ivFav = itemView.findViewById(R.id.iv_fav);
                tvFavCount = itemView.findViewById(R.id.tv_fav_count);
                ivComment = itemView.findViewById(R.id.iv_comment);
                commentsContainer = itemView.findViewById(R.id.comments_container);
                ivAudioPlay = itemView.findViewById(R.id.iv_audio_play);
                tvAudioLabel = itemView.findViewById(R.id.tv_audio_label);
            }

            void bind(Post post) {
                ivAvatar.setImageResource(post.avatarRes);
                tvName.setText(post.userName);
                tvTime.setText(post.time);
                if (post.postTags != null && !post.postTags.isEmpty()) {
                    tvTags.setVisibility(View.VISIBLE);
                    tvTags.setText("标签: " + post.postTags);
                } else {
                    tvTags.setVisibility(View.GONE);
                }
                if (post.locationTags != null && !post.locationTags.isEmpty()) {
                    tvLocation.setVisibility(View.VISIBLE);
                    tvLocation.setText("位置: " + post.locationTags);
                } else {
                    tvLocation.setVisibility(View.GONE);
                }
                tvText.setText(post.text);
                // populate images
                imgContainer.removeAllViews();
                if (post.images != null && !post.images.trim().isEmpty()) {
                    String[] urls = post.images.split(",");
                    for (String url : urls) {
                        String u = url.trim();
                        if (u.isEmpty()) continue;
                        ImageView iv = new ImageView(itemView.getContext());
                        int dp = (int) (80 * itemView.getResources().getDisplayMetrics().density + 0.5f);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp, dp);
                        lp.setMargins(0, 0, (int)(6 * itemView.getResources().getDisplayMetrics().density), 0);
                        iv.setLayoutParams(lp);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imgContainer.addView(iv);
                        // load image async
                        loadImageAsync(u, iv);
                    }
                }
                tvLikeCount.setText(String.valueOf(post.likeCount));
                tvFavCount.setText(String.valueOf(post.favCount));
                ivLike.setImageResource(post.liked ? R.drawable.likecolour : R.drawable.like);
                ivFav.setImageResource(post.favored ? R.drawable.shoucangcoulor : R.drawable.shoucang);

                ivLike.setOnClickListener(v -> {
                    post.liked = !post.liked;
                    post.likeCount += post.liked ? 1 : -1;
                    if (post.likeCount < 0) post.likeCount = 0;
                    tvLikeCount.setText(String.valueOf(post.likeCount));
                    ivLike.setImageResource(post.liked ? R.drawable.likecolour : R.drawable.like);
                });

                ivFav.setOnClickListener(v -> {
                    post.favored = !post.favored;
                    post.favCount += post.favored ? 1 : -1;
                    if (post.favCount < 0) post.favCount = 0;
                    tvFavCount.setText(String.valueOf(post.favCount));
                    ivFav.setImageResource(post.favored ? R.drawable.shoucangcoulor : R.drawable.shoucang);
                    // 根据需求：点击收藏图标也可以打开评论输入，这里保持收藏与评论分离
                });

                ivComment.setOnClickListener(v -> {
                    // 使用统一的 CommentOverlayManager 弹出输入框
                    openCommentForPost(commentsContainer);
                });
                // audio handling
                if (ivAudioPlay != null && tvAudioLabel != null) {
                    if (post.audioUrl != null && !post.audioUrl.trim().isEmpty()) {
                        ivAudioPlay.setVisibility(View.VISIBLE);
                        tvAudioLabel.setVisibility(View.VISIBLE);
                        tvAudioLabel.setText("音频");
                        ivAudioPlay.setOnClickListener(v -> {
                            try {
                                if (audioPlayer == null) {
                                    audioPlayer = new android.media.MediaPlayer();
                                    audioPlayer.setDataSource(post.audioUrl);
                                    audioPlayer.setOnPreparedListener(mp -> {
                                        mp.start();
                                        audioPlaying = true;
                                        ivAudioPlay.setImageResource(R.drawable.ic_pause);
                                    });
                                    audioPlayer.setOnCompletionListener(mp -> {
                                        audioPlaying = false;
                                        ivAudioPlay.setImageResource(R.drawable.ic_play);
                                        try { audioPlayer.release(); } catch (Exception ignored) {}
                                        audioPlayer = null;
                                    });
                                    audioPlayer.prepareAsync();
                                } else {
                                    if (audioPlaying) {
                                        audioPlayer.pause();
                                        audioPlaying = false;
                                        ivAudioPlay.setImageResource(R.drawable.ic_play);
                                    } else {
                                        audioPlayer.start();
                                        audioPlaying = true;
                                        ivAudioPlay.setImageResource(R.drawable.ic_pause);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(itemView.getContext(), "音频播放失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        ivAudioPlay.setVisibility(View.GONE);
                        tvAudioLabel.setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private void openCommentForPost(LinearLayout commentsContainer) {
        if (commentManager == null) commentManager = new CommentOverlayManager(this);
        commentManager.show(text -> {
            runOnUiThread(() -> {
                TextView tv = new TextView(FeaturesActivity.this);
                tv.setText("我: " + text);
                tv.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
                commentsContainer.addView(tv);
            });
        });
    }

    /**
     * 显示底部评论输入框，样式与首页保持一致
     */
    private void showCommentInputForPost(Post post, LinearLayout commentsContainer) {
        try {
            ViewGroup root = findViewById(android.R.id.content);
            if (root == null) return;
            float density = getResources().getDisplayMetrics().density;

            if (commentOverlayContainer == null) {
                FrameLayout overlay = new FrameLayout(this);
                commentOverlayContainer = overlay;
                FrameLayout.LayoutParams overlayLp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                overlayLp.gravity = Gravity.BOTTOM | Gravity.START;
                root.addView(commentOverlayContainer, overlayLp);
                commentOverlayContainer.bringToFront();
            } else {
                commentOverlayContainer.removeAllViews();
                try {
                    ViewGroup.LayoutParams lp = commentOverlayContainer.getLayoutParams();
                    if (lp instanceof FrameLayout.LayoutParams) {
                        ((FrameLayout.LayoutParams) lp).gravity = Gravity.BOTTOM | Gravity.START;
                        commentOverlayContainer.setLayoutParams(lp);
                    }
                } catch (Exception ignored) {}
                commentOverlayContainer.bringToFront();
            }

            android.widget.LinearLayout inputLayout = new android.widget.LinearLayout(this);
            inputLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            inputLayout.setBackgroundColor(Color.WHITE);
            inputLayout.setElevation(6 * density);
            int pad = (int) (8 * density);
            inputLayout.setPadding(pad, pad, pad, pad);
            inputLayout.setGravity(Gravity.CENTER_VERTICAL);

            ImageButton micBtn = new ImageButton(this);
            micBtn.setBackgroundResource(R.drawable.btn_voice_background);
            micBtn.setImageResource(R.drawable.ic_mic);
            micBtn.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
            micBtn.setPadding((int)(8*density),(int)(8*density),(int)(8*density),(int)(8*density));

            commentInputText = new EditText(this);
            commentInputText.setHint("评论一句...");
            commentInputText.setBackgroundResource(R.drawable.bg_input);
            commentInputText.setPadding((int)(14 * density), (int)(10 * density), (int)(14 * density), (int)(10 * density));
            commentInputText.setTextSize(14);
            commentInputText.setMaxLines(3);
            commentInputText.setInputType(InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

            commentSendButton = new ImageButton(this);
            commentSendButton.setBackgroundResource(R.drawable.bg_icon_button);
            commentSendButton.setImageResource(R.drawable.ic_send);
            commentSendButton.setOnClickListener(v -> {
                String text = commentInputText.getText().toString().trim();
                if (!text.isEmpty()) {
                    TextView tv = new TextView(FeaturesActivity.this);
                    tv.setText("我: " + text);
                    tv.setTextAppearance(android.R.style.TextAppearance_Material_Body1);
                    commentsContainer.addView(tv);
                    commentInputText.setText("");
                    hideCommentInputForPost();
                } else {
                    Toast.makeText(FeaturesActivity.this, "请输入评论内容", Toast.LENGTH_SHORT).show();
                }
            });

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

            FrameLayout.LayoutParams inputLp2 = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            inputLp2.gravity = Gravity.BOTTOM;
            commentOverlayContainer.addView(inputLayout, inputLp2);
            commentOverlayContainer.setVisibility(View.VISIBLE);
            commentOverlayContainer.bringToFront();

            commentInputText.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commentInputText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }

            // add keyboard listener to move content up
            setupKeyboardListenerForFeatures();
            
            // 点击空白处收起输入框
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
                                hideCommentInputForPost();
                                return true;
                            }
                        }
                    }
                    return false;
                });
            } catch (Exception ignored) {}

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void hideCommentInputForPost() {
        try {
            if (commentOverlayContainer != null) {
                commentOverlayContainer.removeAllViews();
                commentOverlayContainer.setVisibility(View.GONE);
            }
            if (commentInputText != null) {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(commentInputText.getWindowToken(), 0);
                }
            }
            commentInputText = null;
            commentSendButton = null;
            // 取消根视图触摸监听
            try {
                if (commentRootForTouch != null) {
                    commentRootForTouch.setOnTouchListener(null);
                    commentRootForTouch = null;
                }
            } catch (Exception ignored) {}
        } catch (Throwable ignored) {}
    }

    private void setupKeyboardListenerForFeatures() {
        final android.view.ViewGroup content = (android.view.ViewGroup) findViewById(android.R.id.content);
        if (content == null) return;
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
                        if (commentOverlayContainer != null && commentOverlayContainer.getVisibility() == View.VISIBLE) {
                            if (scrollView != null) {
                                scrollView.animate().translationY(-keypadHeight * 0.3f).setDuration(220).start();
                            }
                        }
                    } else if (!isNowVisible && keyboardVisible) {
                        keyboardVisible = false;
                        if (scrollView != null) {
                            scrollView.animate().translationY(0).setDuration(220).start();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        View rootView = content.getChildCount() > 0 ? content.getChildAt(0) : content;
        if (rootView != null) {
            rootViewChild = rootView;
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);
        }
    }

    private void loadImageAsync(String url, ImageView target) {
        new Thread(() -> {
            try {
                Request r = new Request.Builder().url(url).get().build();
                Response resp = com.virhuman.testsdk.net.ApiClient.get().newCall(r).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    byte[] bytes = resp.body().bytes();
                    final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    runOnUiThread(() -> {
                        try {
                            target.setImageBitmap(bmp);
                        } catch (Throwable ignored) {}
                    });
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).start();
    }

}



