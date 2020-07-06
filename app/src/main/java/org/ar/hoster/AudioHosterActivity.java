package org.ar.hoster;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;

import org.ar.BaseActivity;
import org.ar.ARApplication;
import org.ar.adapter.AudioLineAdapter;
import org.ar.adapter.LiveMessageAdapter;
import org.ar.adapter.LogAdapter;
import org.ar.common.utils.ARAudioManager;
import org.ar.rtmpc.R;
import org.ar.model.LineBean;
import org.ar.model.MessageBean;
import org.ar.utils.ARUtils;
import org.ar.utils.DisplayUtils;
import org.ar.utils.ToastUtil;
import org.ar.widgets.CustomDialog;
import org.ar.widgets.KeyboardDialogFragment;
import org.ar.common.enums.ARVideoCommon;
import org.ar.rtmpc_hybrid.ARRtmpcEngine;
import org.ar.rtmpc_hybrid.ARRtmpcHosterEvent;
import org.ar.rtmpc_hybrid.ARRtmpcHosterKit;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Audio Host Activity
 */
public class AudioHosterActivity extends BaseActivity implements BaseQuickAdapter.OnItemChildClickListener {

    TextView tvTitle, tvRtmpOk, tvRtmpStatus, tvRtcOk, tvMemberNum, tv_host_name;
    RecyclerView rvMsgList;
    View viewSpace;
    ImageButton tvLineList;
    RecyclerView rvLineList, rvLog;
    RelativeLayout rl_log_layout;
    private LogAdapter logAdapter;
    private AudioLineAdapter audioLineAdapter;
    private ARRtmpcHosterKit mHosterKit;
    private ARAudioManager mRtmpAudioManager = null;
    private LiveMessageAdapter mAdapter;
    private String nickname;
    private CustomDialog line_dialog;
    private LineFragment lineFragment;
    private boolean isShowLineList = false;
    HosterActivity.LineListener lineListener;
    private String pushURL = "", pullURL = "", liveId = ARApplication.LIVE_ID, userId = "host" + (int) ((Math.random() * 9 + 1) * 100000) + "";
    private List<String> applyLineList = new ArrayList<>();  //people request microphone

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ShowExitDialog();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHosterKit != null) {
            mHosterKit.clean();
            mHosterKit = null;
        }

        // Close RTMPAudioManager
        if (mRtmpAudioManager != null) {
            mRtmpAudioManager.stop();
            mRtmpAudioManager = null;

        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_audio_hoster;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initView(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tvTitle = findViewById(R.id.tv_title);
        rl_log_layout = findViewById(R.id.rl_log_layout);
        rvLog = findViewById(R.id.rv_log);
        tvRtmpOk = findViewById(R.id.tv_rtmp_ok);
        tvRtmpStatus = findViewById(R.id.tv_rtmp_status);
        tvRtcOk = findViewById(R.id.tv_rtc_ok);
        rvMsgList = findViewById(R.id.rv_msg_list);
        viewSpace = findViewById(R.id.view_space);
        mImmersionBar.titleBar(viewSpace).init();
        tvMemberNum = findViewById(R.id.tv_member_num);
        tvLineList = findViewById(R.id.tv_line_list);
        rvLineList = findViewById(R.id.rv_line_list);
        tv_host_name = findViewById(R.id.tv_host_name);
        initLineFragment();
        logAdapter = new LogAdapter();
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter.bindToRecyclerView(rvLog);
        mAdapter = new LiveMessageAdapter();
        audioLineAdapter = new AudioLineAdapter(true);
        audioLineAdapter.setOnItemChildClickListener(this);
        rvLineList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvLineList.setAdapter(audioLineAdapter);
        rvMsgList.setLayoutManager(new LinearLayoutManager(this));
        rvMsgList.setAdapter(mAdapter);
        pushURL = getIntent().getStringExtra("pushURL");
        pullURL = getIntent().getStringExtra("pullURL");
        tvTitle.setText("Room ID: " + liveId);
        nickname = ARApplication.getNickName();
        tv_host_name.setText(nickname);
        mRtmpAudioManager = ARAudioManager.create(this);
        mRtmpAudioManager.start(new ARAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(ARAudioManager.AudioDevice audioDevice, Set<ARAudioManager.AudioDevice> set) {

            }
        });
        ARRtmpcEngine.Inst().getHosterOption().setMediaType(ARVideoCommon.ARMediaType.Audio);
        mHosterKit = new ARRtmpcHosterKit(mHosterListener);
        mHosterKit.startPushRtmpStream(pushURL);
        mHosterKit.createRTCLine("", liveId, "host", getUserData(), getLiveInfo(pullURL, pullURL));
    }

    public String getLiveInfo(String pullUrl, String hlsUrl) {
        JSONObject liveInfo = new JSONObject();

        try {
            liveInfo.put("hosterId", userId);
            liveInfo.put("rtmpUrl", pullUrl);
            liveInfo.put("hlsUrl", hlsUrl);
            liveInfo.put("liveTopic", liveId);
            liveInfo.put("anyrtcId", liveId);
            liveInfo.put("isAudioLive", 1);
            liveInfo.put("hosterName", nickname);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return liveInfo.toString();
    }

    public String getUserData() {
        JSONObject user = new JSONObject();
        try {
            user.put("isHost", 1);
            user.put("userId", userId);
            user.put("nickName", ARApplication.getNickName());
            user.put("headUrl", "www.ebay.com");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return user.toString();
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

    private void showChatLayout() {
        KeyboardDialogFragment keyboardDialogFragment = new KeyboardDialogFragment();
        keyboardDialogFragment.show(getSupportFragmentManager(), "KeyboardDialogFragment");
        keyboardDialogFragment.setEdittextListener(new KeyboardDialogFragment.EdittextListener() {
            @Override
            public void setTextStr(String text) {
                addChatMessageList(new MessageBean(MessageBean.AUDIO, nickname, text));
                mHosterKit.sendMessage(0, nickname, "", text);
            }

            @Override
            public void dismiss(DialogFragment dialogFragment) {
            }
        });
    }


    private void ShowExitDialog() {
        AlertDialog.Builder build = new AlertDialog.Builder(this);
        build.setTitle(R.string.str_exit);
        build.setMessage(R.string.str_live_stop);
        build.setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAnimActivity();
            }
        });
        build.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        build.show();
    }

    public void printLog(String log) {
        Log.d("RTC", log);
        logAdapter.addData(log);
    }

    private ARRtmpcHosterEvent mHosterListener = new ARRtmpcHosterEvent() {
        @Override
        public void onRtmpStreamOk() {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("Call onRtmpStreamOk");
                    if (tvRtmpOk != null) {
                        tvRtmpOk.setText("RTMP Connected");
                    }
                }
            });
        }

        /**
         * RTMP reconnect
         * @param times times for reconnection
         */
        @Override
        public void onRtmpStreamReconnecting(final int times) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpStreamReconnecting times:" + times);

                }
            });
        }

        /**
         * RTMP stream status
         * @param delayMs stream delay
         * @param netBand stream rate
         */
        @Override
        public void onRtmpStreamStatus(final int delayMs, final int netBand) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpStreamStatus delayMs:" + delayMs + "netBand:" + netBand);
                    if (tvRtmpStatus != null) {
                        tvRtmpStatus.setText(String.format(getString(R.string.str_rtmp_status), delayMs + "ms", netBand / 1024 / 8 + "kb/s"));
                    }
                }
            });
        }

        /**
         * RTMP stream failed
         * @param code code
         */
        @Override
        public void onRtmpStreamFailed(final int code) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("Call onRtmpStreamFailed code:" + code);
                    if (tvRtmpStatus != null) {
                        tvRtmpStatus.setText("Stream failed");
                    }
                }
            });
        }

        /**
         * RTMP stream close
         */
        @Override
        public void onRtmpStreamClosed() {
            Log.d("RTMP", "onRtmpStreamClosed ");
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("Call onRtmpStreamClosed ");
                    if (tvRtmpStatus != null) {
                        tvRtmpStatus.setText("Stream closed");
                    }
                }
            });
        }

        /**
         * RTC create
         * @param code 0: connect succeed
         */
        @Override
        public void onRTCCreateLineResult(final int code, String s) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call  onRTCCreateLineResult  code:" + code);
                    if (code == 0) {
                        if (tvRtcOk != null) {
                            tvRtcOk.setText(R.string.str_rtc_connect_success);
                        }
                    } else {
                        if (tvRtcOk != null) {
                            tvRtcOk.setText(ARUtils.getErrString(code));
                        }
                    }
                }
            });
        }

        /**
         * Guest apply to line
         *
         * @param strLivePeerID live peer id
         * @param strCustomID custom id
         * @param strUserData user data
         */
        @Override
        public void onRTCApplyToLine(final String strLivePeerID, final String strCustomID, final String strUserData) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCApplyToLine  strLivePeerID:" + strLivePeerID + " strCustomID:" + strCustomID + " strUserData:" + strUserData);
                    try {
                        JSONObject jsonObject = new JSONObject(strUserData);
                        if (line_dialog != null && lineListener != null && mHosterKit != null) {
                            lineListener.AddAudioGuest(new LineBean(strLivePeerID, jsonObject.getString("nickName"), false), mHosterKit);
                            tvLineList.setSelected(true);
                        }
                        applyLineList.add(strLivePeerID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        /**
         * Hang up
         * @param strLivePeerID live peer id
         */
        @Override
        public void onRTCCancelLine(final int nCode, final String strLivePeerID) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCCancelLine  strLivePeerID:" + strLivePeerID + "nCode:" + nCode);

                    if (nCode == 602) {
                        ToastUtil.show("The maximum connections");
                    }
                    if (nCode == 0) {
                        if (line_dialog != null && lineListener != null) {
                            lineListener.RemoveGuest(strLivePeerID);
                        }
                        if (applyLineList.contains(strLivePeerID)) {
                            applyLineList.remove(strLivePeerID);
                        }
                        if (applyLineList.size() == 0) {
                            tvLineList.setSelected(false);
                        }
                    }
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
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCOpenRemoteAudioLine  strLivePeerID:" + strLivePeerId + " strUserId:" + strUserId + " strUserData:" + strUserData);
                    try {
                        JSONObject jsonObject = new JSONObject(strUserData);
                        audioLineAdapter.addData(new LineBean(strLivePeerId, jsonObject.getString("nickName"), false));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        @Override
        public void onRTCCloseRemoteAudioLine(final String strLivePeerId, final String strUserId) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCCloseRemoteAudioLine  strLivePeerID:" + strLivePeerId + " strUserId:" + strUserId);
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
        public void onRTCLocalAudioActive(final int leave) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("RTC", "onRTLocalAudioActive leave:" + leave);
                }
            });
        }


        @Override
        public void onRTCRemoteAudioActive(final String strLivePeerId, final String strUserId, final int nTime) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("RTC", "onRTCAudioActive  strLivePeerID:" + strLivePeerId + " strUserId:" + strUserId + " nTime:" + nTime);
                }
            });
        }

        @Override
        public void onRTCRemoteAVStatus(final String s, boolean b, boolean b1) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCRemoteAVStatus peerID:" + s);
                }
            });
        }

        /**
         * RTC connection close
         * @param code 207: please come to AnyRTC website
         * @param s Reason
         */
        @Override
        public void onRTCLineClosed(final int code, String s) {
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCLineClosedLine  code:" + code);
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
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCUserMessage  nType:" + nType + " strUserId:" + strCustomID + " strCustomName:" + strCustomName + " strCustomHeader:" + strCustomHeader + " strMessage:" + strMessage);
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
            AudioHosterActivity.this.runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    printLog("回调：onRTCMemberNotify strServerId:" + strServerId + "strRoomId:" + strRoomId + "totalMembers:" + totalMembers);
                    tvMemberNum.setText("Audience: " + totalMembers + "");
                }
            });
        }
    };

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_close:
                ShowExitDialog();
                break;
            case R.id.iv_message:
                showChatLayout();
                break;
            case R.id.tv_line_list:
                if (isShowLineList) {
                    if (line_dialog != null) {
                        line_dialog.hide();
                        isShowLineList = false;
                    }
                } else {
                    if (line_dialog != null) {
                        line_dialog.show();
                        tvLineList.setSelected(false);
                        isShowLineList = true;
                    }
                }
                break;
            case R.id.btn_log:
                rl_log_layout.setVisibility(View.VISIBLE);
                break;
            case R.id.ibtn_close_log:
                rl_log_layout.setVisibility(View.GONE);
                break;
        }
    }

    private void initLineFragment() {
        CustomDialog.Builder builder = new CustomDialog.Builder(this);
        builder.setContentView(R.layout.item_line_list)
                .setAnimId(R.style.AnimBottom)
                .setGravity(Gravity.BOTTOM)
                .setLayoutParams(WindowManager.LayoutParams.MATCH_PARENT, DisplayUtils.getScreenHeightPixels(this) / 3)
                .setBackgroundDrawable(true)
                .build();
        line_dialog = builder.show(new CustomDialog.Builder.onInitListener() {
            @Override
            public void init(CustomDialog view) {
                if (lineFragment == null) {
                    lineFragment = new LineFragment();

                }
            }
        });
        line_dialog.hide();
        line_dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isShowLineList = false;
            }
        });
    }

    public void SetLineListener(HosterActivity.LineListener mLineListener) {
        this.lineListener = mLineListener;
    }


    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (mHosterKit != null) {
            mHosterKit.hangupRTCLine(audioLineAdapter.getItem(position).peerId);
            audioLineAdapter.remove(position);
        }
    }
}