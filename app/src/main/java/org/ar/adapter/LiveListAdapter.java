package org.ar.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.ar.rtmpc.R;
import org.ar.model.LiveBean;

public class LiveListAdapter extends BaseQuickAdapter<LiveBean, BaseViewHolder> {

    public LiveListAdapter() {
        super(R.layout.item_live);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void convert(BaseViewHolder helper, LiveBean item) {
        helper.setText(R.id.tv_name, item.getmLiveTopic());
        helper.setText(R.id.tv_num, item.getmMemberNum() + "");
        TextView tvLiveType = helper.getView(R.id.tv_live_type);

        Drawable imgVideo = helper.itemView.getContext().getResources().getDrawable(
                R.drawable.img_video);
        Drawable imgAudio = helper.itemView.getContext().getResources().getDrawable(
                R.drawable.img_audio);
        if (item.isAudioLive == 1) {
            tvLiveType.setCompoundDrawablesWithIntrinsicBounds(imgAudio,
                    null, null, null);
            tvLiveType.setText("Audio Live");
        } else {
            tvLiveType.setCompoundDrawablesWithIntrinsicBounds(imgVideo,
                    null, null, null);
            tvLiveType.setText("Video Live");
        }
    }
}
