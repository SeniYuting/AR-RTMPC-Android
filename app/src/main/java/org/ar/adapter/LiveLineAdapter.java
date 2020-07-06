package org.ar.adapter;

import android.annotation.SuppressLint;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import org.ar.rtmpc.R;
import org.ar.model.LineBean;

public class LiveLineAdapter extends BaseQuickAdapter<LineBean, BaseViewHolder> {
    public LiveLineAdapter() {
        super(R.layout.item_line);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void convert(BaseViewHolder helper, LineBean item) {
        TextView tv_name = helper.getView(R.id.tv_name);
        tv_name.setText(item.name + "Request Microphone");
        helper.addOnClickListener(R.id.tv_agree);
        helper.addOnClickListener(R.id.tv_refuse);
    }
}
