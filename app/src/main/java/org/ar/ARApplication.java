package org.ar;

import android.app.Application;

import org.ar.utils.NameUtils;
import org.ar.rtmpc_hybrid.ARRtmpcEngine;

public class ARApplication extends Application {

    public static ARApplication mARApplication;
    private static String NickName = "";
    public static String LIVE_ID = (int) ((Math.random() * 9 + 1) * 100000) + "";//直播间ID

    @Override
    public void onCreate() {
        super.onCreate();
        mARApplication = this;
        NickName = NameUtils.getNickName();
        ARRtmpcEngine.Inst().initEngine(getApplicationContext(), DeveloperInfo.APPID, DeveloperInfo.APPTOKEN);
    }

    public static Application App() {
        return mARApplication;
    }

    public static String getNickName() {
        return NickName;
    }
}