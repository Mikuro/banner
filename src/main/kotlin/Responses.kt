package com.promo

import kotlinx.serialization.Serializable


@Serializable
data class UploadResponse(
        val success: Boolean,
        val imageId: String? = null,
        val message: String? = null
)

@Serializable
data class LinkResponse(
        val success: Boolean,
        val imageUrl: String? = null,
        val message: String? = null
)

@Serializable
data class HtmlResponse(
        val success: Boolean,
        val html: String? = null,
        val message: String? = null
)