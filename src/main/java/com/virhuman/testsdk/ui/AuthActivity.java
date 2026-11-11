package com.virhuman.testsdk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.net.AuthApi;
import com.virhuman.testsdk.net.LoginModels;
import com.virhuman.testsdk.utils.AuthStore;

import java.util.regex.Pattern;

/**
 * 登录/注册页 - 支持验证码登录和密码登录
 */
public class AuthActivity extends AppCompatActivity {
    // 登录模式：true=验证码登录，false=密码登录
    private boolean isVerifyCodeMode = true;

    private ImageView btnBack;
    private TextView tabVerifyCode;
    private TextView tabPassword;
    private EditText etAccount;
    private EditText etPassword;
    private EditText etVerifyCode;
    private ConstraintLayout llVerifyCodeInput;
    private ConstraintLayout llPasswordInput;
    private CheckBox cbAgreement;
    private TextView tvUserAgreement;
    private TextView tvPrivacyPolicy;
    private Button btnSubmit;
    private TextView btnSendSms;
    private TextView tvForgotPassword;
    private ImageView ivWechat;
    private ImageView ivQq;
    private android.widget.FrameLayout flWechat;
    private android.widget.FrameLayout flQq;
    
    // 验证码倒计时相关
    private static final int COUNTDOWN_TIME = 60; // 60秒
    private int countdown = 0;
    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable;
    
    // 手机号验证正则
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    // 请求码：用户协议
    private static final int REQUEST_USER_AGREEMENT = 0x1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_auth);

            initViews();
            setListeners();
            setLoginMode(true); // 默认验证码登录
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("AuthActivity", "Error in onCreate", e);
            // 如果加载失败，显示错误提示
            android.widget.Toast.makeText(this, "登录页面加载失败: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tabVerifyCode = findViewById(R.id.tab_verify_code);
        tabPassword = findViewById(R.id.tab_password);
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        etVerifyCode = findViewById(R.id.et_verify_code);
        llVerifyCodeInput = findViewById(R.id.ll_verify_code_input);
        llPasswordInput = findViewById(R.id.ll_password_input);
        cbAgreement = findViewById(R.id.cb_agreement);
        tvUserAgreement = findViewById(R.id.tv_user_agreement);
        tvPrivacyPolicy = findViewById(R.id.tv_privacy_policy);
        btnSubmit = findViewById(R.id.btn_submit);
        btnSendSms = findViewById(R.id.btn_send_sms);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        ivWechat = findViewById(R.id.iv_wechat);
        ivQq = findViewById(R.id.iv_qq);
        flWechat = findViewById(R.id.fl_wechat);
        flQq = findViewById(R.id.fl_qq);
    }

    private void setListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        
        if (tabVerifyCode != null) {
            tabVerifyCode.setOnClickListener(v -> setLoginMode(true));
        }
        if (tabPassword != null) {
            tabPassword.setOnClickListener(v -> setLoginMode(false));
        }
        
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submit());
        }
        if (btnSendSms != null) {
            btnSendSms.setOnClickListener(v -> sendSmsCode());
        }
        
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Toast.makeText(this, "找回密码功能待实现", Toast.LENGTH_SHORT).show();
            });
        }
        
        if (tvUserAgreement != null) {
            tvUserAgreement.setOnClickListener(v -> {
                // 打开用户协议页面并等待同意结果
                android.content.Intent intent = new android.content.Intent(AuthActivity.this, UserAgreementActivity.class);
                startActivityForResult(intent, REQUEST_USER_AGREEMENT);
            });
        }
        
        if (tvPrivacyPolicy != null) {
            tvPrivacyPolicy.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(AuthActivity.this, UserAgreementActivity.class);
                startActivityForResult(intent, REQUEST_USER_AGREEMENT);
            });
        }
        
        // 微信和QQ登录按钮
        if (flWechat != null) {
            flWechat.setOnClickListener(v -> {
                Toast.makeText(this, "微信登录功能待实现", Toast.LENGTH_SHORT).show();
                // TODO: 实现微信登录
            });
        }
        
        if (flQq != null) {
            flQq.setOnClickListener(v -> {
                Toast.makeText(this, "QQ登录功能待实现", Toast.LENGTH_SHORT).show();
                // TODO: 实现QQ登录
            });
        }
    }

    /**
     * 设置登录模式
     * @param verifyCodeMode true=验证码登录，false=密码登录
     */
    private void setLoginMode(boolean verifyCodeMode) {
        isVerifyCodeMode = verifyCodeMode;
        
        // 更新标签选中状态（文字颜色会通过selector自动更新）
        tabVerifyCode.setSelected(verifyCodeMode);

        tabPassword.setSelected(!verifyCodeMode);
        // Use a ColorStateList so text color follows selected state reliably.
        try {
            android.content.res.ColorStateList csl = androidx.core.content.ContextCompat.getColorStateList(this, R.color.bg_auth_mode_text);
            if (tabVerifyCode != null) {
                tabVerifyCode.setTextColor(csl);
                tabVerifyCode.setTypeface(null, verifyCodeMode ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
            if (tabPassword != null) {
                tabPassword.setTextColor(csl);
                tabPassword.setTypeface(null, !verifyCodeMode ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        } catch (Exception e) {
            // Fallback to explicit colors if ColorStateList not available
            int colorSelected = android.graphics.Color.parseColor("#FFFFFF");
            int colorUnselected = android.graphics.Color.parseColor("#FF5A5F");
            if (tabVerifyCode != null) {
                tabVerifyCode.setTextColor(verifyCodeMode ? colorSelected : colorUnselected);
                tabVerifyCode.setTypeface(null, verifyCodeMode ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
            if (tabPassword != null) {
                tabPassword.setTextColor(!verifyCodeMode ? colorSelected : colorUnselected);
                tabPassword.setTypeface(null, !verifyCodeMode ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
        }
        
        // 显示/隐藏对应的输入框
        llVerifyCodeInput.setVisibility(verifyCodeMode ? View.VISIBLE : View.GONE);
        llPasswordInput.setVisibility(verifyCodeMode ? View.GONE : View.VISIBLE);
        
        // 更新手机号输入框提示
        if (verifyCodeMode) {
            etAccount.setHint(R.string.auth_hint_phone);
            etAccount.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        } else {
            etAccount.setHint(R.string.auth_hint_account);
            etAccount.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        }
    }

    private void submit() {
        // 检查协议是否勾选
        if (!cbAgreement.isChecked()) {
            Toast.makeText(this, R.string.auth_agreement_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String verifyCode = etVerifyCode.getText().toString().trim();

        if (isVerifyCodeMode) {
            // 验证码登录模式
            if (TextUtils.isEmpty(account)) {
                Toast.makeText(this, R.string.auth_phone_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!PHONE_PATTERN.matcher(account).matches()) {
                Toast.makeText(this, R.string.auth_phone_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(verifyCode)) {
                Toast.makeText(this, R.string.auth_verify_code_required, Toast.LENGTH_SHORT).show();
                return;
            }
            
            setLoading(true);
            AuthApi.login(account, verifyCode, new AuthApi.LoginCallback() {
                @Override
                public void onSuccess(LoginModels.LoginResult result) {
                    runOnUiThread(() -> {
                        setLoading(false);

                        // 判断登录是否成功：success为true 或 code为200/0，且有token
                        boolean isSuccess = (result.success || result.code == 200 || result.code == 0);
                        
                        if (!isSuccess || result.data == null || TextUtils.isEmpty(result.data.token)) {
                            String msg = result.bestMessage();
                            if (TextUtils.isEmpty(msg)) {
                                if (result.code != 0 && result.code != 200) {
                                    msg = "登录失败 (错误码: " + result.code + ")";
                                } else {
                                    msg = "登录失败";
                                }
                            }
                            Toast.makeText(AuthActivity.this, msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        AuthStore.saveAuth(AuthActivity.this,
                                result.data.token,
                                result.data.userId,
                                result.data.account,
                                result.data.username
                        );

                        String msg = result.bestMessage();
                        Toast.makeText(AuthActivity.this, TextUtils.isEmpty(msg) ? "登录成功" : msg, Toast.LENGTH_SHORT).show();
                        goHome();
                    });
                }

                @Override
                public void onFailure(String errorMessage, Throwable t) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        // 显示更详细的错误信息
                        String displayMsg = errorMessage;
                        if (t != null) {
                            android.util.Log.e("AuthActivity", "验证码登录失败", t);
                        }
                        Toast.makeText(AuthActivity.this, displayMsg, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            // 密码登录模式
            if (TextUtils.isEmpty(account)) {
                Toast.makeText(this, R.string.auth_phone_required, Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, R.string.auth_password_required, Toast.LENGTH_SHORT).show();
                return;
            }
            
            setLoading(true);
            AuthApi.loginByPassword(account, password, new AuthApi.LoginCallback() {
                @Override
                public void onSuccess(LoginModels.LoginResult result) {
                    runOnUiThread(() -> {
                        setLoading(false);

                        // 判断登录是否成功：success为true 或 code为200/0，且有token
                        boolean isSuccess = (result.success || result.code == 200 || result.code == 0);
                        
                        if (!isSuccess || result.data == null || TextUtils.isEmpty(result.data.token)) {
                            String msg = result.bestMessage();
                            if (TextUtils.isEmpty(msg)) {
                                if (result.code != 0 && result.code != 200) {
                                    msg = "登录失败 (错误码: " + result.code + ")";
                                } else {
                                    msg = "登录失败";
                                }
                            }
                            Toast.makeText(AuthActivity.this, msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        AuthStore.saveAuth(AuthActivity.this,
                                result.data.token,
                                result.data.userId,
                                result.data.account,
                                result.data.username
                        );

                        String msg = result.bestMessage();
                        Toast.makeText(AuthActivity.this, TextUtils.isEmpty(msg) ? "登录成功" : msg, Toast.LENGTH_SHORT).show();
                        goHome();
                    });
                }

                @Override
                public void onFailure(String errorMessage, Throwable t) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        // 显示更详细的错误信息
                        String displayMsg = errorMessage;
                        if (t != null) {
                            android.util.Log.e("AuthActivity", "密码登录失败", t);
                        }
                        Toast.makeText(AuthActivity.this, displayMsg, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    /**
     * 发送短信验证码
     */
    private void sendSmsCode() {
        String phone = etAccount.getText().toString().trim();
        
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, R.string.auth_phone_required, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            Toast.makeText(this, R.string.auth_phone_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (countdown > 0) {
            Toast.makeText(this, "请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        btnSendSms.setEnabled(false);
        AuthApi.sendSms(this, phone, new AuthApi.SendSmsCallback() {
            @Override
            public void onSuccess(LoginModels.SendSmsResult result) {
                runOnUiThread(() -> {
                    btnSendSms.setEnabled(true);
                    if (result.success) {
                        String msg = result.bestMessage();
                        Toast.makeText(AuthActivity.this, 
                            TextUtils.isEmpty(msg) ? getString(R.string.auth_verify_code_sent) : msg, 
                            Toast.LENGTH_SHORT).show();
                        startCountdown();
                    } else {
                        String msg = result.bestMessage();
                        Toast.makeText(AuthActivity.this, 
                            TextUtils.isEmpty(msg) ? "验证码发送失败" : msg, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(String errorMessage, Throwable t) {
                runOnUiThread(() -> {
                    btnSendSms.setEnabled(true);
                    Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 开始倒计时
     */
    private void startCountdown() {
        if (btnSendSms == null) return;
        
        countdown = COUNTDOWN_TIME;
        btnSendSms.setEnabled(false);
        
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (btnSendSms == null) return;
                
                if (countdown > 0) {
                    btnSendSms.setText(getString(R.string.auth_verify_code_countdown, countdown));
                    countdown--;
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    btnSendSms.setText(R.string.auth_get_verify_code);
                    btnSendSms.setEnabled(true);
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }
    
    /**
     * 停止倒计时
     */
    private void stopCountdown() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
        countdown = 0;
        if (btnSendSms != null) {
            btnSendSms.setText(R.string.auth_get_verify_code);
            btnSendSms.setEnabled(true);
        }
    }

    private void setLoading(boolean loading) {
        if (btnSubmit != null) {
            btnSubmit.setEnabled(!loading);
            btnSubmit.setText(loading ? R.string.auth_logging_in : R.string.auth_login);
        }
        if (tabVerifyCode != null) {
            tabVerifyCode.setEnabled(!loading);
        }
        if (tabPassword != null) {
            tabPassword.setEnabled(!loading);
        }
        if (etAccount != null) {
            etAccount.setEnabled(!loading);
        }
        if (isVerifyCodeMode) {
            if (etVerifyCode != null) {
                etVerifyCode.setEnabled(!loading);
            }
            if (!loading && countdown == 0 && btnSendSms != null) {
                btnSendSms.setEnabled(true);
            }
        } else {
            if (etPassword != null) {
                etPassword.setEnabled(!loading);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_USER_AGREEMENT) {
            if (resultCode == RESULT_OK) {
                if (cbAgreement != null) {
                    cbAgreement.setChecked(true);
                }
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdown();
    }

    private void goHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
