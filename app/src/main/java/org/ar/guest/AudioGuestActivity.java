package org.ar.guest;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;

import org.ar.BaseActivity;
import org.ar.ARApplication;
import org.ar.adapter.AudioLineAdapter;
import org.ar.adapter.LiveMessageAdapter;
import org.ar.adapter.LogAdapter;
import org.ar.common.utils.ARAudioManager;
import org.ar.model.LineBean;
import org.ar.model.MessageBean;
import org.ar.utils.ARUtils;
import org.ar.utils.ToastUtil;
import org.ar.widgets.KeyboardDialogFragment;
import org.ar.common.enums.ARVideoCommon;
import org.ar.rtmpc_hybrid.ARRtmpcEngine;
import org.ar.rtmpc_hybrid.ARRtmpcGuestEvent;
import org.ar.rtmpc_hybrid.ARRtmpcGuestKit;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * Audio Guest Activity
 */
public class AudioGuestActivity extends BaseActivity implements BaseQuickAdapter.OnItemChildClickListener {
    TextView tvTitle;
    RecyclerView rvMsgList, rvLog;
    TextView tvApplyLine;
    View viewSpace;
    TextView tvMemberNum;
    RecyclerView rvLineList;
    TextView tvRtmpOk;
    TextView tvRtmpStatus;
    TextView tvRtcOk;
    TextView tvHostName;
    ImageView ivLineAnim;
    RelativeLayout rl_log_layout;
    private ARRtmpcGuestKit mGuestKit;
    private ARAudioManager mRtmpAudioManager = null;
    private LiveMessageAdapter mAdapter;
    private LogAdapter logAdapter;
    private boolean isApplyLine = false;  // is request microphone
    private boolean isLining = false;
    private AnimationDrawable hostAnimation;
    private AudioLineAdapter audioLineAdapter;
    private String userID = "guest" + (int) ((Math.random() * 9 + 1) * 100000) + "";

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isApplyLine) {
                ShowExitDialog();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Destroy RTMP player
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGuestKit != null) {
            mGuestKit.clean();
            mGuestKit = null;
        }
        if (mRtmpAudioManager != null) {
            mRtmpAudioManager.stop();
            mRtmpAudioManager = null;
        }

    }

    @Override
    public int getLayoutId() {
        return org.ar.rtmpc.R.layout.activity_audio_guest;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initView(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Keep screen on
        viewSpace = findViewById(org.ar.rtmpc.R.id.view_space);
        mImmersionBar.titleBar(viewSpace).init();
        tvTitle = findViewById(org.ar.rtmpc.R.id.tv_title);
        rl_log_layout = findViewById(org.ar.rtmpc.R.id.rl_log_layout);
        rvLog = findViewById(org.ar.rtmpc.R.id.rv_log);
        rvMsgList = findViewById(org.ar.rtmpc.R.id.rv_msg_list);
        tvApplyLine = findViewById(org.ar.rtmpc.R.id.tv_apply_line);
        tvMemberNum = findViewById(org.ar.rtmpc.R.id.tv_member_num);
        rvLineList = findViewById(org.ar.rtmpc.R.id.rv_line_list);
        tvRtmpOk = findViewById(org.ar.rtmpc.R.id.tv_rtmp_ok);
        tvRtmpStatus = findViewById(org.ar.rtmpc.R.id.tv_rtmp_status);
        tvRtcOk = findViewById(org.ar.rtmpc.R.id.tv_rtc_ok);
        tvHostName = findViewById(org.ar.rtmpc.R.id.tv_host_name);
        ivLineAnim = findViewById(org.ar.rtmpc.R.id.iv_line_anim);
        hostAnimation = (AnimationDrawable) ivLineAnim.getBackground();
        rvMsgList.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new LiveMessageAdapter();
        audioLineAdapter = new AudioLineAdapter(false);
        audioLineAdapter.setOnItemChildClickListener(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        rvLineList.setLayoutManager(linearLayoutManager);
        rvLineList.setAdapter(audioLineAdapter);
        rvLineList.setItemAnimator(null);
        rvMsgList.setAdapter(mAdapter);
        logAdapter = new LogAdapter();
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter.bindToRecyclerView(rvLog);
        String pullUrl = getIntent().getStringExtra("pullURL");
        String hostName = getIntent().getStringExtra("hostName");
        tvHostName.setText(hostName);
        String liveId = getIntent().getStringExtra("liveId");
        tvTitle.setText("Room ID：" + liveId);
        mRtmpAudioManager = ARAudioManager.create(this);
        mRtmpAudioManager.start(new ARAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(ARAudioManager.AudioDevice audioDevice, Set<ARAudioManager.AudioDevice> set) {

            }
        });
        ARRtmpcEngine.Inst().getGuestOption().setMediaType(ARVideoCommon.ARMediaType.Audio);
        mGuestKit = new ARRtmpcGuestKit(mGuestListener);
        mGuestKit.startRtmpPlay(pullUrl, 0);
        mGuestKit.joinRTCLine("", liveId, userID, getUserData());
    }

    public String getUserData() {
        JSONObject user = new JSONObject();
        try {
            user.put("isHost", 0);
            user.put("userId", userID);
            user.put("nickName", ARApplication.getNickName());
            user.put("headUrl", "www.ebay.com");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return user.toString();
    }

    private void ShowExitDialog() {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle(org.ar.rtmpc.R.string.str_exit);
        build.setMessage(org.ar.rtmpc.R.string.str_line_hangup);
        build.setPositiveButton(org.ar.rtmpc.R.string.str_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mGuestKit.hangupRTCLine();
                isApplyLine = false;
                isLining = false;
                finishAnimActivity();
            }
        });
        build.setNegativeButton(org.ar.rtmpc.R.string.str_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });

        build.show();
    }

    private void addChatMessageList(MessageBean chatMessageBean) {
        if (chatMessageBean == null) {
            return;
        }

        if (mAdapter.getData().size() >= 150) {
            mAdapter.remove(0);
        }
        mAdapter.addData(chatMessageBean);
        rvMsgList.smoothScrollToPosition(mAdapter.getData().size() - 1);
    }

    public void printLog(String log) {
        Log.d("RTMP", log);
        logAdapter.addData(log);
    }

    /**
     * RTMP player
     */
    private ARRtmpcGuestEvent mGuestListener = new ARRtmpcGuestEvent() {

        // Connect to RTMP player
        @Override
        public void onRtmpPlayerOk() {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("Call onRtmpPlayerOk");
                    if (tvRtmpOk != null) {
                        tvRtmpOk.setText("RTMP connected");
                    }
                }
            });
        }

        // Start RTMP player, Live begins
        @Override
        public void onRtmpPlayerStart() {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpPlayerStart");
                }
            });
        }

        /**
         * RTMP player status
         * @param cacheTime cache time
         * @param curBitrate current bit rate for the player stream
         */
        @Override
        public void onRtmpPlayerStatus(final int cacheTime, final int curBitrate) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("Call onRtmpPlayerStatus cacheTime:" + cacheTime + " curBitrate:" + curBitrate);
                    if (tvRtmpStatus != null) {
                        tvRtmpStatus.setText("Cache time: " + cacheTime + " ms" + "\nBit Rate: " + curBitrate / 10024 / 8 + "kb/s");
                    }
                }
            });
        }

        /**
         * RTMP loading
         * @param nPercent percentage
         */
        @Override
        public void onRtmpPlayerLoading(final int nPercent) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpPlayerCache nPercent:" + nPercent);
                }
            });
        }

        /**
         * RTMP closed
         * @param nCode Response code
         */
        @Override
        public void onRtmpPlayerClosed(final int nCode) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpPlayerClosed nCode:" + nCode);
                }
            });
        }


        /**
         * RTC join
         * @param nCode Response code: [0: normal; 101: Live hasn't started]
         */
        @Override
        public void onRTCJoinLineResult(final int nCode, String s) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCJoinLineResult  nCode:" + nCode);
                    if (nCode == 0) {
                        if (tvRtcOk != null) {
                            tvRtcOk.setText(org.ar.rtmpc.R.string.str_rtc_connect_success);
                        }
                    } else if (nCode == 101) {
                        Toast.makeText(AudioGuestActivity.this, org.ar.rtmpc.R.string.str_hoster_not_live, Toast.LENGTH_LONG).show();
                        if (tvRtcOk != null) {
                            tvRtcOk.setText(org.ar.rtmpc.R.string.str_rtc_connect_success);
                        }
                    } else {
                        if (tvRtcOk != null) {
                            tvRtcOk.setText(ARUtils.getErrString(nCode));
                        }
                    }
                }
            });
        }

        /**
         * Request microphone
         * @param nCode 0: Success 1: Rejected
         */
        @Override
        public void onRTCApplyLineResult(final int nCode) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("Call onRTCApplyLineResult nCode:" + nCode);
                    if (nCode == 0) {
                        isApplyLine = true;
                        tvApplyLine.setText("Hang UP");
                        audioLineAdapter.addData(0, new LineBean("self", "Self", true));
                        isLining = true;
                        tvApplyLine.setBackgroundResource(org.ar.rtmpc.R.drawable.shape_room_hang_up_line);
                    } else if (nCode == 601) {
                        Toast.makeText(AudioGuestActivity.this, org.ar.rtmpc.R.string.str_hoster_refused, Toast.LENGTH_LONG).show();
                        isApplyLine = false;
                        isLining = false;
                        tvApplyLine.setText("Connect to Microphone");
                        tvApplyLine.setBackgroundResource(org.ar.rtmpc.R.drawable.shape_room_apply_line);
                    }
                }
            });
        }

        /**
         * RTC hang up
         */
        @Override
        public void onRTCHangupLine() {
            //Hang up
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCHangupLine ");
                    mGuestKit.hangupRTCLine();
                    audioLineAdapter.remove(0);
                    tvApplyLine.setText(org.ar.rtmpc.R.string.str_connect_hoster);
                    tvApplyLine.setBackgroundResource(org.ar.rtmpc.R.drawable.shape_room_apply_line);
                    isApplyLine = false;
                    isLining = false;
                }
            });
        }

        @Override
        public void onRTCOpenRemoteVideoRender(String s, String s1, String s2, String s3) {
        }

        @Override
        public void onRTCCloseRemoteVideoRender(String s, String s1, String s2) {
        }

        @Override
        public void onRTCOpenRemoteAudioLine(final String strLivePeerId, final String strUserId, final String strUserData) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCOpenAudioLine strLivePeerId:" + strLivePeerId + "strUserId:" + strUserId + " strUserData:" + strUserData);
                    try {
                        JSONObject jsonObject = new JSONObject(strUserData);
                        if (strLivePeerId.equals("RTMPC_Line_Hoster")) {
                            audioLineAdapter.addData(new LineBean(strLivePeerId, jsonObject.getString("nickName"), true));
                        } else {
                            audioLineAdapter.addData(new LineBean(strLivePeerId, jsonObject.getString("nickName"), false));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        @Override
        public void onRTCCloseRemoteAudioLine(final String strLivePeerId, final String strUserId) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCCloseAudioLine strLivePeerId:" + strLivePeerId + "strUserId:" + strUserId);
                    int index = 9;
                    for (int i = 0; i < audioLineAdapter.getData().size(); i++) {
                        if (audioLineAdapter.getItem(i).peerId.equals(strLivePeerId)) {
                            index = i;
                        }
                    }
                    if (index != 9 && index <= audioLineAdapter.getData().size()) {
                        audioLineAdapter.remove(index);
                    }
                }
            });
        }

        @Override
        public void onRTCLocalAudioActive(int i) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("RTC", "onRTCLocalAudioActive");
                }
            });
        }

        @Override
        public void onRTCHosterAudioActive(int i) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("RTC", "onRTCHosterAudioActive");
                }
            });
        }

        @Override
        public void onRTCRemoteAudioActive(final String strLivePeerId, final String strUserId, final int nTime) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCAudioActive strLivePeerId:" + strLivePeerId + "strUserId:" + strUserId + " nTime:" + nTime);
                    if (strLivePeerId.equals("RTMPC_Hoster")) { // hoster
                        ivLineAnim.setVisibility(View.VISIBLE);
                        hostAnimation.start();
                    } else {
                        for (int i = 0; i < audioLineAdapter.getData().size(); i++) {
                            if (strLivePeerId.equals(audioLineAdapter.getData().get(i).peerId)) {
                                audioLineAdapter.getItem(i).setStartAnim(true);
                                audioLineAdapter.notifyItemChanged(i);
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void onRTCRemoteAVStatus(final String s, boolean b, boolean b1) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCRemoteAVStatus  peerID:" + s);
                }
            });
        }

        /**
         * Host leave room
         * @param nCode nCode
         */
        @Override
        public void onRTCLineLeave(final int nCode, String s) {
            //host leave room
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCLineLeave nCode:" + nCode);
                    if (mGuestKit != null) {
                        mGuestKit.stopRtmpPlay();
                    }
                    finishAnimActivity();
                    ToastUtil.show("Host leaves");
                }
            });
        }

        /**
         * User message
         * @param strCustomID sender id
         * @param strCustomName sender name
         * @param strCustomHeader sender profile url
         * @param strMessage message
         */
        @Override
        public void onRTCUserMessage(final int nType, final String strCustomID, final String strCustomName, final String strCustomHeader, final String strMessage) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCUserMessage nType:" + nType + "strCustomID:" + strCustomID + "strCustomName:" + strCustomName + "strCustomHeader:" + strCustomHeader + "strMessage:" + strMessage);
                    addChatMessageList(new MessageBean(MessageBean.AUDIO, strCustomName, strMessage));
                }
            });
        }

        /**
         * Number of people watching live
         * @param totalMembers number of people
         */
        @Override
        public void onRTCMemberNotify(final String strServerId, final String strRoomId, final int totalMembers) {
            AudioGuestActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("Call onRTCMemberNotify strServerId:" + strServerId + "strRoomId:" + strRoomId + "totalMembers:" + totalMembers);
                    tvMemberNum.setText("Audience: " + totalMembers + "");
                }
            });
        }


    };

    @SuppressLint("SetTextI18n")
    public void onClick(View view) {
        switch (view.getId()) {
            case org.ar.rtmpc.R.id.btn_close:
                if (isLining) {
                    ShowExitDialog();
                } else {
                    finishAnimActivity();
                }
                break;
            case org.ar.rtmpc.R.id.iv_message:
                showChatLayout();
                break;
            case org.ar.rtmpc.R.id.tv_apply_line:
                if (isApplyLine) {
                    if (mGuestKit != null) {
                        mGuestKit.hangupRTCLine();
                        if (isLining) {
                            audioLineAdapter.remove(0);
                        }
                        tvApplyLine.setText("Connect to Microphone");
                        tvApplyLine.setBackgroundResource(org.ar.rtmpc.R.drawable.shape_room_apply_line);
                        isApplyLine = false;
                        isLining = false;
                    }
                } else {
                    if (mGuestKit != null) {
                        mGuestKit.applyRTCLine();
                        tvApplyLine.setText("Hang UP");
                        tvApplyLine.setBackgroundResource(org.ar.rtmpc.R.drawable.shape_room_hang_up_line);
                        isApplyLine = true;
                        isLining = false;
                    }
                }
                break;
            case org.ar.rtmpc.R.id.btn_log:
                rl_log_layout.setVisibility(View.VISIBLE);
                break;
            case org.ar.rtmpc.R.id.ibtn_close_log:
                rl_log_layout.setVisibility(View.GONE);
                break;
        }
    }

    private void showChatLayout() {
        KeyboardDialogFragment keyboardDialogFragment = new KeyboardDialogFragment();
        keyboardDialogFragment.show(getSupportFragmentManager(), "KeyboardDialogFragment");
        keyboardDialogFragment.setEdittextListener(new KeyboardDialogFragment.EdittextListener() {
            @Override
            public void setTextStr(String text) {
                addChatMessageList(new MessageBean(MessageBean.AUDIO, ARApplication.getNickName(), text));
                mGuestKit.sendMessage(0, ARApplication.getNickName(), "", text);
            }

            @Override
            public void dismiss(DialogFragment dialogFragment) {
            }
        });
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        switch (view.getId()) {
            case org.ar.rtmpc.R.id.tv_hangup:
                if (mGuestKit != null) {

                    mGuestKit.hangupRTCLine();
                    tvApplyLine.setBackgroundResource(org.ar.rtmpc.R.drawable.shape_room_apply_line);
                    tvApplyLine.setText("Connect to Microphone");
                    audioLineAdapter.remove(0);
                    isApplyLine = false;
                    isLining = false;
                }
                break;
        }
    }

}