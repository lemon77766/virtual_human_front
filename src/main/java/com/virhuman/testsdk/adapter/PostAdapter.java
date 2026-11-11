package com.virhuman.testsdk.adapter;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.model.PostItem;
import com.virhuman.testsdk.ui.WaveformView;

import java.io.IOException;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostItem> postList;
    private Context context;
    private MediaPlayer mediaPlayer;
    private int currentPlayingPosition = -1;
    private Handler progressHandler;
    private Runnable progressRunnable;

    public PostAdapter(List<PostItem> postList, Context context) {
        this.postList = postList;
        this.context = context;
        initMediaPlayer();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
        );
        mediaPlayer.setOnCompletionListener(mp -> {
            stopPlayback();
        });
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostItem post = postList.get(position);
        holder.bind(post, position);
    }

    @Override
    public int getItemCount() {
        return postList != null ? postList.size() : 0;
    }

    public void updatePosts(List<PostItem> newPosts) {
        this.postList = newPosts;
        notifyDataSetChanged();
    }

    private void stopPlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        currentPlayingPosition = -1;
        notifyDataSetChanged();
        stopProgressUpdate();
    }

    private void startProgressUpdate(WaveformView waveformView, TextView durationView) {
        if (progressHandler == null) {
            progressHandler = new Handler(Looper.getMainLooper());
        }
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    int totalDuration = mediaPlayer.getDuration();

                    if (totalDuration > 0) {
                        // 更新波形视图进度
                        if (waveformView != null) {
                            // WaveformView expects integer progress in the same units as max.
                            // Ensure max is set to totalDuration (ms) and pass currentPosition directly.
                            try {
                                waveformView.setMax(totalDuration);
                                waveformView.setProgress(currentPosition);
                            } catch (Exception ignored) {
                                // Fallback: set a scaled int if unusual
                                waveformView.setMax(1000);
                                waveformView.setProgress((int) (1000f * currentPosition / Math.max(1, totalDuration)));
                            }
                        }

                        // 更新时间显示
                        if (durationView != null) {
                            String timeText = formatTime(currentPosition / 1000);
                            durationView.setText(timeText);
                        }
                    }

                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdate() {
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    public class PostViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivPostAvatar;
        private TextView tvPostName;
        private TextView tvPostTime;
        private TextView tvPostText;
        private TextView tvPostTags;
        private TextView tvPostLocation;
        private LinearLayout imgContainer;
        private LinearLayout audioContainer;
        private ImageButton ivAudioPlay;
        private WaveformView waveformAudioPost;
        private TextView tvAudioDurationPost;
        private ImageView ivLike;
        private TextView tvLikeCount;
        private ImageView ivComment;
        private ImageView ivFav;
        private TextView tvFavCount;
        private ConstraintLayout rootLayout;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            rootLayout = (ConstraintLayout) itemView;
            ivPostAvatar = itemView.findViewById(R.id.iv_post_avatar);
            tvPostName = itemView.findViewById(R.id.tv_post_name);
            tvPostTime = itemView.findViewById(R.id.tv_post_time);
            tvPostText = itemView.findViewById(R.id.tv_post_text);
            tvPostTags = itemView.findViewById(R.id.tv_post_tags);
            tvPostLocation = itemView.findViewById(R.id.tv_post_location);
            imgContainer = itemView.findViewById(R.id.img_container);
            audioContainer = itemView.findViewById(R.id.audio_container);
            ivAudioPlay = itemView.findViewById(R.id.iv_audio_play);
            waveformAudioPost = itemView.findViewById(R.id.waveform_audio_post);
            tvAudioDurationPost = itemView.findViewById(R.id.tv_audio_duration_post);
            ivLike = itemView.findViewById(R.id.iv_like);
            tvLikeCount = itemView.findViewById(R.id.tv_like_count);
            ivComment = itemView.findViewById(R.id.iv_comment);
            ivFav = itemView.findViewById(R.id.iv_fav);
            tvFavCount = itemView.findViewById(R.id.tv_fav_count);
        }

        public void bind(PostItem post, int position) {
            // 绑定基本信息
            tvPostName.setText(post.getAuthorName() != null ? post.getAuthorName() : "未知用户");
            tvPostTime.setText(post.getCreateTime() != null ? post.getCreateTime() : "");
            tvPostText.setText(post.getContent() != null ? post.getContent() : "");

            // 加载头像
            if (post.getAuthorAvatar() != null && !post.getAuthorAvatar().isEmpty()) {
                Glide.with(context)
                        .load(post.getAuthorAvatar())
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(ivPostAvatar);
            } else {
                ivPostAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            }

            // 设置标签和位置
            LinearLayout postMetaRow = itemView.findViewById(R.id.post_meta_row);
            if (post.getPostTags() != null && !post.getPostTags().isEmpty()) {
                tvPostTags.setText("标签: " + post.getPostTags());
                tvPostTags.setVisibility(View.VISIBLE);
            } else {
                tvPostTags.setVisibility(View.GONE);
            }

            if (post.getLocationTags() != null && !post.getLocationTags().isEmpty()) {
                tvPostLocation.setText("位置: " + post.getLocationTags());
                tvPostLocation.setVisibility(View.VISIBLE);
            } else {
                tvPostLocation.setVisibility(View.GONE);
            }

            if ((post.getPostTags() == null || post.getPostTags().isEmpty()) &&
                (post.getLocationTags() == null || post.getLocationTags().isEmpty())) {
                postMetaRow.setVisibility(View.GONE);
            } else {
                postMetaRow.setVisibility(View.VISIBLE);
            }

            // 处理图片
            imgContainer.removeAllViews();
            if (post.hasImages()) {
                imgContainer.setVisibility(View.VISIBLE);
                // 添加图片缩略图
                for (String imageUrl : post.getImages()) {
                    ImageView imageView = new ImageView(context);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            (int)(100 * context.getResources().getDisplayMetrics().density),
                            (int)(100 * context.getResources().getDisplayMetrics().density)
                    );
                    params.setMargins(0, 0, (int)(8 * context.getResources().getDisplayMetrics().density), 0);
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .centerCrop()
                            .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                                @Override
                                public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                    android.util.Log.w("PostAdapter", "Glide failed to load image: " + imageUrl, e);
                                    return false; // allow Glide to handle placeholder
                                }

                                @Override
                                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                    return false;
                                }
                            })
                            .into(imageView);

                    imgContainer.addView(imageView);
                }
            } else {
                imgContainer.setVisibility(View.GONE);
            }

            // 处理音频
            if (post.hasAudio()) {
                audioContainer.setVisibility(View.VISIBLE);
                tvAudioDurationPost.setText(post.getAudioDuration() != null ? post.getAudioDuration() : "00:00");

                // 设置音频播放按钮点击事件
                ivAudioPlay.setOnClickListener(v -> {
                    if (currentPlayingPosition == position) {
                        // 当前正在播放这个音频，停止播放
                        stopPlayback();
                    } else {
                        // 播放这个音频
                        playAudio(post.getAudioUrl(), position);
                    }
                });

                // 设置波形视图的拖拽监听
                // allow user to drag waveform to seek
                waveformAudioPost.setOnProgressChangeListener(progress -> {
                    if (mediaPlayer != null && currentPlayingPosition == position) {
                        int duration = mediaPlayer.getDuration();
                        int seekTo = (int) (duration * progress);
                        try { mediaPlayer.seekTo(seekTo); } catch (Exception ignored) {}
                    }
                });
            } else {
                audioContainer.setVisibility(View.GONE);
            }

            // 设置操作栏数据
            tvLikeCount.setText(String.valueOf(post.getLikeCount()));
            tvFavCount.setText(String.valueOf(post.getFavoriteCount()));

            ivLike.setImageResource(post.isLiked() ? R.drawable.likecolour : R.drawable.like);
            // drawable file in repo is named shoucangcoulor.png (note spelling), use that resource
            ivFav.setImageResource(post.isFavorited() ? R.drawable.shoucangcoulor : R.drawable.shoucang);

            // 设置点击事件
            ivLike.setOnClickListener(v -> {
                // 处理点赞逻辑
                handleLike(post, position);
            });

            ivFav.setOnClickListener(v -> {
                // 处理收藏逻辑
                handleFavorite(post, position);
            });

            ivComment.setOnClickListener(v -> {
                // 处理评论逻辑
                handleComment(post, position);
            });

            // 动态调整布局
            adjustLayoutForContent(post);
        }

        private void adjustLayoutForContent(PostItem post) {
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(rootLayout);

            if (post.hasOnlyText()) {
                // 只有文字：文字上下居中，不预留图片和语音位置
                // 将文字垂直居中
                constraintSet.clear(R.id.tv_post_text, ConstraintSet.TOP);
                constraintSet.clear(R.id.tv_post_text, ConstraintSet.BOTTOM);
                constraintSet.connect(R.id.tv_post_text, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                constraintSet.connect(R.id.tv_post_text, ConstraintSet.BOTTOM, R.id.action_bar, ConstraintSet.TOP);

                // 隐藏图片和音频容器
                imgContainer.setVisibility(View.GONE);
                audioContainer.setVisibility(View.GONE);

            } else if (post.hasOnlyTextAndAudio()) {
                // 只有文字和语音：语音位置向上贴近文字
                // 文字正常位置，音频紧跟文字
                constraintSet.connect(R.id.audio_container, ConstraintSet.TOP, R.id.post_meta_row, ConstraintSet.BOTTOM, (int)(4 * context.getResources().getDisplayMetrics().density));
                imgContainer.setVisibility(View.GONE);
                audioContainer.setVisibility(View.VISIBLE);

            } else {
                // 有图片或其他情况：使用原始布局
                constraintSet.connect(R.id.img_container, ConstraintSet.TOP, R.id.post_meta_row, ConstraintSet.BOTTOM);
                constraintSet.connect(R.id.audio_container, ConstraintSet.TOP, R.id.img_container, ConstraintSet.BOTTOM);
            }

            constraintSet.applyTo(rootLayout);
        }

        private void playAudio(String audioUrl, int position) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.setDataSource(audioUrl);
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    currentPlayingPosition = position;
                    notifyDataSetChanged(); // 更新UI显示播放状态
                    startProgressUpdate(waveformAudioPost, tvAudioDurationPost);
                });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "音频播放失败", Toast.LENGTH_SHORT).show();
            }
        }

        private void handleLike(PostItem post, int position) {
            // 实现点赞逻辑
            post.setLiked(!post.isLiked());
            post.setLikeCount(post.isLiked() ? post.getLikeCount() + 1 : post.getLikeCount() - 1);
            notifyItemChanged(position);
            // TODO: 调用API更新后端数据
        }

        private void handleFavorite(PostItem post, int position) {
            // 实现收藏逻辑
            post.setFavorited(!post.isFavorited());
            post.setFavoriteCount(post.isFavorited() ? post.getFavoriteCount() + 1 : post.getFavoriteCount() - 1);
            notifyItemChanged(position);
            // TODO: 调用API更新后端数据
        }

        private void handleComment(PostItem post, int position) {
            // 实现评论逻辑
            Toast.makeText(context, "评论功能待实现", Toast.LENGTH_SHORT).show();
            // TODO: 打开评论界面
        }
    }
}
