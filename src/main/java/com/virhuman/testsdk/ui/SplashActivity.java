package com.virhuman.testsdk.ui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.utils.AuthStore;

/**
 * 启动页：带进度条加载动画，虚拟人左右摇摆动画，停留2秒后进入登录/注册页；若已登录则直接进首页
 */
public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_DELAY_MS = 2000L;
    private static final long PROGRESS_ANIMATION_DURATION_MS = 1800L;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View progressFill;
    private ImageView avatarView;
    private ValueAnimator progressAnimator;
    private Animation swayAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressFill = findViewById(R.id.progress_fill);
        avatarView = findViewById(R.id.iv_avatar);
        
        // 启动虚拟人左右摇摆动画
        startSwayAnimation();
        
        // 等待布局测量完成后再启动进度条动画
        progressFill.post(() -> startProgressAnimation());
        
        // 延迟跳转：根据是否首启 & 登录态决定下一步
        handler.postDelayed(() -> {
            boolean onboardingShown = AuthStore.isOnboardingShown(this);
            Class<?> target;
            if (!onboardingShown) {
                // 首次启动，先进入引导页
                target = GuideActivity.class;
            } else {
                // 非首次，根据登录态进入 登录 / 首页
                target = AuthStore.isLoggedIn(this) ? HomeActivity.class : AuthActivity.class;
            }
            startActivity(new Intent(this, target));
            finish();
        }, SPLASH_DELAY_MS);
    }

    /**
     * 启动虚拟人左右摇摆和旋转动画
     */
    private void startSwayAnimation() {
        if (avatarView != null) {
            swayAnimation = AnimationUtils.loadAnimation(this, R.anim.sway_animation);
            // 设置动画监听器，确保动画持续运行
            swayAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // 动画开始
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // 动画结束（由于是无限循环，这里不会触发）
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // 动画重复
                }
            });
            avatarView.startAnimation(swayAnimation);
        }
    }

    /**
     * 启动进度条动画：从0%到100%
     */
    private void startProgressAnimation() {
        if (progressFill == null) {
            return;
        }
        
        // 获取父容器（FrameLayout）宽度
        ViewGroup parent = (ViewGroup) progressFill.getParent();
        if (parent == null) {
            return;
        }
        
        // 确保父容器已经测量
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(parent.getHeight(), View.MeasureSpec.EXACTLY)
        );
        
        int maxWidth = parent.getMeasuredWidth();
        if (maxWidth <= 0) {
            maxWidth = parent.getWidth();
        }
        
        if (maxWidth > 0) {
            animateProgress(maxWidth);
        } else {
            // 如果仍然无法获取宽度，使用默认值
            progressFill.postDelayed(() -> {
                ViewGroup p = (ViewGroup) progressFill.getParent();
                if (p != null && p.getWidth() > 0) {
                    animateProgress(p.getWidth());
                }
            }, 100);
        }
    }

    private void animateProgress(int maxWidth) {
        // 确保初始宽度为0
        ViewGroup.LayoutParams params = progressFill.getLayoutParams();
        params.width = 0;
        progressFill.setLayoutParams(params);
        
        // 创建动画
        progressAnimator = ValueAnimator.ofInt(0, maxWidth);
        progressAnimator.setDuration(PROGRESS_ANIMATION_DURATION_MS);
        progressAnimator.addUpdateListener(animation -> {
            int width = (Integer) animation.getAnimatedValue();
            ViewGroup.LayoutParams lp = progressFill.getLayoutParams();
            lp.width = width;
            progressFill.setLayoutParams(lp);
        });
        progressAnimator.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
        if (swayAnimation != null && avatarView != null) {
            avatarView.clearAnimation();
            swayAnimation = null;
        }
    }
}


























