package com.zhengyuan.emcarlocation03;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.lang.ref.WeakReference;

/**
 * Created by 林亮 on 2018/10/16
 */

public class ScreenManager {
    private Context mContext;
    private static ScreenManager mScreenManager;
    // 使用弱引用，防止内存泄漏c
    private WeakReference<Activity> mActivityRef;

    private ScreenManager(Context mContext) {
        this.mContext = mContext;
    }

    public static ScreenManager getScreenManagerInstance(Context context) {
        if (mScreenManager == null) {
            mScreenManager = new ScreenManager(context);
        }
        return mScreenManager;
    }

    // 获得SinglePixelActivity的引用
    public void setSingleActivity(Activity mActivity) {
        mActivityRef = new WeakReference<>(mActivity);
    }

    // 启动SinglePixelActivity
    public void startActivity() {
        Intent intent = new Intent(mContext, SinglePixelActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    // 结束SinglePixelActivity
    public void finishActivity() {
        if (mActivityRef != null) {
            Activity mActivity = mActivityRef.get();
            if (mActivity != null) {
                mActivity.finish();
            }
        }
    }
}