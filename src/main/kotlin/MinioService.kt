package com.promo

import io.minio.*
import io.minio.http.Method
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.TimeUnit

class MinioService {
    private val minioClient: MinioClient
    private val bucketName = "promo-images"
    private val internalEndpoint: String
    private val externalEndpoint: String

    init {
        internalEndpoint = System.getenv("MINIO_ENDPOINT") ?: "http://localhost:9000"
        externalEndpoint = System.getenv("MINIO_EXTERNAL_ENDPOINT") ?: "http://localhost:9000"

        minioClient = MinioClient.builder()
            .endpoint(internalEndpoint)
            .credentials(
                System.getenv("MINIO_ACCESS_KEY") ?: "minioadmin",
                System.getenv("MINIO_SECRET_KEY") ?: "minioadmin"
            )
            .build()

        // Инициализируем bucket с retry логикой
        initializeBucket()
    }

    private fun initializeBucket() {
        var attempts = 0
        val maxAttempts = 30 // 30 попыток по 2 секунды = 1 минута

        while (attempts < maxAttempts) {
            try {
                // Проверяем доступность MinIO
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
                    println("✅ MinIO bucket '$bucketName' создан успешно")
                } else {
                    println("✅ MinIO bucket '$bucketName' уже существует")
                }
                return // Успешно подключились
            } catch (e: Exception) {
                attempts++
                println("⏳ Попытка подключения к MinIO $attempts/$maxAttempts: ${e.message}")
                if (attempts >= maxAttempts) {
                    throw RuntimeException("❌ Не удалось подключиться к MinIO после $maxAttempts попыток", e)
                }
                // Ждем 2 секунды перед следующей попыткой
                Thread.sleep(2000)
            }
        }
    }

    fun uploadImage(imageBytes: ByteArray, contentType: String): String {
        val imageId = UUID.randomUUID().toString()
        val fileName = "$imageId.${getFileExtension(contentType)}"

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(fileName)
                .stream(ByteArrayInputStream(imageBytes), imageBytes.size.toLong(), -1)
                .contentType(contentType)
                .build()
        )

        return imageId
    }

    fun getPresignedUrl(imageId: String, contentType: String): String {
        val fileName = "$imageId.${getFileExtension(contentType)}"
        val presignedUrl = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .`object`(fileName)
                .expiry(24, TimeUnit.HOURS) // URL действителен 24 часа
                .build()
        )

        // ВАЖНО! Заменяем внутренний endpoint на внешний для клиентов
        return presignedUrl.replace(internalEndpoint, externalEndpoint)
    }

    fun getImageBytes(imageId: String, contentType: String): ByteArray {
        val fileName = "$imageId.${getFileExtension(contentType)}"
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(fileName)
                .build()
        ).readAllBytes()
    }

    private fun getFileExtension(contentType: String): String {
        return when (contentType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
}