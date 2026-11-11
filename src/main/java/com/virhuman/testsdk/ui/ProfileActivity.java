package com.virhuman.testsdk.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import com.virhuman.testsdk.R;
import com.virhuman.testsdk.net.UserApi;
import com.virhuman.testsdk.utils.AuthStore;
import android.text.TextUtils;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;


/**
 * Bottom navigation - profile center with rich UI cards.
 */
public class ProfileActivity extends AppCompatActivity {

    private View navHome;
    private View navFeatures;
    private View navMessages;
    private View navProfile;

    private TextView tvUserName;
    private TextView tvUsageDays;
    private TextView tvFavoritesCount;
    private TextView tvFontPreview;
    private TextView tvVolumeValue;
    private TextView optionFontStandard;
    private TextView optionFontLarge;
    private TextView optionFontExtra;
    private TextView optionFontHuge;
    private SeekBar volumeSeekBar;
    private ImageButton btnEditProfile;

    private View btnEnjoyLife;
    private View btnEmergencyCall;
    private View btnWatchTutorial;
    private View btnFamilyBinding;
    private View rowNotifications;
    private View rowPrivacy;
    private View rowAbout;
    private View rowLogout;
    
    // 用户信息数据
    private UserApi.UserInfoResponse.UserData userData;
    private static final int REQ_CAMERA = 0x4001;
    private static final int REQ_PICK = 0x4002;
    private static final int REQ_PERM_CAMERA = 0x5001;
    private static final int REQ_PERM_READ = 0x5002;
    private static final String TAG = "ProfileActivity";
    // 临时保存用户在编辑对话框中选择但尚未上传的头像文件
    private java.io.File pendingAvatarFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initNavigationViews();
        initProfileContent();
        initActionCards();
        setNavSelected(navProfile, true);
        
        // 调用用户信息API获取数据
        fetchUserInfo();
    }
    
    /**
     * 调用用户信息API获取数据
     */
    private void fetchUserInfo() {
        // 获取当前用户手机号（从AuthStore读取）
        String phone = AuthStore.getAccount(this);
        
        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "未找到用户手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        
        UserApi.getUserInfo(this, phone, new UserApi.UserInfoCallback() {
            @Override
            public void onSuccess(UserApi.UserInfoResponse response) {
                runOnUiThread(() -> {
                    if (response != null && response.data != null) {
                        // 更新UI显示用户信息
                        updateProfileUI(response.data);
                    } else {
                        Toast.makeText(ProfileActivity.this, "获取用户信息失败：数据为空", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(String errorMessage, Throwable t) {
                runOnUiThread(() -> {
                    // 显示详细错误信息，方便调试
                    String fullErrorMessage = "获取用户信息失败：" + errorMessage;
                    if (t != null && t.getMessage() != null) {
                        fullErrorMessage += "\n错误详情：" + t.getMessage();
                    }
                    Toast.makeText(ProfileActivity.this, fullErrorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * 更新用户信息到UI
     */
    private void updateProfileUI(UserApi.UserInfoResponse.UserData userData) {
        if (userData == null) return;
        
        // 保存用户数据
        this.userData = userData;
        
        // 更新用户名
        if (userData.username != null && !userData.username.isEmpty()) {
            tvUserName.setText(userData.username);
        }
        
        // 更新头像（header）
        try {
            ImageView headerAvatar = findViewById(R.id.img_avatar);
            if (headerAvatar != null) {
                String avatarUrl = userData.avatar;
                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    Glide.with(this)
                            .load(avatarUrl)
                            .apply(new RequestOptions().placeholder(R.drawable.ic_profile_avatar_placeholder).error(R.drawable.ic_profile_avatar_placeholder).circleCrop())
                            .into(headerAvatar);
                } else {
                    headerAvatar.setImageResource(R.drawable.ic_profile_avatar_placeholder);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh SeekBar to match current system volume in case it changed externally
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null && volumeSeekBar != null && tvVolumeValue != null) {
                int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                volumeSeekBar.setMax(maxVol);
                volumeSeekBar.setProgress(curVol);
                int percent = maxVol > 0 ? (curVol * 100 / maxVol) : 0;
                tvVolumeValue.setText(percent + "%");
            }
        } catch (Exception ignored) {}
    }

    private void initNavigationViews() {
        navHome = findViewById(R.id.nav_home);
        navFeatures = findViewById(R.id.nav_features);
        navMessages = findViewById(R.id.nav_messages);
        navProfile = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> navigateTo(HomeActivity.class, navHome));
        navFeatures.setOnClickListener(v -> navigateTo(FeaturesActivity.class, navFeatures));
        navMessages.setOnClickListener(v -> navigateTo(MessagesActivity.class, navMessages));
        navProfile.setOnClickListener(v -> setNavSelected(navProfile, true));
    }

    private void initProfileContent() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvUsageDays = findViewById(R.id.tv_usage_days);
        tvFavoritesCount = findViewById(R.id.tv_favorites_count);
        tvFontPreview = findViewById(R.id.tv_font_preview);
        tvVolumeValue = findViewById(R.id.tv_volume_value);
        optionFontStandard = findViewById(R.id.option_font_standard);
        optionFontLarge = findViewById(R.id.option_font_large);
        optionFontExtra = findViewById(R.id.option_font_extra);
        optionFontHuge = findViewById(R.id.option_font_huge);
        volumeSeekBar = findViewById(R.id.seek_volume);
        btnEditProfile = findViewById(R.id.btn_edit_profile);

        String userName = "张大爷";
        int usedDays = 30;
        int favoriteCount = 12;
        tvUserName.setText(userName);
        tvUsageDays.setText(getString(R.string.profile_used_days, usedDays));
        tvFavoritesCount.setText(getString(R.string.profile_my_likes, favoriteCount));

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        View.OnClickListener fontClickListener = v -> {
            if (v == optionFontStandard) {
                applyFontSelection(optionFontStandard, 14f, 1.0f);
            } else if (v == optionFontLarge) {
                applyFontSelection(optionFontLarge, 20f, 1.05f);
            } else if (v == optionFontExtra) {
                applyFontSelection(optionFontExtra, 24f, 1.15f);
            } else if (v == optionFontHuge) {
                applyFontSelection(optionFontHuge, 30f, 1.25f);
            }
        };
        optionFontStandard.setOnClickListener(fontClickListener);
        optionFontLarge.setOnClickListener(fontClickListener);
        optionFontExtra.setOnClickListener(fontClickListener);
        optionFontHuge.setOnClickListener(fontClickListener);
        // 根据已保存的字体缩放决定默认选中项（不弹出重启对话框）
        float savedScale = com.virhuman.testsdk.utils.FontUtils.getSavedFontScale(this);
        if (Math.abs(savedScale - 1.05f) < 0.01f) {
            setFontOptionWithoutPrompt(optionFontLarge, 20f, 1.05f);
        } else if (Math.abs(savedScale - 1.15f) < 0.01f) {
            setFontOptionWithoutPrompt(optionFontExtra, 24f, 1.15f);
        } else if (Math.abs(savedScale - 1.25f) < 0.01f) {
            setFontOptionWithoutPrompt(optionFontHuge, 30f, 1.25f);
        } else {
            // 默认标准字体（1.0），不弹框
            setFontOptionWithoutPrompt(optionFontStandard, 14f, 1.0f);
        }

        // Sync SeekBar with system STREAM_MUSIC volume
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volumeSeekBar.setMax(maxVol);
            volumeSeekBar.setProgress(curVol);
            int percent = maxVol > 0 ? (curVol * 100 / maxVol) : 0;
            tvVolumeValue.setText(percent + "%");

            volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                        } catch (Exception ignored) {}
                    }
                    int p = maxVol > 0 ? (progress * 100 / maxVol) : 0;
                    tvVolumeValue.setText(p + "%");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        } else {
            // Fallback: keep previous behavior
            volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    tvVolumeValue.setText(progress + "%");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            volumeSeekBar.setProgress(70);
        }
    }

    private void initActionCards() {
        btnEnjoyLife = findViewById(R.id.btn_enjoy_life);
        btnEmergencyCall = findViewById(R.id.btn_emergency_call);
        btnWatchTutorial = findViewById(R.id.btn_watch_tutorial);
        btnFamilyBinding = findViewById(R.id.btn_family_binding);
        rowNotifications = findViewById(R.id.row_notifications);
        rowPrivacy = findViewById(R.id.row_privacy);
        rowAbout = findViewById(R.id.row_about);
        rowLogout = findViewById(R.id.row_logout);

        // "我的喜欢"：跳转到新的我的收藏页面，不再进入悦享生活
        btnEnjoyLife.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });


        View.OnClickListener emergencyClickListener = v -> {
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:120"));
            startActivity(dialIntent);
        };
        btnEmergencyCall.setOnClickListener(emergencyClickListener);

        View.OnClickListener tutorialClickListener = v -> {
            String url = "https://v.qq.com/x/cover/f5k9rkj2ds0sufw/e018333yneq.html";
            android.content.Intent intent = new android.content.Intent(ProfileActivity.this, VideoTutorialActivity.class);
            intent.putExtra(VideoTutorialActivity.EXTRA_URL, url);
            startActivity(intent);
        };
        btnWatchTutorial.setOnClickListener(tutorialClickListener);

        View.OnClickListener familyBindingClickListener = v -> Toast.makeText(this, "进入家人绑定", Toast.LENGTH_SHORT).show();
        btnFamilyBinding.setOnClickListener(familyBindingClickListener);

        View.OnClickListener notificationsClickListener = v -> Toast.makeText(this, R.string.profile_notifications, Toast.LENGTH_SHORT).show();
        rowNotifications.setOnClickListener(notificationsClickListener);

        View.OnClickListener privacyClickListener = v -> Toast.makeText(this, R.string.profile_privacy, Toast.LENGTH_SHORT).show();
        rowPrivacy.setOnClickListener(privacyClickListener);

        View.OnClickListener aboutClickListener = v -> {
            // 打开关于我们页面
            startActivity(new android.content.Intent(ProfileActivity.this, AboutActivity.class));
        };
        rowAbout.setOnClickListener(aboutClickListener);

        // 点击退出：弹出确认对话框（样式和字体重启弹框一致）
        rowLogout.setOnClickListener(v -> showLogoutConfirmDialog());
    }

    private void showLogoutConfirmDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_restart_confirm, null);
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.dialog_message);
        android.widget.TextView btnLater = dialogView.findViewById(R.id.btn_later);
        android.widget.TextView btnRestart = dialogView.findViewById(R.id.btn_restart);

        tvTitle.setText("提示");
        tvMessage.setText("确定您要退出登录吗？");
        btnLater.setText("暂不");
        btnRestart.setText("确认");

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnLater.setOnClickListener(v -> dialog.dismiss());
        btnRestart.setOnClickListener(v -> {
            dialog.dismiss();
            performLogout();
        });
        dialog.show();
    }

    private void performLogout() {
        UserApi.logout(ProfileActivity.this, new UserApi.UserInfoCallback() {
            @Override
            public void onSuccess(UserApi.UserInfoResponse response) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, response.message != null ? response.message : "退出登录成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onFailure(String errorMessage, Throwable t) {
                runOnUiThread(() -> {
                    String fullErrorMessage = "退出登录失败：" + errorMessage;
                    if (t != null && t.getMessage() != null) {
                        fullErrorMessage += "\n错误详情：" + t.getMessage();
                    }
                    Toast.makeText(ProfileActivity.this, fullErrorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void applyFontSelection(TextView selectedOption, float previewSizeSp, float scale) {
        setOptionSelected(optionFontLarge, optionFontLarge == selectedOption);
        setOptionSelected(optionFontExtra, optionFontExtra == selectedOption);
        setOptionSelected(optionFontHuge, optionFontHuge == selectedOption);
        tvFontPreview.setTextSize(previewSizeSp);
        tvFontPreview.setText(getString(R.string.profile_font_preview) + " · " + selectedOption.getText());
        // 保存并应用全局字体缩放（保存设置，重启由用户确认）
        com.virhuman.testsdk.utils.FontUtils.setFontScale(this, scale);

        // 使用自定义样式的确认弹框（模仿示例图）
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_restart_confirm, null);
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.dialog_title);
        android.widget.TextView tvMessage = dialogView.findViewById(R.id.dialog_message);
        android.widget.TextView btnLater = dialogView.findViewById(R.id.btn_later);
        android.widget.TextView btnRestart = dialogView.findViewById(R.id.btn_restart);

        tvTitle.setText("提示");
        tvMessage.setText("需要您重新应用以应用字体设置，立即重启？");

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnLater.setOnClickListener(v -> {
            // 仅保存设置，不重启
            dialog.dismiss();
        });

        btnRestart.setOnClickListener(v -> {
            dialog.dismiss();
            com.virhuman.testsdk.utils.AppUtils.restartApp(ProfileActivity.this);
        });

        dialog.show();
    }

    /**
     * 设置字体选项但不弹出重启提示（用于初始化或仅保存设置）
     */
    private void setFontOptionWithoutPrompt(TextView selectedOption, float previewSizeSp, float scale) {
        setOptionSelected(optionFontLarge, optionFontLarge == selectedOption);
        setOptionSelected(optionFontExtra, optionFontExtra == selectedOption);
        setOptionSelected(optionFontHuge, optionFontHuge == selectedOption);
        tvFontPreview.setTextSize(previewSizeSp);
        tvFontPreview.setText(getString(R.string.profile_font_preview) + " · " + selectedOption.getText());
        // 仅在界面上反映当前选中项，不在此处修改全局 fontScale（避免进入页面时触发重启或重绘）
        // 全局实际的 fontScale 在应用启动时由 DemoApplication.applyFontScale() 生效，
        // 保存/应用设置应由用户在点击选项时触发（applyFontSelection 会保存并弹框）。
    }

    private void setOptionSelected(TextView option, boolean selected) {
        option.setSelected(selected);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            option.setTextColor(selected ? getColor(android.R.color.white) : getColor(R.color.profile_text_title));
        }
    }

    private void navigateTo(Class<?> target, View clickedNav) {
        setNavSelected(clickedNav, true);
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setNavSelected(View selectedView, boolean selected) {
        navHome.setSelected(selectedView == navHome);
        navFeatures.setSelected(selectedView == navFeatures);
        navMessages.setSelected(selectedView == navMessages);
        navProfile.setSelected(selectedView == navProfile);
    }

    /**
     * Show edit profile dialog with user information.
     */
    private void showEditProfileDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        // Find dialog views
        ImageButton btnCloseDialog = dialogView.findViewById(R.id.btn_close_dialog);
        ImageView imgDialogAvatar = dialogView.findViewById(R.id.img_dialog_avatar);
        ImageButton btnEditAvatar = dialogView.findViewById(R.id.btn_edit_avatar);
        EditText etNickname = dialogView.findViewById(R.id.et_nickname);
        EditText etRealName = dialogView.findViewById(R.id.et_real_name);
        EditText etBirthday = dialogView.findViewById(R.id.et_birthday);
        EditText etPhone = dialogView.findViewById(R.id.et_phone);
        EditText etPassword = dialogView.findViewById(R.id.et_password);
        View btnSaveChanges = dialogView.findViewById(R.id.btn_save_changes);

        // Populate initial data from API
        if (userData != null) {
            etNickname.setText(userData.username != null ? userData.username : "");
            etRealName.setText(userData.realname != null ? userData.realname : "");
            
            // 格式化生日日期
            String birthday = userData.age != null ? userData.age.replace("-", "/") : "";
            etBirthday.setText(birthday);
            
            // 格式化手机号（显示前三位和后四位，中间用*代替）
            String phone = userData.phone != null ? userData.phone : "";
            if (phone.length() == 11) {
                phone = phone.substring(0, 3) + "****" + phone.substring(7);
            }
            etPhone.setText(phone);
            
            // load avatar into dialog avatar view
            try {
                String avatarUrl = userData.avatar;
                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    Glide.with(this)
                            .load(avatarUrl)
                            .apply(new RequestOptions().placeholder(R.drawable.ic_profile_avatar_placeholder).error(R.drawable.ic_profile_avatar_placeholder).circleCrop())
                            .into(imgDialogAvatar);
                } else {
                    imgDialogAvatar.setImageResource(R.drawable.ic_profile_avatar_placeholder);
                }
            } catch (Exception ignored) {}
        } else {
            // 如果没有API数据，使用默认值
            etNickname.setText(tvUserName.getText());
            etRealName.setText("张建国");
            etBirthday.setText("1955/06/15");
            etPhone.setText("138****8888");
        }

        // Create the dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Set dialog background to be transparent (for rounded corners effect)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set click listeners
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());

        btnEditAvatar.setOnClickListener(v -> showImageChoiceDialog());
        // 点击对话框中的头像也弹出拍照/相册选择
        imgDialogAvatar.setOnClickListener(v -> showImageChoiceDialog());

        etBirthday.setOnClickListener(v -> {
            // Parse current birthday from edit text
            String currentBirthday = etBirthday.getText().toString();
            int year = 1955;
            int month = 5; // 0-based
            int day = 15;
            
            if (!currentBirthday.isEmpty() && currentBirthday.contains("/")) {
                String[] parts = currentBirthday.split("/");
                if (parts.length == 3) {
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]) - 1; // Convert to 0-based
                    day = Integer.parseInt(parts[2]);
                }
            }
            
            // Create DatePickerDialog with custom style
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                            // Format date as YYYY/MM/DD
                            String formattedDate = String.format("%04d/%02d/%02d", 
                                    selectedYear, (selectedMonth + 1), selectedDay);
                            etBirthday.setText(formattedDate);
                        }
                    },
                    year, month, day);
            
            // Set dialog properties
            datePickerDialog.setCancelable(true);
            datePickerDialog.setTitle("选择出生日期");
            
            // Add Clear and Today buttons
            datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "清除", (dateDialog, which) -> {
                if (which == DatePickerDialog.BUTTON_NEGATIVE) {
                    etBirthday.setText("");
                    dateDialog.dismiss();
                }
            });
            
            datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "确定", (dateDialog, which) -> {
                if (which == DatePickerDialog.BUTTON_POSITIVE) {
                    DatePicker datePicker = datePickerDialog.getDatePicker();
                    int selectedYear = datePicker.getYear();
                    int selectedMonth = datePicker.getMonth();
                    int selectedDay = datePicker.getDayOfMonth();
                    String formattedDate = String.format("%04d/%02d/%02d", 
                            selectedYear, (selectedMonth + 1), selectedDay);
                    etBirthday.setText(formattedDate);
                }
            });
            
            datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "今天", (dateDialog, which) -> {
                if (which == DatePickerDialog.BUTTON_NEUTRAL) {
                    java.util.Calendar calendar = java.util.Calendar.getInstance();
                    int todayYear = calendar.get(java.util.Calendar.YEAR);
                    int todayMonth = calendar.get(java.util.Calendar.MONTH);
                    int todayDay = calendar.get(java.util.Calendar.DAY_OF_MONTH);
                    
                    DatePicker datePicker = datePickerDialog.getDatePicker();
                    datePicker.updateDate(todayYear, todayMonth, todayDay);
                }
            });
            
            // Ensure the DatePicker shows calendar view (not spinner)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                datePickerDialog.getDatePicker().setCalendarViewShown(true);
                datePickerDialog.getDatePicker().setSpinnersShown(false);
            }
            
            // Show the dialog
            datePickerDialog.show();
        });

        btnSaveChanges.setOnClickListener(v -> {
            // Get updated values
            String updatedNickname = etNickname.getText().toString().trim();
            String updatedRealName = etRealName.getText().toString().trim();
            String updatedBirthday = etBirthday.getText().toString().trim();
            String updatedPassword = etPassword.getText().toString();

            // Validate inputs
            if (updatedNickname.isEmpty()) {
                Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if userData is available
            if (userData == null || userData.id <= 0) {
                Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create update request
            UserApi.UpdateUserRequest request = new UserApi.UpdateUserRequest();
            request.username = updatedNickname;
            request.realname = updatedRealName;
            
            // Convert birthday format from YYYY/MM/DD to YYYY-MM-DD
            String formattedBirthday = updatedBirthday.replace("/", "-");
            request.age = formattedBirthday;
            
            // Only update password if it's not empty
            if (!updatedPassword.isEmpty()) {
                request.password = updatedPassword;
            } else {
                // Keep original password
                request.password = userData.password;
            }
            
            // Use real phone number from userData (not the masked one)
            request.phone = userData.phone;
            
            // Use original account
            request.account = userData.account;

            // If user selected a new avatar file in the dialog, upload with avatar; otherwise update info only.
            if (pendingAvatarFile != null) {
                UserApi.updateUserWithAvatar(this, userData.id, request, pendingAvatarFile, new UserApi.UserInfoCallback() {
                    @Override
                    public void onSuccess(UserApi.UserInfoResponse response) {
                        runOnUiThread(() -> {
                            tvUserName.setText(updatedNickname);
                            userData.username = updatedNickname;
                            userData.realname = updatedRealName;
                            userData.age = formattedBirthday;
                            if (!updatedPassword.isEmpty()) {
                                userData.password = updatedPassword;
                            }
                            Toast.makeText(ProfileActivity.this, response.message != null ? response.message : "个人信息与头像已更新", Toast.LENGTH_SHORT).show();
                            // 清除 pending
                            pendingAvatarFile = null;
                            // 关闭对话框并刷新个人中心数据
                            dialog.dismiss();
                            fetchUserInfo();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage, Throwable t) {
                        runOnUiThread(() -> {
                            String fullErrorMessage = "上传失败: " + errorMessage;
                            if (t != null && t.getMessage() != null) fullErrorMessage += "\n" + t.getMessage();
                            Toast.makeText(ProfileActivity.this, fullErrorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            } else {
                // Call update user info API (no avatar)
                UserApi.updateUserInfo(this, userData.id, request, new UserApi.UserInfoCallback() {
                    @Override
                    public void onSuccess(UserApi.UserInfoResponse response) {
                        runOnUiThread(() -> {
                            // Update user interface
                            tvUserName.setText(updatedNickname);
                            
                            // Update userData with new values
                            userData.username = updatedNickname;
                            userData.realname = updatedRealName;
                            userData.age = formattedBirthday;
                            if (!updatedPassword.isEmpty()) {
                                userData.password = updatedPassword;
                            }
                            
                            // Show success message
                            Toast.makeText(ProfileActivity.this, "个人信息已更新", Toast.LENGTH_SHORT).show();
                            
                            // Close the dialog
                            dialog.dismiss();
                        });
                    }
                    
                    @Override
                    public void onFailure(String errorMessage, Throwable t) {
                        runOnUiThread(() -> {
                            // 显示详细错误信息，方便调试
                            String fullErrorMessage = "更新失败: " + errorMessage;
                            if (t != null && t.getMessage() != null) {
                                fullErrorMessage += "\n错误详情: " + t.getMessage();
                            }
                            Toast.makeText(ProfileActivity.this, fullErrorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
        });

        // Show the dialog
        dialog.show();
    }

    // -------- image pick / camera and upload -------------
    private void showImageChoiceDialog() {
        String[] items = new String[] {"拍照", "从相册选择"};
        new AlertDialog.Builder(this)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_PERM_CAMERA);
                        } else {
                            startCamera();
                        }
                    } else {
                        // Gallery
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM_READ);
                        } else {
                            startPick();
                        }
                    }
                })
                .show();
    }

    private void startCamera() {
        try {
            Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(takePic, REQ_CAMERA);
        } catch (Exception e) {
            Log.e(TAG, "startCamera failed", e);
            Toast.makeText(this, "无法启动相机: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startPick() {
        try {
            Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pick.setType("image/*");
            startActivityForResult(pick, REQ_PICK);
        } catch (Exception e) {
            Log.e(TAG, "startPick failed", e);
            Toast.makeText(this, "无法打开相册: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "相机权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_PERM_READ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPick();
            } else {
                Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode != RESULT_OK) return;
            if (requestCode == REQ_CAMERA) {
                Bundle extras = data != null ? data.getExtras() : null;
                Bitmap bmp = extras != null ? (Bitmap) extras.get("data") : null;
                if (bmp != null) {
                    java.io.File f = new java.io.File(getCacheDir(), "avatar_camera.jpg");
                    try (FileOutputStream out = new FileOutputStream(f)) {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.flush();
                    }
                    // 保存到 pending，界面预览但不立即上传（同时更新对话框内与 header 的头像预览）
                    pendingAvatarFile = f;
                    ImageView imgDialogAvatar = findViewById(R.id.img_dialog_avatar);
                    ImageView headerAvatar = findViewById(R.id.img_avatar);
                    if (imgDialogAvatar != null) {
                        Glide.with(this).load(f).circleCrop().into(imgDialogAvatar);
                    }
                    if (headerAvatar != null) {
                        Glide.with(this).load(f).circleCrop().into(headerAvatar);
                    }
                } else {
                    Toast.makeText(this, "未获取到相机图片", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_PICK) {
                Uri uri = data != null ? data.getData() : null;
                    if (uri != null) {
                    java.io.File f = new java.io.File(getCacheDir(), "avatar_pick.jpg");
                    try (InputStream in = getContentResolver().openInputStream(uri);
                         OutputStream out = new FileOutputStream(f)) {
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                        out.flush();
                    }
                    // 保存到 pending，界面预览但不立即上传（同时更新对话框内与 header 的头像预览）
                    pendingAvatarFile = f;
                    ImageView imgDialogAvatar = findViewById(R.id.img_dialog_avatar);
                    ImageView headerAvatar = findViewById(R.id.img_avatar);
                    if (imgDialogAvatar != null) {
                        Glide.with(this).load(f).circleCrop().into(imgDialogAvatar);
                    }
                    if (headerAvatar != null) {
                        Glide.with(this).load(f).circleCrop().into(headerAvatar);
                    }
                } else {
                    Toast.makeText(this, "未选择图片", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "处理图片失败", e);
            Toast.makeText(this, "处理图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAvatarFile(java.io.File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "头像文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userData == null || userData.id <= 0) {
            Toast.makeText(this, "未获取到用户信息，无法上传", Toast.LENGTH_SHORT).show();
            return;
        }
        UserApi.UpdateUserRequest req = new UserApi.UpdateUserRequest();
        // 填充现有用户信息，后端示例要求 user JSON
        req.username = userData.username;
        req.account = userData.account;
        req.password = userData.password;
        req.realname = userData.realname;
        req.phone = userData.phone;
        req.age = userData.age;

        Toast.makeText(this, "正在上传头像...", Toast.LENGTH_SHORT).show();
        UserApi.updateUserWithAvatar(this, userData.id, req, file, new UserApi.UserInfoCallback() {
            @Override
            public void onSuccess(UserApi.UserInfoResponse response) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, response.message != null ? response.message : "头像上传成功", Toast.LENGTH_SHORT).show();
                    fetchUserInfo();
                });
            }

            @Override
            public void onFailure(String errorMessage, Throwable t) {
                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "上传失败: " + errorMessage, Toast.LENGTH_LONG).show());
            }
        });
    }
}