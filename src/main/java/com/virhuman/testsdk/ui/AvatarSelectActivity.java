package com.virhuman.testsdk.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.config.AvatarConfig;
import com.virhuman.testsdk.net.VirtualHumanApi;
import com.virhuman.testsdk.net.VirtualHumanModels;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟人形象选择页面
 */
public class AvatarSelectActivity extends AppCompatActivity {
    private static final String TAG = "AvatarSelectActivity";
    private static final String PREF_NAME = "avatar_prefs";
    private static final String KEY_SELECTED_AVATAR_ID = "selected_avatar_id";
    private static final String KEY_SELECTED_VOICE_ID = "selected_voice_id";
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private AvatarAdapter adapter;
    private String currentSelectedAvatarId;
    private List<AvatarItem> avatarList = new ArrayList<>();
    private String pendingAvatarName = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_select);
        
        // 设置返回按钮
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        // 右上角：打开背景选择（只切换背景，不切换形象）
        View btnOpenBg = findViewById(R.id.btn_open_bg);
        if (btnOpenBg != null) {
            btnOpenBg.setOnClickListener(v -> {
                // 使用当前已选的 avatarId 作为待修改对象
                pendingAvatarId = getSelectedAvatarId(AvatarSelectActivity.this);
                pendingVoiceId = getSelectedVoiceId(AvatarSelectActivity.this);
                pendingAvatarName = getSelectedAvatarName(AvatarSelectActivity.this);
                Intent intent = new Intent(AvatarSelectActivity.this, BackgroundSelectActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SELECT_BG);
            });
        }
        
        recyclerView = findViewById(R.id.recycler_avatar_list);
        progressBar = findViewById(R.id.progress_bar);
        tvEmpty = findViewById(R.id.tv_empty);
        
        // 改为单列列表显示，每行显示一个形象
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        
        // 获取当前选中的形象
        currentSelectedAvatarId = getSelectedAvatarId(this);
        
        // 初始化适配器
        adapter = new AvatarAdapter(avatarList);
        recyclerView.setAdapter(adapter);
        
        // 从后端API获取虚拟人列表
        loadVirtualHumanList();
    }
    
    /**
     * 从后端API加载虚拟人列表
     */
    private void loadVirtualHumanList() {
        showLoading(true);
        
        VirtualHumanApi.getVirtualHumanList(this, new VirtualHumanApi.VirtualHumanListCallback() {
            @Override
            public void onSuccess(VirtualHumanModels.VirtualHumanListResult result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (result == null || !result.success) {
                        String errorMsg = result != null ? result.bestMessage() : "获取虚拟人列表失败";
                        if (TextUtils.isEmpty(errorMsg)) {
                            errorMsg = "获取虚拟人列表失败";
                        }
                        Toast.makeText(AvatarSelectActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                        showEmpty(true);
                        Log.e(TAG, "获取虚拟人列表失败: " + errorMsg);
                        return;
                    }
                    
                    if (result.data == null || result.data.length == 0) {
                        showEmpty(true);
                        Toast.makeText(AvatarSelectActivity.this, "暂无虚拟人数据", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 转换数据
                    avatarList.clear();
                    for (VirtualHumanModels.VirtualHumanItem item : result.data) {
                        if (item != null && !TextUtils.isEmpty(item.avatarId) && !TextUtils.isEmpty(item.voiceId)) {
                            avatarList.add(new AvatarItem(
                                item.avatarId,
                                item.avatarName != null ? item.avatarName : "未知",
                                item.voiceId,
                                item.number != null ? item.number : "",
                                item.imageUrl,
                                item.avatarTags != null ? item.avatarTags : "",
                                item.voiceTags != null ? item.voiceTags : ""
                            ));
                        }
                    }
                    
                    if (avatarList.isEmpty()) {
                        showEmpty(true);
                        Toast.makeText(AvatarSelectActivity.this, "虚拟人数据无效", Toast.LENGTH_SHORT).show();
                    } else {
                        showEmpty(false);
                        adapter.notifyDataSetChanged();
                        Log.d(TAG, "成功加载 " + avatarList.size() + " 个虚拟人");
                    }
                });
            }
            
            @Override
            public void onFailure(String errorMessage, Throwable t) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showEmpty(true);
                    Toast.makeText(AvatarSelectActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "获取虚拟人列表失败", t);
                });
            }
        });
    }
    
    /**
     * 显示/隐藏加载状态
     */
    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }
    
    /**
     * 显示/隐藏空状态
     */
    private void showEmpty(boolean empty) {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * 保存选中的虚拟人形象
     */
    public static void saveSelectedAvatar(Context context, String avatarId, String voiceId) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
            .putString(KEY_SELECTED_AVATAR_ID, avatarId)
            .putString(KEY_SELECTED_VOICE_ID, voiceId)
            .apply();
    }

    /**
     * 获取保存的选中形象名称
     */
    public static String getSelectedAvatarName(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SELECTED_AVATAR_ID + "_name", "小雨");
    }
    
    /**
     * 获取选中的虚拟人形象ID
     */
    public static String getSelectedAvatarId(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SELECTED_AVATAR_ID, AvatarConfig.DEFAULT_ANCHOR_ID);
    }
    
    /**
     * 获取选中的发音人ID
     */
    public static String getSelectedVoiceId(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SELECTED_VOICE_ID, AvatarConfig.DEFAULT_VCN);
    }
    
    /**
     * 虚拟人数据模型
     */
    private static class AvatarItem {
        String avatarId;
        String name;
        // 不再展示 voiceName，保留 voiceId 以便保存
        String voiceId;
        String number;
        String imageUrl;
        // 新增的标签字段（可为空）
        String avatarTags;
        String voiceTags;

        AvatarItem(String avatarId, String name, String voiceId, String number, String imageUrl, String avatarTags, String voiceTags) {
            this.avatarId = avatarId;
            this.name = name;
            this.voiceId = voiceId;
            this.number = number;
            this.imageUrl = imageUrl;
            this.avatarTags = avatarTags;
            this.voiceTags = voiceTags;
        }
    }
    
    /**
     * 适配器
     */
    private class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.ViewHolder> {
        private List<AvatarItem> avatarList;
        
        AvatarAdapter(List<AvatarItem> list) {
            this.avatarList = list;
        }
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_select, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AvatarItem item = avatarList.get(position);
            holder.tvName.setText(item.name);
            // 不再展示 voiceName；如果有 avatarTags/voiceTags 则显示标签
            if (holder.tvAvatarTags != null) {
                if (!TextUtils.isEmpty(item.avatarTags)) {
                    holder.tvAvatarTags.setText(item.avatarTags);
                    holder.tvAvatarTags.setVisibility(View.VISIBLE);
                } else {
                    holder.tvAvatarTags.setVisibility(View.GONE);
                }
            }
            if (holder.tvVoiceTags != null) {
                if (!TextUtils.isEmpty(item.voiceTags)) {
                    holder.tvVoiceTags.setText(item.voiceTags);
                    holder.tvVoiceTags.setVisibility(View.VISIBLE);
                } else {
                    holder.tvVoiceTags.setVisibility(View.GONE);
                }
            }
            
            // 加载头像图片（异步）
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            loadImage(holder.ivAvatar, item.imageUrl);
            
            // 标记当前选中的形象
            boolean isSelected = item.avatarId.equals(currentSelectedAvatarId);
            holder.itemView.setSelected(isSelected);
            holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            
            // 点击选择：立即切换形象并返回到首页（不弹出背景选择）
            View.OnClickListener selectAvatarAndReturn = v -> {
                try {
                    // 立即保存选中的 avatar 到偏好设置
                    saveSelectedAvatar(AvatarSelectActivity.this, item.avatarId, item.voiceId);
                    SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    sp.edit().putString(KEY_SELECTED_AVATAR_ID + "_name", item.name).apply();

                    // 返回结果给调用方（例如 HomeActivity）
                    Intent result = new Intent();
                    result.putExtra("avatar_id", item.avatarId);
                    result.putExtra("voice_id", item.voiceId);
                    result.putExtra("avatar_name", item.name);
                    setResult(RESULT_OK, result);
                    finish();
                } catch (Exception e) {
                    Log.w(TAG, "select avatar failed", e);
                    Toast.makeText(AvatarSelectActivity.this, "选择失败，请重试", Toast.LENGTH_SHORT).show();
                }
            };

            // 确保 item 可点击并给主要子视图都添加点击响应
            holder.itemView.setClickable(true);
            holder.itemView.setOnClickListener(selectAvatarAndReturn);
            holder.ivAvatar.setOnClickListener(selectAvatarAndReturn);
            holder.tvName.setOnClickListener(selectAvatarAndReturn);
        }
        
        @Override
        public int getItemCount() {
            return avatarList.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvAvatarTags;
            TextView tvVoiceTags;
            ImageView ivSelected;
            ImageView ivAvatar;
            
            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_avatar_name);
                // 标签视图
                tvAvatarTags = itemView.findViewById(R.id.tv_avatar_tags);
                tvVoiceTags = itemView.findViewById(R.id.tv_voice_tags);
                ivSelected = itemView.findViewById(R.id.iv_selected);
                ivAvatar = itemView.findViewById(R.id.iv_avatar_image);
            }
        }
    }
    
    // 临时保存用户点击的 avatar 信息，等待背景选择结果
    private String pendingAvatarId = null;
    private String pendingVoiceId = null;
    private static final int REQUEST_CODE_SELECT_BG = 200;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_BG) {
            if (resultCode == RESULT_OK && data != null) {
                int bgRes = data.getIntExtra(BackgroundSelectActivity.EXTRA_SELECTED_BG_RES, -1);
                // 保存选中的 avatar 与背景到 SharedPreferences（可按需改为保存 URL 等）
                if (!TextUtils.isEmpty(pendingAvatarId)) {
                    SharedPreferences sp = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(KEY_SELECTED_AVATAR_ID, pendingAvatarId);
                    editor.putString(KEY_SELECTED_VOICE_ID, pendingVoiceId != null ? pendingVoiceId : "");
                    if (!TextUtils.isEmpty(pendingAvatarName)) {
                        editor.putString(KEY_SELECTED_AVATAR_ID + "_name", pendingAvatarName);
                    }
                    if (bgRes != -1) {
                        editor.putInt(KEY_SELECTED_AVATAR_ID + "_bg_res", bgRes);
                    }
                    editor.apply();

                    // 通知调用方最终选择（包含背景）
                    Intent result = new Intent();
                    result.putExtra("avatar_id", pendingAvatarId);
                    result.putExtra("voice_id", pendingVoiceId);
                    if (!TextUtils.isEmpty(pendingAvatarName)) result.putExtra("avatar_name", pendingAvatarName);
                    if (bgRes != -1) result.putExtra("background_res", bgRes);
                    setResult(RESULT_OK, result);
                    // 结束当前页面，返回到上游（例如 HomeActivity）
                    finish();
                }
            } else {
                // 用户取消背景选择，清理 pending
                pendingAvatarId = null;
                pendingVoiceId = null;
            }
        }
    }

    /**
     * 简单的异步图片加载（不依赖第三方库），失败时显示占位图。
     * 注意：生产建议使用 Glide/Picasso 等库替代此实现以获得缓存和更好性能。
     */
    private void loadImage(ImageView imageView, String urlString) {
        if (imageView == null) return;
        if (TextUtils.isEmpty(urlString)) {
            imageView.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }

        new Thread(() -> {
            Bitmap bitmap = null;
            HttpURLConnection conn = null;
            InputStream is = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setInstanceFollowRedirects(true);
                is = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(is);
            } catch (Exception e) {
                Log.w(TAG, "加载头像失败: " + urlString, e);
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException ignored) {}
                }
                if (conn != null) conn.disconnect();
            }

            final Bitmap finalBitmap = bitmap;
            runOnUiThread(() -> {
                if (finalBitmap != null) {
                    try {
                        int bmpW = finalBitmap.getWidth();
                        int bmpH = finalBitmap.getHeight();

                        int ivW = imageView.getWidth();
                        int ivH = imageView.getHeight();
                        if (ivW == 0) {
                            // fallback to 140dp default used in layout
                            float density = imageView.getResources().getDisplayMetrics().density;
                            ivW = (int) (140 * density + 0.5f);
                        }
                        if (ivH == 0) {
                            float density = imageView.getResources().getDisplayMetrics().density;
                            ivH = (int) (140 * density + 0.5f);
                        }

                        float aspect = (float) bmpW / (float) bmpH;
                        // Choose scaleType: wide images -> centerCrop to fill horizontally,
                        // tall/normal -> centerInside to show full person
                        if (aspect > 1.3f) {
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        } else {
                            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        }

                        // Downscale large bitmaps to avoid memory pressure and to better fit view
                        int maxTarget = Math.max(ivW, ivH) * 2; // allow some headroom
                        Bitmap toSet = finalBitmap;
                        int maxBmpDim = Math.max(bmpW, bmpH);
                        if (maxBmpDim > maxTarget && maxTarget > 0) {
                            float scale = (float) maxTarget / (float) maxBmpDim;
                            int newW = Math.max(1, Math.round(bmpW * scale));
                            int newH = Math.max(1, Math.round(bmpH * scale));
                            try {
                                toSet = Bitmap.createScaledBitmap(finalBitmap, newW, newH, true);
                            } catch (OutOfMemoryError oom) {
                                Log.w(TAG, "Bitmap downscale OOM, using original", oom);
                                toSet = finalBitmap;
                            }
                        }

                        imageView.setImageBitmap(toSet);
                    } catch (Exception e) {
                        Log.w(TAG, "set bitmap failed, fallback to placeholder", e);
                        imageView.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                } else {
                    imageView.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            });
        }).start();
    }
}

