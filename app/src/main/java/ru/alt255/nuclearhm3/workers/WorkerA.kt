package ru.alt255.nuclearhm3.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import ru.alt255.nuclearhm3.notifications.NotificationHelper
import ru.alt255.nuclearhm3.notifications.showNotification
import kotlin.random.Random
import java.io.File

class WorkerA(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            kotlinx.coroutines.delay(2000)

            val numbers = List(10) { Random.nextInt(100) }
            val file = File(applicationContext.cacheDir, "numbers.txt")
            file.writeText(numbers.joinToString("\n"))

            showNotification(
                NotificationHelper.NOTIFICATION_ID_WORKER_A,
                "Данные сгенерированы",
                "Числа: ${numbers.joinToString()}"
            )

            Result.success(Data.Builder()
                .putString("numbers", numbers.joinToString())
                .putString("file_path", file.absolutePath)
                .build())

        } catch (e: Exception) {
            Log.e("WorkerA", "Error", e)
            Result.failure()
        }
    }
}