package com.zhengyuan.emcarlocation03;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by 林亮 on 2018/10/16
 */

public class SinglePixelActivity extends Activity {

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Log.i("Location", "++++");
            }
            super.handleMessage(msg);
        }
    };

    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        /*Window mWindow = getWindow();
        mWindow.setGravity(Gravity.START | Gravity.TOP);
        WindowManager.LayoutParams attrParams = mWindow.getAttributes();
        attrParams.x = 0;
        attrParams.y = 0;
        attrParams.height = 1;
        attrParams.width = 1;
        mWindow.setAttributes(attrParams);
        //避免卡死，允许点击外部消失
        setFinishOnTouchOutside(true);*/
        // 绑定SinglePixelActivity到ScreenManager
        ScreenManager.getScreenManagerInstance(this).setSingleActivity(this);
        timer.schedule(timerTask, 1000, 500);//延时1s，每隔500毫秒执行一次run方法
    }

    @Override
    protected void onDestroy() {
        Log.i("Location_", "onDestroy");
        timer.cancel();
        if (!MainActivity.isAPPALive(getApplicationContext(), getPackageName())) {
            Intent intentAlive = new Intent(this, MainActivity.class);
            intentAlive.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentAlive);
        }
        super.onDestroy();
    }
}