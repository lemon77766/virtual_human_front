package com.virhuman.testsdk.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.virhuman.testsdk.R;

/**
 * Helper to manage a bottom comment input overlay that appears above the keyboard,
 * handles outside touch to dismiss, and delivers sent text via callback.
 */
public class CommentOverlayManager {
    public interface OnSendListener {
        void onSend(@NonNull String text);
    }

    private final Activity activity;
    private FrameLayout overlay;
    private EditText input;
    private ImageButton sendBtn;
    private ViewGroup root;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardListener;
    private int lastKeyboardHeight = 0;

    public CommentOverlayManager(Activity activity) {
        this.activity = activity;
    }

    public void show(OnSendListener listener) {
        try {
            // attach to decor view so overlay floats above bottom navigation and other UI
            root = (ViewGroup) activity.getWindow().getDecorView();
            if (root == null) return;

            if (overlay == null) {
                overlay = new FrameLayout(activity);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.BOTTOM | Gravity.START;
                root.addView(overlay, lp);
            } else {
                overlay.removeAllViews();
                // ensure bottom gravity
                ViewGroup.LayoutParams params = overlay.getLayoutParams();
                if (params instanceof FrameLayout.LayoutParams) {
                    ((FrameLayout.LayoutParams) params).gravity = Gravity.BOTTOM | Gravity.START;
                    overlay.setLayoutParams(params);
                }
            }

            float density = activity.getResources().getDisplayMetrics().density;
            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.HORIZONTAL);
            container.setBackgroundResource(R.drawable.bg_input);
            int pad = (int) (8 * density);
            container.setPadding(pad, pad, pad, pad);

            ImageButton mic = new ImageButton(activity);
            mic.setBackgroundResource(R.drawable.btn_voice_background);
            mic.setImageResource(R.drawable.ic_mic);
            LinearLayout.LayoutParams micLp = new LinearLayout.LayoutParams((int)(48*density),(int)(48*density));
            container.addView(mic, micLp);

            input = new EditText(activity);
            input.setHint("评论一句...");
            input.setBackground(null);
            LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            inputLp.setMargins((int)(8*density),0,(int)(8*density),0);
            container.addView(input, inputLp);

            sendBtn = new ImageButton(activity);
            sendBtn.setBackgroundResource(R.drawable.bg_icon_button);
            sendBtn.setImageResource(R.drawable.ic_send);
            LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams((int)(44*density),(int)(44*density));
            container.addView(sendBtn, sendLp);

            FrameLayout.LayoutParams containerLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            containerLp.gravity = Gravity.BOTTOM;
            overlay.addView(container, containerLp);
            overlay.setVisibility(View.VISIBLE);
            overlay.bringToFront();

            // show keyboard
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);

            sendBtn.setOnClickListener(v -> {
                if (input == null) return;
                String text = input.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(activity, "请输入评论内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                listener.onSend(text);
                hide();
            });

            // add an invisible interceptor view behind the input layout to catch outside touches
            View interceptor = new View(activity);
            interceptor.setClickable(true);
            FrameLayout.LayoutParams fullLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            overlay.addView(interceptor, 0, fullLp);
            interceptor.setOnTouchListener((v, ev) -> {
                if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        if (input != null) input.clearFocus();
                        // reuse existing imm variable declared earlier if available
                        InputMethodManager im = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (im != null) im.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
                    } catch (Exception ignored) {}
                    hide();
                    return true;
                }
                return false;
            });

            // listen to global layout changes to detect keyboard height and move overlay above keyboard
            keyboardListener = () -> {
                try {
                    Rect r = new Rect();
                    root.getWindowVisibleDisplayFrame(r);
                    int screenHeight = root.getRootView().getHeight();
                    int keyboardHeight = screenHeight - r.bottom;
                    // only act when height changed
                    if (keyboardHeight != lastKeyboardHeight) {
                        lastKeyboardHeight = keyboardHeight;
                        if (keyboardHeight > screenHeight * 0.15f) {
                            // keyboard visible: move overlay up by keyboardHeight
                            overlay.setTranslationY(-keyboardHeight);
                        } else {
                            // keyboard hidden: reset
                            overlay.setTranslationY(0);
                        }
                    }
                } catch (Exception ignored) {}
            };
            root.getViewTreeObserver().addOnGlobalLayoutListener(keyboardListener);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void hide() {
        try {
            if (overlay != null) {
                overlay.removeAllViews();
                overlay.setVisibility(View.GONE);
            }
            if (input != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
            }
            // clear listener
            if (root != null) {
                try { root.setOnTouchListener(null); } catch (Exception ignored) {}
                try { 
                    if (keyboardListener != null) {
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardListener);
                        keyboardListener = null;
                    }
                } catch (Exception ignored) {}
            }
            input = null;
            sendBtn = null;
        } catch (Throwable ignored) {}
    }
}


