/*
 * Copyright (C) $year Huajian Jiang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huajianjiang.flutter.plugins.xunfeimsc;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import io.flutter.Log;

public class WakeupService extends Service implements WakeupController.OnWakeupListener {
    private static final String TAG = WakeupService.class.getSimpleName();
    private WakeupController controller;
    private ScreenStateReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        receiver = new ScreenStateReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);

        controller = new WakeupController(getApplicationContext());
        controller.setListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG , "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG , "onDestroy");
        controller.destroy();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void turnScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        Log.d(TAG, "turnScreenOn: isScreenOn=" + pm.isScreenOn());
        if (!pm.isScreenOn()) {
            // 唤醒屏幕
            PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
            wakeLock.acquire(60 * 1000); // 1 分钟唤醒超时
            // 解锁屏幕
            KeyguardManager keyguardManager = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock(TAG);
            keyguardLock.disableKeyguard();
        }
    }

    @Override
    public void onWakeup() {
        Log.d(TAG, "onWakeup");
        turnScreenOn();
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ObjectsCompat.equals(action, Intent.ACTION_SCREEN_ON)) {
                Log.d(TAG, "SCREEN_ON");
                // 手动唤醒情况下取消语音唤醒操作
                controller.cancel();
            } else if (ObjectsCompat.equals(action, Intent.ACTION_SCREEN_OFF)) {
                Log.d(TAG, "SCREEN_OFF");
                controller.startRecord();
            }
        }
    }

}
