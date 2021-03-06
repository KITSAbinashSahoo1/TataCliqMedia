package com.tisl.mpl.utility;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.tisl.mpl.VideoCompressor;
import com.tisl.mpl.property.MediaStorageProperties;

@Component
public class MediaUploadUtility {
    private static final Logger logger = LoggerFactory.getLogger(MediaUploadUtility.class);

    @Autowired
    private MediaStorageProperties fileStorageProperties;

    public void compressVideoAndSave(final MultipartFile pFile, Path targetLocation, boolean isS3StorageEnabled) {
        logger.info("Starting video compression...");
        try {
            VideoCompressor job = new VideoCompressor(pFile, targetLocation, fileStorageProperties.getTmpStoragePath(),
                    isS3StorageEnabled);
            CompletableFuture.runAsync(job);
        } catch(Exception e) {
            logger.error("Exception while compression :: {}", e);
        }
    }

}
