package com.promo

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.util.*

fun Application.configureRouting() {
    val minioService = MinioService()
    val logger = LoggerFactory.getLogger("PromoService")

    // Храним metadata загруженных изображений
    val imageMetadata = mutableMapOf<String, String>() // imageId -> contentType

    routing {
        get("/") {
            call.respondText("Промо-сервис работает! 🚀")
        }

        // Эндпоинт для загрузки картинки
        post("/upload") {
            try {
                val multipart = call.receiveMultipart()
                var imageBytes: ByteArray? = null
                var contentType = "image/jpeg"

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            if (part.name == "image") {
                                imageBytes = part.streamProvider().readBytes()
                                contentType = part.contentType?.toString() ?: "image/jpeg"
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                if (imageBytes == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        UploadResponse(false, message = "Изображение не найдено")
                    )
                    return@post
                }

                val imageId = minioService.uploadImage(imageBytes!!, contentType)
                imageMetadata[imageId] = contentType

                logger.info("Загружено изображение: $imageId")

                call.respond(
                    HttpStatusCode.OK,
                    UploadResponse(true, imageId = imageId, message = "Изображение успешно загружено")
                )

            } catch (e: Exception) {
                logger.error("Ошибка загрузки", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    UploadResponse(false, message = "Ошибка загрузки: ${e.message}")
                )
            }
        }

        // Эндпоинт для получения ссылки на картинку (вариант 1)
        get("/link/{imageId}") {
            try {
                val imageId = call.parameters["imageId"] ?: ""
                val contentType = imageMetadata[imageId]

                if (contentType == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        LinkResponse(false, message = "Изображение не найдено")
                    )
                    return@get
                }

                val imageUrl = minioService.getPresignedUrl(imageId, contentType)

                call.respond(
                    HttpStatusCode.OK,
                    LinkResponse(true, imageUrl = imageUrl)
                )

            } catch (e: Exception) {
                logger.error("Ошибка получения ссылки", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    LinkResponse(false, message = "Ошибка: ${e.message}")
                )
            }
        }

        // Эндпоинт для получения HTML с встроенной картинкой (вариант 2)
        get("/html-embed/{imageId}") {
            try {
                val imageId = call.parameters["imageId"] ?: ""
                val contentType = imageMetadata[imageId]

                if (contentType == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        HtmlResponse(false, message = "Изображение не найдено")
                    )
                    return@get
                }

                val imageBytes = minioService.getImageBytes(imageId, contentType)
                val base64Image = Base64.getEncoder().encodeToString(imageBytes)

                val htmlTemplate = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Промо баннер</title>
                        <style>
                            .promo-banner {
                                max-width: 100%;
                                height: auto;
                                border-radius: 8px;
                                box-shadow: 0 4px 8px rgba(0,0,0,0.1);
                            }
                        </style>
                    </head>
                    <body>
                        <img src="data:$contentType;base64,$base64Image" class="promo-banner" alt="Промо баннер"/>
                    </body>
                    </html>
                """.trimIndent()

                val encodedHtml = Base64.getEncoder().encodeToString(htmlTemplate.toByteArray())

                call.respond(
                    HttpStatusCode.OK,
                    HtmlResponse(true, html = encodedHtml)
                )

            } catch (e: Exception) {
                logger.error("Ошибка создания HTML с встроенной картинкой", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    HtmlResponse(false, message = "Ошибка: ${e.message}")
                )
            }
        }

        // Эндпоинт для получения HTML со ссылкой на картинку (вариант 3)
        get("/html-link/{imageId}") {
            try {
                val imageId = call.parameters["imageId"] ?: ""
                val contentType = imageMetadata[imageId]

                if (contentType == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        HtmlResponse(false, message = "Изображение не найдено")
                    )
                    return@get
                }

                val imageUrl = minioService.getPresignedUrl(imageId, contentType)

                val htmlTemplate = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Промо баннер</title>
                        <style>
                            .promo-banner {
                                max-width: 100%;
                                height: auto;
                                border-radius: 8px;
                                box-shadow: 0 4px 8px rgba(0,0,0,0.1);
                            }
                        </style>
                    </head>
                    <body>
                        <img src="$imageUrl" class="promo-banner" alt="Промо баннер"/>
                    </body>
                    </html>
                """.trimIndent()

                val encodedHtml = Base64.getEncoder().encodeToString(htmlTemplate.toByteArray())

                call.respond(
                    HttpStatusCode.OK,
                    HtmlResponse(true, html = encodedHtml)
                )

            } catch (e: Exception) {
                logger.error("Ошибка создания HTML со ссылкой", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    HtmlResponse(false, message = "Ошибка: ${e.message}")
                )
            }
        }

        // Дополнительный эндпоинт для просмотра HTML (для тестирования)
        get("/preview/{imageId}") {
            try {
                val imageId = call.parameters["imageId"] ?: ""
                val contentType = imageMetadata[imageId]

                if (contentType == null) {
                    call.respondText("Изображение не найдено", status = HttpStatusCode.NotFound)
                    return@get
                }

                val imageUrl = minioService.getPresignedUrl(imageId, contentType)

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Промо баннер - Превью</title>
                        <style>
                            body { font-family: Arial, sans-serif; margin: 40px; }
                            .promo-banner {
                                max-width: 100%;
                                height: auto;
                                border-radius: 8px;
                                box-shadow: 0 4px 8px rgba(0,0,0,0.1);
                                margin-bottom: 20px;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>Превью баннера</h1>
                        <img src="$imageUrl" class="promo-banner" alt="Промо баннер"/>
                        <p>ID изображения: $imageId</p>
                    </body>
                    </html>
                """.trimIndent()

                call.respondText(html, ContentType.Text.Html)

            } catch (e: Exception) {
                call.respondText("Ошибка: ${e.message}", status = HttpStatusCode.InternalServerError)
            }
        }
    }
}