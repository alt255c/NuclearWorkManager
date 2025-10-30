package ru.alt255.nuclearhm3.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import ru.alt255.nuclearhm3.notifications.NotificationHelper
import ru.alt255.nuclearhm3.notifications.showNotification
import java.io.File

class WorkerC(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            kotlinx.coroutines.delay(3000)

            val filePath = inputData.getString("file_path")
            val file = File(filePath!!)

            if (file.exists()) {
                val numbers = file.readLines().map { it.toInt() }
                val average = numbers.average()

                showNotification(
                    NotificationHelper.NOTIFICATION_ID_WORKER_C,
                    "Анализ завершен",
                    "Среднее значение: ${"%.2f".format(average)}"
                )

                file.delete()

                Result.success(Data.Builder()
                    .putString("average", "%.2f".format(average))
                    .build())
            } else {
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e("WorkerC", "Error", e)
            Result.failure()
        }
    }
}