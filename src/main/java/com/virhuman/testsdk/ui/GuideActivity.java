package com.virhuman.testsdk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.utils.AuthStore;

/**
 * 首次启动引导页
 *
 * 流程：
 * - 首次启动：Splash -> GuideActivity -> 登录 / 首页
 * - 非首次：Splash 直接根据登录态进入 登录 / 首页
 */
public class GuideActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private View dot1;
    private View dot2;
    private View dot3;
    private TextView btnSkip;
    private TextView btnNext;

    private int[] pageTitles = {
            R.string.guide_title_companion,
            R.string.guide_title_enjoy_life,
            R.string.guide_title_community
    };

    private int[] pageSubtitles = {
            R.string.guide_desc_companion,
            R.string.guide_desc_enjoy_life,
            R.string.guide_desc_community
    };

    private int[] pageIcons = {
            R.drawable.ic_logo_simple,
            R.drawable.ic_history,
            R.drawable.ic_notification_icon
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_guide);

            viewPager = findViewById(R.id.view_pager);
            dot1 = findViewById(R.id.dot_1);
            dot2 = findViewById(R.id.dot_2);
            dot3 = findViewById(R.id.dot_3);
            btnSkip = findViewById(R.id.btn_skip);
            btnNext = findViewById(R.id.btn_next);

            // 检查所有视图是否成功初始化
            if (viewPager == null || dot1 == null || dot2 == null || dot3 == null 
                    || btnSkip == null || btnNext == null) {
                throw new IllegalStateException("Failed to initialize views in GuideActivity");
            }

            // 设置 offscreenPageLimit 为 1，减少预加载页面数量
            viewPager.setOffscreenPageLimit(1);
            
            // 延迟设置 adapter，避免在 onCreate 中阻塞主线程
            viewPager.post(() -> {
                try {
                    viewPager.setAdapter(new GuideAdapter());
                    viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageSelected(int position) {
                            super.onPageSelected(position);
                            updateDots(position);
                            updateNextButton(position);
                        }
                    });
                    
                    // 初始状态
                    updateDots(0);
                    updateNextButton(0);
                } catch (Exception e) {
                    e.printStackTrace();
                    completeOnboarding();
                }
            });

            btnSkip.setOnClickListener(v -> completeOnboarding());
            btnNext.setOnClickListener(v -> {
                int position = viewPager.getCurrentItem();
                if (position < pageTitles.length - 1) {
                    viewPager.setCurrentItem(position + 1, true);
                } else {
                    completeOnboarding();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // 如果引导页加载失败，直接跳转到登录页
            completeOnboarding();
        }
    }

    private void updateDots(int position) {
        dot1.setSelected(position == 0);
        dot2.setSelected(position == 1);
        dot3.setSelected(position == 2);
    }

    private void updateNextButton(int position) {
        if (position == pageTitles.length - 1) {
            btnNext.setText(R.string.guide_action_enter);
        } else {
            btnNext.setText(R.string.guide_action_next);
        }
    }

    /**
     * 完成引导：标记已展示，并根据登录态跳转
     */
    private void completeOnboarding() {
        AuthStore.setOnboardingShown(this, true);
        Class<?> target = AuthStore.isLoggedIn(this) ? HomeActivity.class : AuthActivity.class;
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private class GuideAdapter extends RecyclerView.Adapter<GuideViewHolder> {

        @NonNull
        @Override
        public GuideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_guide_page, parent, false);
            return new GuideViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GuideViewHolder holder, int position) {
            holder.bind(pageIcons[position], pageTitles[position], pageSubtitles[position]);
        }

        @Override
        public int getItemCount() {
            return pageTitles.length;
        }
    }

    private static class GuideViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;

        GuideViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.iv_icon);
            titleView = itemView.findViewById(R.id.tv_title);
            subtitleView = itemView.findViewById(R.id.tv_subtitle);
        }

        void bind(int iconRes, int titleRes, int subtitleRes) {
            iconView.setImageResource(iconRes);
            titleView.setText(titleRes);
            subtitleView.setText(subtitleRes);
        }
    }
}


