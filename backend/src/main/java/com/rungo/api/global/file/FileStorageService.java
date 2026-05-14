package com.rungo.api.global.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service

public class FileStorageService {

    @Value("${file.upload-dir}")

    private String uploadDir;

    public String saveMarathonPoster(MultipartFile file) {

        if (file == null || file.isEmpty()) {

            return null;

        }

        validateImage(file);

        try {

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

            String extension = extractExtension(originalFilename);

            String storedFileName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);

            Path marathonDir = Paths.get(uploadDir, "marathons");

            Files.createDirectories(marathonDir);

            Path targetPath = marathonDir.resolve(storedFileName);

            file.transferTo(targetPath);

            return "/uploads/marathons/" + storedFileName;

        } catch (IOException e) {

            throw new IllegalStateException("포스터 이미지 저장에 실패했습니다.", e);

        }

    }

    private void validateImage(MultipartFile file) {

        String contentType = file.getContentType();

        if (contentType == null ||

                (!contentType.equals("image/png")

                        && !contentType.equals("image/jpeg")

                        && !contentType.equals("image/jpg")

                        && !contentType.equals("image/webp"))) {

            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");

        }

    }

    private String extractExtension(String filename) {

        int lastDotIndex = filename.lastIndexOf(".");

        if (lastDotIndex == -1) {

            return "";

        }

        return filename.substring(lastDotIndex + 1).toLowerCase();

    }

}