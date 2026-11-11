package com.virhuman.testsdk.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.virhuman.testsdk.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 供用户选择背景图的页面（简单实现，使用项目内 drawable 资源作为背景候选）
 */
public class BackgroundSelectActivity extends AppCompatActivity {
    public static final String EXTRA_SELECTED_BG_RES = "selected_background_res";

    private RecyclerView recyclerView;
    private BgAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_background_select);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_bg_list);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        List<Integer> bgList = new ArrayList<>();
        // 使用项目中新增的三张背景图片，按用户要求提供直接预览与选择
        bgList.add(R.drawable.background1);
        bgList.add(R.drawable.background2);
        bgList.add(R.drawable.background3);

        adapter = new BgAdapter(bgList);
        recyclerView.setAdapter(adapter);
    }

    private class BgAdapter extends RecyclerView.Adapter<BgAdapter.VH> {
        private final List<Integer> list;

        BgAdapter(List<Integer> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_background_select, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            int res = list.get(position);
            holder.iv.setImageResource(res);
            holder.card.setOnClickListener(v -> {
                Intent result = new Intent();
                result.putExtra(EXTRA_SELECTED_BG_RES, res);
                setResult(Activity.RESULT_OK, result);
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView iv;
            CardView card;
            VH(@NonNull View itemView) {
                super(itemView);
                iv = itemView.findViewById(R.id.iv_bg_preview);
                card = (CardView) itemView;
            }
        }
    }
}



