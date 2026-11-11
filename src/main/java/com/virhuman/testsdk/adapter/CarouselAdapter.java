package com.virhuman.testsdk.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.virhuman.testsdk.R;
import com.virhuman.testsdk.model.DramaItem;


import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private static final String TAG = "CarouselAdapter";

    private final List<DramaItem> items;


    public CarouselAdapter(List<DramaItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carousel, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        DramaItem item = items.get(position);
        if (item != null) {
            Log.d(TAG, "Carousel onBind position=" + position
                    + ", title=" + item.getTitle()
                    + ", coverUrl=" + item.getCoverUrl());
        } else {
            Log.w(TAG, "Carousel onBind position=" + position + " with null item");
        }

        holder.titleText.setText(item != null ? item.getTitle() : "");
        holder.subtitleText.setText(item != null ? item.getDescription() : "");

        String coverUrl = item != null ? item.getCoverUrl() : null;
        if (coverUrl == null || coverUrl.isEmpty()) {
            Log.w(TAG, "Carousel coverUrl is empty, position=" + position
                    + ", title=" + (item != null ? item.getTitle() : "null"));
        }

        Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Log.d(TAG, "Glide loaded carousel image success, position=" + position
                                + ", title=" + (item != null ? item.getTitle() : "null"));
                        holder.container.setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        Log.d(TAG, "Glide onLoadCleared, position=" + position);
                    }
                });
    }


    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView subtitleText;
        final ConstraintLayout container;

        CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.tv_carousel_title);
            subtitleText = itemView.findViewById(R.id.tv_carousel_subtitle);
            container = itemView.findViewById(R.id.carousel_item_container);


        }
    }
}
