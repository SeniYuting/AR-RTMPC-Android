package org.ar.utils;

import org.ar.ARApplication;
import org.ar.rtmpc.R;

public enum ARUtils {
    AnyRTC_OK(0),
    AnyRTC_UNKNOW(1),
    AnyRTC_EXCEPTION(2),


    AnyRTC_NET_ERR(100),
    AnyRTC_LIVE_ERR(101),


    AnyRTC_BAD_REQ(201),
    AnyRTC_AUTH_FAIL(202),
    AnyRTC_NO_USER(203),
    AnyRTC_SQL_ERR(204),
    AnyRTC_ARREARS(205),
    AnyRTC_LOCKED(206),
    AnyRTC_FORCE_EXIT(207);

    private int value;

    ARUtils(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Get error string
     *
     * @param value value
     */
    public static String getErrString(int value) {
        if (value == AnyRTC_OK.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_ok);
        } else if (value == AnyRTC_UNKNOW.getValue()) {
            return ARApplication.App().getString(R.string.str_unknow_exception);
        } else if (value == AnyRTC_EXCEPTION.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_exception);
        } else if (value == AnyRTC_NET_ERR.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_net_err);
        } else if (value == AnyRTC_LIVE_ERR.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_live_err);
        } else if (value == AnyRTC_BAD_REQ.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_bad_req);
        } else if (value == AnyRTC_AUTH_FAIL.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_auth_fail);
        } else if (value == AnyRTC_NO_USER.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_no_user);
        } else if (value == AnyRTC_SQL_ERR.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_sql_err);
        } else if (value == AnyRTC_ARREARS.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_arrears);
        } else if (value == AnyRTC_LOCKED.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_locked);
        } else if (value == AnyRTC_FORCE_EXIT.getValue()) {
            return ARApplication.App().getString(R.string.str_anyrtc_force_exit);
        } else {
            return "";
        }
    }
}
