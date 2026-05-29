package com.zhuimeng.dreambox

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DreamboxApp : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Log.d("DreamboxApp", "应用启动, Hilt 注入完成, workerFactory=$workerFactory")
        try {
            WorkManager.initialize(
                this,
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build()
            )
            Log.d("DreamboxApp", "WorkManager 初始化成功")
        } catch (e: IllegalStateException) {
            Log.w("DreamboxApp", "WorkManager 已初始化: ${e.message}")
        }
    }
}
