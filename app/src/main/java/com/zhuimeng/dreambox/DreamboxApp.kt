package com.zhuimeng.dreambox

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.zhuimeng.dreambox.data.MigrationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DreamboxApp : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Log.d("DreamboxApp", "应用启动, Hilt 注入完成, workerFactory=$workerFactory")

        // 数据迁移: v0.1.2 WidgetConfig → v0.1.3 Profile
        MigrationHelper.migrateIfNeeded(this)

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

        // 清理旧版 WorkManager 任务（升级安装后可能会有旧的 pending work）
        MigrationHelper.cleanupStaleWork(this)
    }
}
