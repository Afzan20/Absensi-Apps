package com.example.absensiapps.ui.employee

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.app.AlertDialog
import android.widget.ImageView
import android.view.ViewGroup

import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

import com.example.absensiapps.databinding.ActivityFaceDetectionBinding
import com.example.absensiapps.utils.FaceAligner
import com.example.absensiapps.utils.MobileFaceNetEngine
import com.example.absensiapps.utils.applySystemBarInsets
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.math.sqrt


class FaceDetectionActivity : AppCompatActivity() {

    // =====================================
    // VIEW BINDING
    // =====================================

    private lateinit var binding: ActivityFaceDetectionBinding


    // =====================================
    // FACE DETECTOR
    // =====================================

    private lateinit var faceDetector: FaceDetector


    // =====================================
    // CAMERA
    // =====================================

    private lateinit var cameraExecutor: ExecutorService

    private var cameraProvider: ProcessCameraProvider? = null

    private var imageAnalysis: ImageAnalysis? = null


    // =====================================
    // MOBILE FACENET
    // =====================================

    private var faceEngine: MobileFaceNetEngine? = null


    // =====================================
    // FRAME CONTROL
    // =====================================

    private val frameBusy = AtomicBoolean(false)

    @Volatile
    private var activityDestroyed = false

    private var embeddingTestCompleted = false

    private var lastAnalysisAtMs = 0L


    // =====================================
    // CAMERA PERMISSION
    // =====================================

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (activityDestroyed || isFinishing) {
                return@registerForActivityResult
            }

            if (granted) {

                startCamera()

            } else {

                binding.tvCameraStatus.text =
                    "Izin kamera diperlukan untuk melanjutkan."

                binding.tvCameraStatus.setTextColor(Color.RED)

                binding.tvFaceStatus.text =
                    "Akses kamera belum diberikan."
            }
        }


    // =====================================
    // ON CREATE
    // =====================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        Log.d(
            "FACE_TEST",
            "1. FaceDetectionActivity onCreate dipanggil"
        )

        enableEdgeToEdge()

        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.root.applySystemBarInsets()

        // Executor khusus analisis kamera
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Inisialisasi ML Kit
        setupFaceDetector()

        // Inisialisasi MobileFaceNet
        setupFaceEngine()

        // Setup tombol
        setupButtons()

        // Setup tombol back Android
        setupBackPress()

        // Periksa izin kamera
        checkCameraPermission()
    }


    // =====================================
    // INITIALIZE FACE DETECTOR
    // =====================================

    private fun setupFaceDetector() {

        val options = FaceDetectorOptions.Builder()

            .setPerformanceMode(
                FaceDetectorOptions.PERFORMANCE_MODE_FAST
            )

            .setLandmarkMode(
                FaceDetectorOptions.LANDMARK_MODE_ALL
            )

            .setClassificationMode(
                FaceDetectorOptions.CLASSIFICATION_MODE_ALL
            )

            .build()

        faceDetector = FaceDetection.getClient(options)

        Log.d(
            "FACE_TEST",
            "Face Detector berhasil diinisialisasi"
        )
    }


    // =====================================
    // INITIALIZE MOBILEFACENET
    // =====================================

    private fun setupFaceEngine() {

        Log.d(
            "FACE_TEST",
            "2. setupFaceEngine mulai"
        )

        try {

            faceEngine = MobileFaceNetEngine(this)

            Log.d(
                "FACE_TEST",
                faceEngine!!.getModelInfo()
            )

            updateFaceStatus(
                "MobileFaceNet berhasil dimuat",
                Color.rgb(49, 169, 104)
            )

        } catch (e: Exception) {

            faceEngine = null

            Log.e(
                "FACE_TEST",
                "Gagal memuat MobileFaceNet",
                e
            )

            updateFaceStatus(
                "Model gagal dimuat: ${e.message}",
                Color.RED
            )
        }
    }


    // =====================================
    // BUTTON SETUP
    // =====================================

    private fun setupButtons() {

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }


    // =====================================
    // BACK BUTTON
    // =====================================

    private fun setupBackPress() {

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    finish()
                }
            }
        )
    }


    // =====================================
    // CHECK CAMERA PERMISSION
    // =====================================

    private fun checkCameraPermission() {

        when {

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {

                startCamera()
            }

            else -> {

                cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }


    // =====================================
    // START CAMERA
    // =====================================

    private fun startCamera() {

        if (activityDestroyed || isFinishing || isDestroyed) {
            return
        }

        Log.d(
            "FACE_TEST",
            "3. startCamera dipanggil"
        )

        binding.tvCameraStatus.text =
            "Menghubungkan kamera..."

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            if (activityDestroyed || isFinishing || isDestroyed) {
                return@addListener
            }

            try {

                val provider = cameraProviderFuture.get()

                cameraProvider = provider

                val rotation =
                    binding.previewView.display?.rotation
                        ?: Surface.ROTATION_0


                // =====================================
                // PREVIEW
                // =====================================

                val preview = Preview.Builder()

                    .setTargetRotation(rotation)

                    .build()

                    .also {

                        it.setSurfaceProvider(
                            binding.previewView.surfaceProvider
                        )
                    }


                // =====================================
                // IMAGE ANALYSIS
                // =====================================

                val analysis = ImageAnalysis.Builder()

                    .setTargetRotation(rotation)

                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )

                    .build()

                imageAnalysis = analysis

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->

                    if (activityDestroyed) {

                        imageProxy.close()

                        return@setAnalyzer
                    }

                    Log.d(
                        "FACE_FLOW",
                        "ANALYZER DIPANGGIL: " +
                                "${imageProxy.width}x${imageProxy.height}"
                    )

                    analyzeImage(imageProxy)
                }


                // =====================================
                // FRONT CAMERA
                // =====================================

                val cameraSelector =
                    CameraSelector.DEFAULT_FRONT_CAMERA

                provider.unbindAll()

                provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    analysis
                )

                binding.tvCameraStatus.text =
                    "Kamera aktif"

                binding.tvCameraStatus.setTextColor(
                    Color.rgb(49, 169, 104)
                )

                Log.d(
                    "FACE_TEST",
                    "Kamera depan berhasil diaktifkan"
                )

            } catch (e: Exception) {

                Log.e(
                    "FACE_TEST",
                    "Gagal membuka kamera",
                    e
                )

                binding.tvCameraStatus.text =
                    "Gagal membuka kamera: ${e.localizedMessage}"

                binding.tvCameraStatus.setTextColor(Color.RED)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    // =====================================
    // ROTATE BITMAP
    // =====================================

    private fun rotateBitmap(
        source: Bitmap,
        rotationDegrees: Int
    ): Bitmap {

        if (rotationDegrees == 0) {
            return source
        }

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }

        return Bitmap.createBitmap(
            source,
            0,
            0,
            source.width,
            source.height,
            matrix,
            true
        )
    }


    // =====================================
    // ANALYZE IMAGE
    // =====================================

    private fun analyzeImage(imageProxy: ImageProxy) {

        // Jangan proses frame setelah Activity ditutup
        if (activityDestroyed) {

            imageProxy.close()

            return
        }

        Log.d(
            "FACE_FLOW",
            "MASUK ANALYZE IMAGE"
        )

        val currentTime = SystemClock.elapsedRealtime()

        // Batasi analisis setiap 250 ms
        if (currentTime - lastAnalysisAtMs < 250L) {

            imageProxy.close()

            return
        }

        lastAnalysisAtMs = currentTime

        // Cegah proses frame bertumpuk
        if (!frameBusy.compareAndSet(false, true)) {

            imageProxy.close()

            return
        }

        var rawBitmap: Bitmap? = null

        var uprightBitmap: Bitmap? = null

        try {

            Log.d(
                "FACE_TEST",
                "4. analyzeImage berjalan"
            )


            // =====================================
            // 1. CONVERT IMAGEPROXY TO BITMAP
            // =====================================

            rawBitmap = imageProxy.toBitmap()

            if (activityDestroyed) {
                return
            }


            // =====================================
            // 2. ROTATE BITMAP
            // =====================================

            uprightBitmap = rotateBitmap(
                rawBitmap,
                imageProxy.imageInfo.rotationDegrees
            )

            if (uprightBitmap !== rawBitmap) {

                rawBitmap.recycle()

                rawBitmap = null
            }


            // =====================================
            // 3. CREATE INPUT IMAGE
            // =====================================

            val inputImage = InputImage.fromBitmap(
                uprightBitmap,
                0
            )


            // =====================================
            // 4. RUN ML KIT FACE DETECTION
            // =====================================

            // Menunggu hasil ML Kit di cameraExecutor.
            // Tidak menggunakan callback cameraExecutor
            // yang berisiko dipanggil setelah executor mati.

            val faces = Tasks.await(
                faceDetector.process(inputImage)
            )

            Log.d(
                "FACE_TEST",
                "Jumlah wajah terdeteksi: ${faces.size}"
            )

            if (activityDestroyed || isFinishing || isDestroyed) {
                return
            }


            // =====================================
            // 5. PROCESS DETECTION RESULT
            // =====================================

            when {

                faces.isEmpty() -> {

                    binding.faceLandmarkOverlay.clear()

                    updateFaceStatus(
                        "Wajah belum terdeteksi",
                        Color.rgb(217, 155, 41)
                    )
                }

                faces.size > 1 -> {

                    binding.faceLandmarkOverlay.clear()

                    updateFaceStatus(
                        "${faces.size} wajah terdeteksi. " +
                                "Pastikan hanya satu orang.",
                        Color.RED
                    )
                }


                else -> {
                    updateFaceStatus(
                        "Satu wajah terdeteksi",
                        Color.rgb(49, 169, 104)
                    )

                    val face = faces.first()

                    val minimumFaceSize =
                        (uprightBitmap.width * 0.15f).toInt()

                    if (
                        face.boundingBox.width() < minimumFaceSize ||
                        face.boundingBox.height() < minimumFaceSize
                    ) {
                        binding.faceLandmarkOverlay.clear()

                        updateFaceStatus(
                            "Dekatkan wajah ke kamera",
                            Color.rgb(217, 155, 41)
                        )
                    } else {
                        // Landmark diperbarui setiap frame.
                        extractFaceLandmarks(
                            face,
                            uprightBitmap
                        )
                    }
                }
            }

        } catch (e: Exception) {

            Log.e(
                "FACE_TEST",
                "Camera analysis error",
                e
            )

            if (!activityDestroyed) {

                updateFaceStatus(
                    "Analisis gagal: ${e.localizedMessage}",
                    Color.RED
                )
            }

        } finally {

            // =====================================
            // 6. RECYCLE BITMAP
            // =====================================

            uprightBitmap?.let {

                if (!it.isRecycled) {
                    it.recycle()
                }
            }

            rawBitmap?.let {

                if (!it.isRecycled) {
                    it.recycle()
                }
            }


            // =====================================
            // 7. CLOSE IMAGEPROXY
            // =====================================

            imageProxy.close()

            frameBusy.set(false)
        }
    }


    // =====================================
    // EXTRACT FIVE FACE LANDMARKS
    // =====================================

    private fun extractFaceLandmarks(
        face: Face,
        bitmap: Bitmap
    ) {

        try {

            // =====================================
            // 1. GET LANDMARK POSITIONS
            // =====================================

            val leftEye = face.getLandmark(
                FaceLandmark.LEFT_EYE
            )?.position

            val rightEye = face.getLandmark(
                FaceLandmark.RIGHT_EYE
            )?.position

            val nose = face.getLandmark(
                FaceLandmark.NOSE_BASE
            )?.position

            val mouthLeft = face.getLandmark(
                FaceLandmark.MOUTH_LEFT
            )?.position

            val mouthRight = face.getLandmark(
                FaceLandmark.MOUTH_RIGHT
            )?.position


            Log.d(
                "FACE_TEST",
                """
                Landmark:
                Left Eye  = ${leftEye != null}
                Right Eye = ${rightEye != null}
                Nose      = ${nose != null}
                Mouth L   = ${mouthLeft != null}
                Mouth R   = ${mouthRight != null}
                """.trimIndent()
            )


            // =====================================
            // 2. CHECK FIVE LANDMARKS
            // =====================================

            if (
                leftEye == null ||
                rightEye == null ||
                nose == null ||
                mouthLeft == null ||
                mouthRight == null
            ) {

                updateFaceStatus(
                    "Landmark wajah belum lengkap. " +
                            "Hadapkan wajah ke kamera.",
                    Color.rgb(217, 155, 41)
                )

                return
            }


            // =====================================
            // 3. SORT LANDMARKS
            // =====================================

            val landmarks = listOf(
                leftEye,
                rightEye,
                nose,
                mouthLeft,
                mouthRight
            )

            // Tampilkan lima titik landmark pada preview.
            binding.faceLandmarkOverlay.updateLandmarks(
                landmarks,
                bitmap.width,
                bitmap.height
            )

            Log.d(
                "FACE_TEST",
                "Five facial landmarks detected"
            )


            if (!embeddingTestCompleted) {

                // =====================================
                // 5. FACE ALIGNMENT
                // =====================================

                val alignedFace = FaceAligner.align(
                    bitmap,
                    landmarks
                )

                // Tampilkan hasil alignment.
                showAlignedFacePreview(alignedFace)

                try {
                    Log.d(
                        "FACE_TEST",
                        "Alignment successful: " +
                                "${alignedFace.width}x${alignedFace.height}"
                    )

                    // =====================================
                    // 6. RUN FACE EMBEDDING
                    // =====================================

                    embeddingTestCompleted =
                        runFaceEmbeddingTest(alignedFace)

                } finally {
                    if (!alignedFace.isRecycled) {
                        alignedFace.recycle()
                    }
                }
            }

        } catch (e: Exception) {

            Log.e(
                "FACE_TEST",
                "Landmark atau alignment gagal",
                e
            )

            updateFaceStatus(
                "Gagal memproses wajah: ${e.message}",
                Color.RED
            )
        }
    }

    private fun showAlignedFacePreview(bitmap: Bitmap) {

        // Salin bitmap agar tidak ikut terhapus
        // ketika bitmap asli selesai diproses.
        val previewBitmap = bitmap.copy(
            Bitmap.Config.ARGB_8888,
            false
        )

        runOnUiThread {

            if (activityDestroyed || isFinishing || isDestroyed) {
                previewBitmap.recycle()
                return@runOnUiThread
            }

            val imageView = ImageView(this).apply {
                setImageBitmap(previewBitmap)

                scaleType = ImageView.ScaleType.FIT_CENTER

                layoutParams = ViewGroup.LayoutParams(
                    600,
                    600
                )

                adjustViewBounds = true

                setPadding(20, 20, 20, 20)
            }

            val dialog = AlertDialog.Builder(this)
                .setTitle("Hasil Face Alignment")
                .setMessage(
                    "Ukuran gambar: ${previewBitmap.width} x ${previewBitmap.height}"
                )
                .setView(imageView)
                .setPositiveButton("Tutup", null)
                .create()

            dialog.setOnDismissListener {
                imageView.setImageDrawable(null)

                if (!previewBitmap.isRecycled) {
                    previewBitmap.recycle()
                }
            }

            dialog.show()
        }
    }


    // =====================================
    // RUN MOBILEFACENET EMBEDDING TEST
    // =====================================

    private fun runFaceEmbeddingTest(
        alignedFace: Bitmap
    ): Boolean {

        return try {

            // =====================================
            // 1. CHECK ENGINE
            // =====================================

            val engine = faceEngine
                ?: throw IllegalStateException(
                    "MobileFaceNet belum diinisialisasi"
                )


            // =====================================
            // 2. EXTRACT EMBEDDING
            // =====================================

            val embedding = engine.extractEmbedding(
                alignedFace
            )


            // =====================================
            // 3. CHECK EMBEDDING DIMENSION
            // =====================================

            check(embedding.size == 128) {
                "Embedding harus memiliki 128 dimensi"
            }


            // =====================================
            // 4. CALCULATE L2 NORM
            // =====================================

            val norm = sqrt(
                embedding.sumOf {
                    it.toDouble() * it.toDouble()
                }
            )


            // =====================================
            // 5. SELF COMPARISON
            // =====================================

            val comparison = engine.compareFaces(
                firstFace = alignedFace,
                secondFace = alignedFace
            )


            // =====================================
            // 6. LOG RESULT
            // =====================================

            Log.d(
                "FACE_TEST",
                "================================"
            )

            Log.d(
                "FACE_TEST",
                "FACE ALIGNMENT: SUCCESS"
            )

            Log.d(
                "FACE_TEST",
                "Image: ${alignedFace.width}x${alignedFace.height}"
            )

            Log.d(
                "FACE_TEST",
                "Embedding dimension: ${embedding.size}"
            )

            Log.d(
                "FACE_TEST",
                "Embedding L2 norm: $norm"
            )

            Log.d(
                "FACE_TEST",
                "Self cosine similarity: ${comparison.cosineSimilarity}"
            )

            Log.d(
                "FACE_TEST",
                "Self angle: ${comparison.angleDegrees}"
            )

            Log.d(
                "FACE_TEST",
                "================================"
            )


            // =====================================
            // 7. DISPLAY RESULT
            // =====================================

            val resultText = String.format(
                Locale.US,
                "INFERENCE BERHASIL\n\n" +
                        "Face: 112 x 112\n" +
                        "Embedding: %d dimensi\n" +
                        "L2 Norm: %.4f\n" +
                        "Similarity: %.4f\n" +
                        "Angle: %.4f°",
                embedding.size,
                norm,
                comparison.cosineSimilarity,
                comparison.angleDegrees
            )

            updateFaceStatus(
                resultText,
                Color.rgb(49, 169, 104)
            )

            true

        } catch (e: Exception) {

            Log.e(
                "FACE_TEST",
                "MobileFaceNet inference gagal",
                e
            )

            updateFaceStatus(
                "Inference gagal: ${e.message}",
                Color.RED
            )

            false
        }
    }


    // =====================================
    // UPDATE FACE STATUS
    // =====================================

    private fun updateFaceStatus(
        message: String,
        color: Int
    ) {

        if (activityDestroyed) {
            return
        }

        runOnUiThread {

            if (!activityDestroyed && !isFinishing && !isDestroyed) {

                binding.tvFaceStatus.text = message

                binding.tvFaceStatus.setTextColor(color)
            }
        }
    }


    // =====================================
    // ON DESTROY
    // =====================================

    override fun onDestroy() {

        Log.d(
            "FACE_TEST",
            "FaceDetectionActivity onDestroy"
        )

        // Tandai Activity sudah dihentikan
        activityDestroyed = true


        // =====================================
        // 1. STOP IMAGE ANALYZER
        // =====================================

        imageAnalysis?.clearAnalyzer()

        imageAnalysis = null


        // =====================================
        // 2. STOP CAMERA
        // =====================================

        cameraProvider?.unbindAll()

        cameraProvider = null


        // =====================================
        // 3. CLEANUP EXECUTOR AND MODELS
        // =====================================

        if (::cameraExecutor.isInitialized) {

            try {

                // Cleanup dimasukkan ke antrean executor.
                // Proses analisis yang sedang berjalan akan
                // selesai terlebih dahulu.

                cameraExecutor.execute {

                    try {

                        if (::faceDetector.isInitialized) {
                            faceDetector.close()
                        }

                    } catch (e: Exception) {

                        Log.e(
                            "FACE_TEST",
                            "Gagal menutup Face Detector",
                            e
                        )
                    }

                    try {

                        faceEngine?.close()

                        faceEngine = null

                    } catch (e: Exception) {

                        Log.e(
                            "FACE_TEST",
                            "Gagal menutup MobileFaceNet",
                            e
                        )
                    }

                    Log.d(
                        "FACE_TEST",
                        "Cleanup model selesai"
                    )
                }

                // Tidak menerima pekerjaan baru.
                // Cleanup yang sudah masuk antrean tetap dijalankan.

                cameraExecutor.shutdown()

            } catch (e: Exception) {

                Log.e(
                    "FACE_TEST",
                    "Gagal menjadwalkan cleanup",
                    e
                )
            }

        } else {

            // Fallback jika executor belum pernah dibuat

            try {

                if (::faceDetector.isInitialized) {
                    faceDetector.close()
                }

                faceEngine?.close()

                faceEngine = null

            } catch (e: Exception) {

                Log.e(
                    "FACE_TEST",
                    "Gagal melakukan cleanup",
                    e
                )
            }
        }

        super.onDestroy()
    }
}