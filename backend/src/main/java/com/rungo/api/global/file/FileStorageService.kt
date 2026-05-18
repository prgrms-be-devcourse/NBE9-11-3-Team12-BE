package com.rungo.api.global.file

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Service
class FileStorageService {
    @Value("\${file.upload-dir}")
    private lateinit var uploadDir: String

    fun saveMarathonPoster(file: MultipartFile?): String? {
        if (file == null || file.isEmpty) return null

        validateImage(file)

        try {
            val originalFilename = StringUtils.cleanPath(file.originalFilename ?: "")
            val extension = extractExtension(originalFilename)
            val storedFileName = buildStoredFileName(extension)
            val marathonDir = buildMarathonDirectoryPath()
            Files.createDirectories(marathonDir)
            val targetPath = marathonDir.resolve(storedFileName)
            file.transferTo(targetPath)
            return buildFileUrl(storedFileName)
        } catch (e: IOException) {
            throw IllegalStateException("포스터 이미지 저장에 실패했습니다.", e)
        }
    }

    private fun validateImage(file: MultipartFile) {
        require(file.contentType in ALLOWED_IMAGE_CONTENT_TYPES) {
            "이미지 파일만 업로드할 수 있습니다."
        }
    }

    private fun extractExtension(filename: String): String =
        filename.substringAfterLast('.', "").lowercase()

    private fun buildStoredFileName(extension: String): String =
        UUID.randomUUID().toString() + if (extension.isBlank()) "" else ".$extension"

    private fun buildMarathonDirectoryPath(): Path = Paths.get(uploadDir, MARATHONS_DIRECTORY)

    private fun buildFileUrl(storedFileName: String): String = "$UPLOAD_URL_PREFIX/$MARATHONS_DIRECTORY/$storedFileName"

    companion object {
        private val ALLOWED_IMAGE_CONTENT_TYPES = setOf(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
        )
        private const val UPLOAD_URL_PREFIX = "/uploads"
        private const val MARATHONS_DIRECTORY = "marathons"
    }
}