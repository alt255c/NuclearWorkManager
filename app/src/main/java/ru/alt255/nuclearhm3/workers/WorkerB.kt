package ru.alt255.nuclearhm3.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import ru.alt255.nuclearhm3.notifications.NotificationHelper
import ru.alt255.nuclearhm3.notifications.showNotification
import java.io.File

class WorkerB(
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
                val evenNumbers = numbers.filter { it % 2 == 0 }

                file.writeText(evenNumbers.joinToString("\n"))

                showNotification(
                    NotificationHelper.NOTIFICATION_ID_WORKER_B,
                    "Данные отфильтрованы",
                    "Четные числа: ${evenNumbers.joinToString()}"
                )

                Result.success(Data.Builder()
                    .putString("filtered_numbers", evenNumbers.joinToString())
                    .putString("file_path", filePath)
                    .build())
            } else {
                Result.failure()
            }

        } catch (e: Exception) {
            Log.e("WorkerB", "Error", e)
            Result.failure()
        }
    }
}