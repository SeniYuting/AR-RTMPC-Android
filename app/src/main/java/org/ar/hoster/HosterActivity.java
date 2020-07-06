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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.ar.BaseActivity;
import org.ar.ARApplication;
import org.ar.adapter.LiveMessageAdapter;
import org.ar.adapter.LogAdapter;
import org.ar.common.utils.ARAudioManager;
import org.ar.rtmpc.R;
import org.ar.model.LineBean;
import org.ar.model.MessageBean;
import org.ar.utils.ARUtils;
import org.ar.utils.DisplayUtils;
import org.ar.utils.ToastUtil;
import org.ar.widgets.ARVideoView;
import org.ar.widgets.CustomDialog;
import org.ar.widgets.KeyboardDialogFragment;
import org.ar.common.enums.ARVideoCommon;
import org.ar.rtmpc_hybrid.ARRtmpcEngine;
import org.ar.rtmpc_hybrid.ARRtmpcHosterEvent;
import org.ar.rtmpc_hybrid.ARRtmpcHosterKit;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.VideoRenderer;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Host Activity
 */
public class HosterActivity extends BaseActivity {
    RelativeLayout rlRtmpcVideos, rl_log_layout;
    TextView tvTitle, tvRtmpOk, tvRtmpStatus, tvRtcOk, tvMemberNum;
    RecyclerView rvMsgList, rvLog;
    View viewSpace;
    ImageButton tvLineList;
    private ARRtmpcHosterKit mHosterKit;
    private ARVideoView mVideoView;
    private ARAudioManager mRtmpAudioManager;
    private LiveMessageAdapter mAdapter;
    private LogAdapter logAdapter;
    private CustomDialog line_dialog;
    private LineFragment lineFragment;
    private boolean isShowLineList = false;
    private LineListener lineListener;

    private String pushURL = "", pullURL = "";
    private String liveId = ARApplication.LIVE_ID;
    private String nickname = ARApplication.getNickName();
    private String userId = "host" + (int) ((Math.random() * 9 + 1) * 100000) + "";
    private List<String> applyLineList = new ArrayList<>();

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
            mVideoView.removeLocalVideoRender();
            mHosterKit.clean();
        }
        // Close RTMPAudioManager
        if (mRtmpAudioManager != null) {
            mRtmpAudioManager.stop();
            mRtmpAudioManager = null;
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_hoster;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initView(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        rlRtmpcVideos = findViewById(R.id.rl_rtmpc_videos);
        rl_log_layout = findViewById(R.id.rl_log_layout);
        rvLog = findViewById(R.id.rv_log);
        tvTitle = findViewById(R.id.tv_title);
        tvRtmpOk = findViewById(R.id.tv_rtmp_ok);
        tvRtmpStatus = findViewById(R.id.tv_rtmp_status);
        tvRtcOk = findViewById(R.id.tv_rtc_ok);
        rvMsgList = findViewById(R.id.rv_msg_list);
        viewSpace = findViewById(R.id.view_space);
        mImmersionBar.titleBar(viewSpace).init();
        tvMemberNum = findViewById(R.id.tv_member_num);
        tvLineList = findViewById(R.id.tv_line_list);

        pushURL = getIntent().getStringExtra("pushURL");
        pullURL = getIntent().getStringExtra("pullURL");
        initLineFragment();
        mAdapter = new LiveMessageAdapter();
        rvMsgList.setLayoutManager(new LinearLayoutManager(this));
        rvMsgList.setAdapter(mAdapter);

        logAdapter = new LogAdapter();
        rvLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter.bindToRecyclerView(rvLog);

        ARRtmpcEngine.Inst().getHosterOption().setVideoProfile(ARVideoCommon.ARVideoProfile.ARVideoProfile480x640);
        tvTitle.setText("Room ID: " + liveId);
        ARRtmpcEngine.Inst().getHosterOption().setVideoOrientation(ARVideoCommon.ARVideoOrientation.Portrait);
        mRtmpAudioManager = ARAudioManager.create(this);
        mRtmpAudioManager.start(new ARAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(ARAudioManager.AudioDevice audioDevice, Set<ARAudioManager.AudioDevice> set) {
            }
        });
        ARRtmpcEngine.Inst().getHosterOption().setMediaType(ARVideoCommon.ARMediaType.Video);

        mHosterKit = new ARRtmpcHosterKit(mHosterListener);
        mHosterKit.setAudioActiveCheck(true);

        mVideoView = new ARVideoView(rlRtmpcVideos, ARRtmpcEngine.Inst().egl(), this, false, true);
        mVideoView.setVideoViewLayout(false, Gravity.RIGHT, LinearLayout.VERTICAL);
        mVideoView.setVideoLayoutOnclickEvent(mBtnVideoCloseEvent);

        VideoRenderer render = mVideoView.openLocalVideoRender();
        mHosterKit.setLocalVideoCapturer(render.GetRenderPointer());
        mHosterKit.startPushRtmpStream(pushURL);
        mHosterKit.createRTCLine("", liveId, userId, getUserData(), getLiveInfo(pullURL, pullURL));
    }

    public String getLiveInfo(String pullUrl, String hlsUrl) {
        JSONObject liveInfo = new JSONObject();

        try {
            liveInfo.put("hosterId", userId);
            liveInfo.put("rtmpUrl", pullUrl);
            liveInfo.put("hlsUrl", hlsUrl);
            liveInfo.put("liveTopic", ARApplication.LIVE_ID);
            liveInfo.put("anyrtcId", ARApplication.LIVE_ID);
            liveInfo.put("isAudioLive", 0);
            liveInfo.put("hosterName", ARApplication.getNickName());
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

    /**
     * Close button
     */
    private ARVideoView.VideoLayoutOnclickEvent mBtnVideoCloseEvent = new ARVideoView.VideoLayoutOnclickEvent() {
        @Override
        public void onCloseVideoRender(View view, String strPeerId) {
            mHosterKit.hangupRTCLine(strPeerId);
        }
    };

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
                addChatMessageList(new MessageBean(MessageBean.VIDEO, nickname, text));
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
                if (mHosterKit != null) {
                    mHosterKit.stopRtmpStream();
                }
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
            HosterActivity.this.runOnUiThread(new Runnable() {
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
         * @param times times
         */
        @Override
        public void onRtmpStreamReconnecting(final int times) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpStreamReconnecting times:" + times);
                    if (tvRtmpStatus != null) {
                        tvRtmpStatus.setText(String.format(getString(R.string.str_reconnect_times), times));
                    }
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
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpStreamStatus delayMs:" + delayMs + "netBand:" + netBand);
                    if (tvRtmpStatus != null) {
                        tvRtmpStatus.setText(String.format(getString(R.string.str_rtmp_status), delayMs + "ms", netBand / 1024 / 8 + "kb/s"));
                    }
                }
            });
        }

        @Override
        public void onRtmpStreamFailed(final int code) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpStreamFailed code:" + code);
                    if (tvRtcOk != null) {
                        tvRtcOk.setText(R.string.str_rtmp_connect_failed);
                    }
                }
            });
        }

        @Override
        public void onRtmpStreamClosed() {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRtmpStreamClosed ");
                    finish();
                }
            });

        }

        @Override
        public void onRTCCreateLineResult(final int code, String s) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCCreateLineResult code:" + code);
                    if (tvRtcOk != null) {
                        if (code == 0) {
                            tvRtcOk.setText(R.string.str_rtc_connect_success);
                        } else {
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
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCApplyToLine  strLivePeerID:" + strLivePeerID + " strCustomID:" + strCustomID + " strUserData:" + strUserData);
                    try {
                        String userdata = URLDecoder.decode(strUserData);
                        JSONObject jsonObject = new JSONObject(userdata);
                        if (line_dialog != null && lineListener != null && mHosterKit != null && tvLineList != null) {
                            lineListener.AddGuest(new LineBean(strLivePeerID, jsonObject.getString("nickName"), false), mHosterKit);
                            tvLineList.setSelected(true);
                        }
                        applyLineList.add(strLivePeerID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onRTCCancelLine(final int nCode, final String strLivePeerID) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCCancelLine  strLivePeerID:" + strLivePeerID + "nCode:" + nCode);
                    if (nCode == 0) {
                        if (line_dialog != null && lineListener != null) {
                            lineListener.RemoveGuest(strLivePeerID);
                        }
                        if (applyLineList.contains(strLivePeerID)) {
                            applyLineList.remove(strLivePeerID);
                        }
                        if (applyLineList.size() == 0) {//小红点
                            tvLineList.setSelected(false);
                        }
                    }

                    if (nCode == 602) {
                        ToastUtil.show("The maximum connections");
                    }
                }
            });
        }

        @Override
        public void onRTCOpenRemoteVideoRender(final String strLivePeerId, final String strPublishId, final String strUserId, final String strUserData) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCOpenVideoRender  strPublishId:" + strPublishId + " strUserId:" + strUserId + " strUserData:" + strUserData);
                    final VideoRenderer render = mVideoView.openRemoteVideoRender(strLivePeerId);
                    if (null != render) {
                        mHosterKit.setRTCRemoteVideoRender(strPublishId, render.GetRenderPointer());
                    }
                }
            });
        }

        @Override
        public void onRTCCloseRemoteVideoRender(final String strLivePeerId, final String strPublishId, final String strUserId) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCCloseVideoRender  strPublishId:" + strPublishId + " strUserId:" + strUserId);
                    mHosterKit.setRTCRemoteVideoRender(strPublishId, 0);
                    mVideoView.removeRemoteRender(strLivePeerId);
                    if (line_dialog != null && lineListener != null) {
                        lineListener.RemoveGuest(strLivePeerId);
                    }
                }
            });
        }

        @Override
        public void onRTCOpenRemoteAudioLine(String s, String s1, String s2) {
        }

        @Override
        public void onRTCCloseRemoteAudioLine(String s, String s1) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        @Override
        public void onRTCLocalAudioActive(int i) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("RTC", "onRTLocalAudioActive ");
                }
            });
        }


        @Override
        public void onRTCRemoteAudioActive(String s, String s1, int i) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("RTC", "onRTCRemoteAudioActive ");
                }
            });
        }

        @Override
        public void onRTCRemoteAVStatus(final String s, boolean b, boolean b1) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCRemoteAVStatus peerID:" + s);
                }
            });
        }


        /**
         * RTC connection close
         * @param code 207: please come to AnyRTC website
         */
        @Override
        public void onRTCLineClosed(final int code, String s) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("RTC", "onRTCLineClosedLine  code:" + code);
                    if (code == 207) {
                        Toast.makeText(HosterActivity.this, getString(R.string.str_apply_anyrtc_account), Toast.LENGTH_LONG).show();
                        finish();
                    }
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
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCUserMessage  nType:" + nType + " strUserId:" + strCustomID + " strCustomName:" + strCustomName + " strCustomHeader:" + strCustomHeader + " strMessage:" + strMessage);
                    addChatMessageList(new MessageBean(MessageBean.VIDEO, strCustomName, strMessage));
                }
            });
        }

        /**
         * Number of people watching live
         * @param totalMembers number of people
         */
        @Override
        public void onRTCMemberNotify(final String strServerId, final String strRoomId, final int totalMembers) {
            HosterActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printLog("Call onRTCMemberNotify strServerId:" + strServerId + "strRoomId:" + strRoomId + "totalMembers:" + totalMembers);
                    tvMemberNum.setText("Audience: " + totalMembers + "");
                }
            });
        }
    };

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_camare:
                if (mHosterKit == null) {
                    return;
                }
                mHosterKit.switchCamera();
                break;
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

    public interface LineListener {
        void AddAudioGuest(LineBean lineBean, ARRtmpcHosterKit hosterKit);//Add audio guest

        void AddGuest(LineBean lineBean, ARRtmpcHosterKit hosterKit);//Add guest

        void RemoveGuest(String peerid);//Remove guest
    }

    public void SetLineListener(LineListener mLineListener) {
        this.lineListener = mLineListener;
    }

    public void closeLineDialog() {
        if (line_dialog != null) {
            line_dialog.hide();
        }
    }
}