package ru.alt255.nuclearhm3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.*
import com.bumptech.glide.Glide
import ru.alt255.nuclearhm3.databinding.ActivityMainBinding
import ru.alt255.nuclearhm3.workers.WorkerA
import ru.alt255.nuclearhm3.workers.WorkerB
import ru.alt255.nuclearhm3.workers.WorkerC

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var workManager: WorkManager
    private var currentChainId: String? = null

    companion object {
        private const val TAG = "WorkChain"
        private const val CHAIN_TAG_PREFIX = "work_chain_"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Разрешение на уведомления получено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Без разрешения уведомления не будут показываться", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        workManager = WorkManager.getInstance(this)
        setupGifs()
        requestNotificationPermission()
        setupWorkObserver()
        binding.startButton.setOnClickListener { startWorkChain() }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {}
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun setupGifs() {
        Glide.with(this).load(R.drawable.rotate_1).into(binding.gifWorkerA)
        Glide.with(this).load(R.drawable.rotate_2).into(binding.gifWorkerB)
        Glide.with(this).load(R.drawable.rotate_3).into(binding.gifWorkerC)
    }

    private fun setupWorkObserver() {
        workManager.getWorkInfosByTagLiveData(TAG).observe(this) { workInfos ->
            Log.d(TAG, "Received workInfos: ${workInfos.size}")

            val activeWork = workInfos
                .filter { it.state != WorkInfo.State.CANCELLED }
                .sortedByDescending { it.runAttemptCount }

            val latestWorkA = activeWork.find { it.tags.contains("worker_a") }
            val latestWorkB = activeWork.find { it.tags.contains("worker_b") }
            val latestWorkC = activeWork.find { it.tags.contains("worker_c") }

            latestWorkA?.let { handleWorkerA(it) }
            latestWorkB?.let { handleWorkerB(it) }
            latestWorkC?.let { handleWorkerC(it) }
        }
    }

    private fun handleWorkerA(info: WorkInfo) {
        Log.d(TAG, "handleWorkerA: ${info.state}")
        when (info.state) {
            WorkInfo.State.RUNNING -> {
                binding.gifWorkerA.visibility = View.VISIBLE
                binding.numbersView.text = "Генерация данных..."
            }
            WorkInfo.State.SUCCEEDED -> {
                binding.gifWorkerA.visibility = View.GONE
                val numbers = info.outputData.getString("numbers")
                binding.numbersView.text = "Сгенерированы: $numbers"
                Log.d(TAG, "WorkerA succeeded with numbers: $numbers")
            }
            WorkInfo.State.FAILED -> {
                binding.gifWorkerA.visibility = View.GONE
                resetUIOnError("Ошибка генерации данных")
                Log.e(TAG, "WorkerA failed")
            }
            else -> {}
        }
    }

    private fun handleWorkerB(info: WorkInfo) {
        Log.d(TAG, "handleWorkerB: ${info.state}")
        when (info.state) {
            WorkInfo.State.RUNNING -> {
                binding.gifWorkerB.visibility = View.VISIBLE
                binding.numbersView.text = "Фильтрация данных..."
            }
            WorkInfo.State.SUCCEEDED -> {
                binding.gifWorkerB.visibility = View.GONE
                val numbers = info.outputData.getString("filtered_numbers")
                binding.numbersView.text = "Четные: $numbers"
                Log.d(TAG, "WorkerB succeeded with filtered numbers: $numbers")
            }
            WorkInfo.State.FAILED -> {
                binding.gifWorkerB.visibility = View.GONE
                resetUIOnError("Ошибка фильтрации данных")
                Log.e(TAG, "WorkerB failed")
            }
            else -> {}
        }
    }

    private fun handleWorkerC(info: WorkInfo) {
        Log.d(TAG, "handleWorkerC: ${info.state}")
        when (info.state) {
            WorkInfo.State.RUNNING -> {
                binding.gifWorkerC.visibility = View.VISIBLE
                binding.numbersView.text = "Анализ данных..."
            }
            WorkInfo.State.SUCCEEDED -> {
                binding.gifWorkerC.visibility = View.GONE
                val average = info.outputData.getString("average")
                binding.numbersView.text = "Среднее: $average"
                binding.startButton.visibility = View.VISIBLE
                Log.d(TAG, "WorkerC succeeded with average: $average, button should be visible")
            }
            WorkInfo.State.FAILED -> {
                binding.gifWorkerC.visibility = View.GONE
                resetUIOnError("Ошибка анализа данных")
                Log.e(TAG, "WorkerC failed")
            }
            else -> {}
        }
    }

    private fun resetUIOnError(errorMessage: String) {
        binding.numbersView.text = errorMessage
        binding.startButton.visibility = View.VISIBLE
        binding.gifWorkerA.visibility = View.GONE
        binding.gifWorkerB.visibility = View.GONE
        binding.gifWorkerC.visibility = View.GONE
    }

    private fun startWorkChain() {
        Log.d(TAG, "Starting work chain")

        currentChainId?.let { previousChainId ->
            workManager.cancelAllWorkByTag(previousChainId)
        }

        currentChainId = "${CHAIN_TAG_PREFIX}${System.currentTimeMillis()}"
        resetUIForNewChain()

        val workA = OneTimeWorkRequestBuilder<WorkerA>()
            .addTag(currentChainId!!)
            .addTag("worker_a")
            .build()

        val workB = OneTimeWorkRequestBuilder<WorkerB>()
            .addTag(currentChainId!!)
            .addTag("worker_b")
            .build()

        val workC = OneTimeWorkRequestBuilder<WorkerC>()
            .addTag(currentChainId!!)
            .addTag("worker_c")
            .build()

        workManager.beginWith(workA)
            .then(workB)
            .then(workC)
            .enqueue()

        observeWorkChain(currentChainId!!)

        Log.d(TAG, "Work chain enqueued: workA=${workA.id}, workB=${workB.id}, workC=${workC.id}")
    }

    private fun observeWorkChain(chainId: String) {
        workManager.getWorkInfosByTagLiveData(chainId).removeObservers(this)

        workManager.getWorkInfosByTagLiveData(chainId).observe(this) { workInfos ->
            Log.d(TAG, "Received workInfos for chain $chainId: ${workInfos.size}")

            workInfos.forEach { info ->
                Log.d(TAG, "WorkInfo: id=${info.id}, state=${info.state}, tags=${info.tags}")

                when {
                    info.tags.contains("worker_a") -> handleWorkerA(info)
                    info.tags.contains("worker_b") -> handleWorkerB(info)
                    info.tags.contains("worker_c") -> handleWorkerC(info)
                }
            }
        }
    }

    private fun resetUIForNewChain() {
        binding.startButton.visibility = View.GONE
        binding.numbersView.text = ""
        binding.gifWorkerA.visibility = View.GONE
        binding.gifWorkerB.visibility = View.GONE
        binding.gifWorkerC.visibility = View.GONE
    }
}