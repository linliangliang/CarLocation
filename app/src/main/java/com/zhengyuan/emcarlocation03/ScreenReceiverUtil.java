package com.zhengyuan.emcarlocation03;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by 林亮 on 2018/10/16
 */

public class ScreenReceiverUtil {
    private Context mContext;
    // 锁屏广播接收器
    private ScreenBroadcastReceiver mScreenReceiver;
    // 屏幕状态改变回调接口
    private ScreenStateListener mStateReceiverListener;

    public ScreenReceiverUtil(Context mContext) {
        this.mContext = mContext;
    }

    public void setScreenReceiverListener(ScreenStateListener mStateReceiverListener) {
        this.mStateReceiverListener = mStateReceiverListener;
        // 动态启动广播接收器
        this.mScreenReceiver = new ScreenBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(mScreenReceiver, filter);
    }

    public void stopScreenReceiverListener() {
        if (mContext != null && mScreenReceiver != null)
            mContext.unregisterReceiver(mScreenReceiver);
    }

    public class ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mStateReceiverListener == null) return;
            String action = intent.getAction();
            Log.d("KeepAppAlive", "SreenLockReceiver-->监听到系统广播：" + action);
            if (action == null) return;
            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    mStateReceiverListener.onScreenOn();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    mStateReceiverListener.onScreenOff();
                    break;
                case Intent.ACTION_USER_PRESENT:
                    mStateReceiverListener.onUserPresent();
                    break;
            }
        }
    }

    // 监听screen状态对外回调接口
    public interface ScreenStateListener {
        void onScreenOn();

        void onScreenOff();

        void onUserPresent();
    }
}