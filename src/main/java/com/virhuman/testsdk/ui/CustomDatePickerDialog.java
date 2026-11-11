package com.virhuman.testsdk.ui;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;

import com.virhuman.testsdk.R;

import java.util.Calendar;

/**
 * 自定义日期选择对话框，支持年月日滑动选择
 */
public class CustomDatePickerDialog extends Dialog {

    private NumberPicker yearPicker;
    private NumberPicker monthPicker;
    private NumberPicker dayPicker;
    private Button btnClear;
    private Button btnToday;
    private Button btnConfirm;

    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;

    private OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(int year, int month, int day);
        void onClear();
    }

    public CustomDatePickerDialog(@NonNull Context context) {
        super(context, android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth);
        init();
    }

    private void init() {
        setContentView(R.layout.dialog_custom_date_picker);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        // 初始化视图
        yearPicker = findViewById(R.id.picker_year);
        monthPicker = findViewById(R.id.picker_month);
        dayPicker = findViewById(R.id.picker_day);
        btnClear = findViewById(R.id.btn_clear);
        btnToday = findViewById(R.id.btn_today);
        btnConfirm = findViewById(R.id.btn_confirm);

        // 设置年份范围 (1900-当前年份+10)
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        yearPicker.setMinValue(1900);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentYear - 20); // 默认设置为20年前

        // 设置月份范围 (1-12)
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(1);

        // 设置日期范围 (1-31)，默认设置为1月31日
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(31);
        dayPicker.setValue(1);

        // 设置月份变化监听器，动态调整日期范围
        monthPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            updateDayRange();
        });

        // 设置年份变化监听器，处理闰年
        yearPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            updateDayRange();
        });

        // 设置按钮点击事件
        btnClear.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClear();
            }
            dismiss();
        });

        btnToday.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            int todayYear = today.get(Calendar.YEAR);
            int todayMonth = today.get(Calendar.MONTH) + 1; // Calendar.MONTH是从0开始的
            int todayDay = today.get(Calendar.DAY_OF_MONTH);

            yearPicker.setValue(todayYear);
            monthPicker.setValue(todayMonth);
            dayPicker.setValue(todayDay);
        });

        btnConfirm.setOnClickListener(v -> {
            selectedYear = yearPicker.getValue();
            selectedMonth = monthPicker.getValue();
            selectedDay = dayPicker.getValue();

            if (listener != null) {
                listener.onDateSelected(selectedYear, selectedMonth, selectedDay);
            }
            dismiss();
        });
    }

    /**
     * 根据年份和月份动态调整日期范围
     */
    private void updateDayRange() {
        int year = yearPicker.getValue();
        int month = monthPicker.getValue();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1); // 设置为指定年月的1号
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH); // 获取该月的最大天数

        dayPicker.setMaxValue(maxDay);

        // 如果当前选择的日期超过了该月的最大天数，调整为最大天数
        if (dayPicker.getValue() > maxDay) {
            dayPicker.setValue(maxDay);
        }
    }

    /**
     * 设置初始日期
     */
    public void setDate(int year, int month, int day) {
        if (yearPicker != null) {
            yearPicker.setValue(year);
        }
        if (monthPicker != null) {
            monthPicker.setValue(month);
        }
        if (dayPicker != null) {
            dayPicker.setValue(day);
        }
    }

    /**
     * 设置日期选择监听器
     */
    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }
}