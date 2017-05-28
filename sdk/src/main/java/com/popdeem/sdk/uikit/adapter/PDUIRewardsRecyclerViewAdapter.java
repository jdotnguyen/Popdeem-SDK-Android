/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Popdeem
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.popdeem.sdk.uikit.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.popdeem.sdk.R;
import com.popdeem.sdk.core.model.PDReward;
import com.popdeem.sdk.core.utils.PDNumberUtils;
import com.popdeem.sdk.uikit.utils.PDUIUtils;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by mikenolan on 19/02/16.
 */
public class PDUIRewardsRecyclerViewAdapter extends RecyclerView.Adapter<PDUIRewardsRecyclerViewAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(View view);
    }

    private OnItemClickListener mListener;
    private ArrayList<PDReward> mItems;

    public PDUIRewardsRecyclerViewAdapter(ArrayList<PDReward> mItems) {
        this.mItems = mItems;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward, parent, false), parent.getContext());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PDReward reward = this.mItems.get(position);

        holder.offerTextView.setText(reward.getDescription());

        if (reward.getRules() == null || reward.getRules().isEmpty()) {
            holder.rulesTextView.setText("");
            holder.rulesTextView.setVisibility(View.GONE);
        } else {
            holder.rulesTextView.setText(reward.getRules());
            holder.rulesTextView.setVisibility(View.VISIBLE);
        }

        StringBuilder actionStringBuilder = new StringBuilder("");

        final boolean TWITTER_ACTION_REQUIRED = twitterActionRequired(reward.getSocialMediaTypes());
        if (reward.getAction().equalsIgnoreCase(PDReward.PD_REWARD_ACTION_PHOTO)) {
            actionStringBuilder.append(holder.context.getString(TWITTER_ACTION_REQUIRED ? R.string.pd_claim_action_tweet_photo : R.string.pd_claim_action_photo));
        } else if (reward.getAction().equalsIgnoreCase(PDReward.PD_REWARD_ACTION_CHECKIN)) {
            actionStringBuilder.append(holder.context.getString(TWITTER_ACTION_REQUIRED ? R.string.pd_claim_action_tweet_checkin : R.string.pd_claim_action_checkin));
        } else {
            actionStringBuilder.append(holder.context.getString(R.string.pd_claim_action_none));
        }

        long timeInSecs = PDNumberUtils.toLong(reward.getAvailableUntilInSeconds(), -1);
        String convertedTimeString = PDUIUtils.convertTimeToDayAndMonth(timeInSecs);
        if (!convertedTimeString.isEmpty()) {
            actionStringBuilder.append(String.format(Locale.getDefault(), " | Exp %1s", convertedTimeString));
        }

//        if (reward.getDisableLocationVerification().equalsIgnoreCase(PDReward.PD_FALSE) && reward.getDistanceFromUser() > 0) {
//            actionStringBuilder.append(String.format(Locale.getDefault(), " | %1s", PDUIUtils.formatDistance(reward.getDistanceFromUser())));
//        }

        holder.actionTextView.setText(actionStringBuilder.toString());

        String imageUrl = reward.getCoverImage();
        if (imageUrl == null || imageUrl.isEmpty() || imageUrl.contains("default")) {
            Picasso.with(holder.context)
                    .load(R.drawable.pd_ui_star_icon)
                    .error(R.drawable.pd_ui_star_icon)
                    .placeholder(R.drawable.pd_ui_star_icon)
                    .into(holder.imageView);
        } else {
            Picasso.with(holder.context)
                    .load(imageUrl)
                    .error(R.drawable.pd_ui_star_icon)
                    .resizeDimen(R.dimen.pd_reward_item_image_dimen, R.dimen.pd_reward_item_image_dimen)
                    .placeholder(R.drawable.pd_ui_star_icon)
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    private boolean twitterActionRequired(String[] socialMediaTypes) {
        return socialMediaTypes != null && socialMediaTypes.length == 1 && socialMediaTypes[0].equalsIgnoreCase(PDReward.PD_SOCIAL_MEDIA_TYPE_TWITTER);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        Context context;
        ImageView imageView;
        TextView offerTextView;
        TextView rulesTextView;
        TextView actionTextView;

        public ViewHolder(View itemView, Context context) {
            super(itemView);
            itemView.setClickable(true);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onItemClick(v);
                    }
                }
            });
            this.context = context;
            this.imageView = (ImageView) itemView.findViewById(R.id.pd_reward_star_image_view);
            this.offerTextView = (TextView) itemView.findViewById(R.id.pd_reward_offer_text_view);
            this.rulesTextView = (TextView) itemView.findViewById(R.id.pd_reward_item_rules_text_view);
            this.actionTextView = (TextView) itemView.findViewById(R.id.pd_reward_request_text_view);
        }
    }

}
