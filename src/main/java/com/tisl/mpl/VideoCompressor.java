package com.tisl.mpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import io.github.techgnious.IVCompressor;
import io.github.techgnious.dto.IVAudioAttributes;
import io.github.techgnious.dto.IVSize;
import io.github.techgnious.dto.IVVideoAttributes;
import io.github.techgnious.dto.ResizeResolution;
import io.github.techgnious.dto.VideoFormats;
import io.github.techgnious.exception.VideoException;

public class VideoCompressor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(VideoCompressor.class);
    MultipartFile uploadFile;
    Path targetLocation;
    boolean isS3StorageEnabled;

    public VideoCompressor(Path targetLocation, final MultipartFile pFile, boolean isS3StorageEnabled) {
        this.uploadFile = pFile;
        this.targetLocation = targetLocation;
        this.isS3StorageEnabled = isS3StorageEnabled;
    }

    private InputStream compressVideoSmall(MultipartFile pFile) {
        InputStream targetStream = null;
        try {
            targetStream = new ByteArrayInputStream(
                    new IVCompressor().reduceVideoSize(pFile.getBytes(), VideoFormats.MP4, ResizeResolution.R480P));
            logger.info("File {} got converted successfully", pFile.getOriginalFilename());
        } catch(VideoException | IOException ie) {
            logger.error("Some exception occurred during video compression :: {}", ie);
        }
        return targetStream;
    }

    private InputStream compressVideo(MultipartFile pFile) {
        InputStream targetStream = null;
        try {
            IVSize customRes = new IVSize();
            customRes.setWidth(400);
            customRes.setHeight(300);
            IVAudioAttributes audioAttribute = new IVAudioAttributes();
            // here 64kbit/s is 64000
            audioAttribute.setBitRate(64000);
            audioAttribute.setChannels(2);
            audioAttribute.setSamplingRate(44100);
            IVVideoAttributes videoAttribute = new IVVideoAttributes();
            // Here 160 kbps video is 160000
            videoAttribute.setBitRate(160000);
            // More the frames more quality and size, but keep it low based on //devices like mobile
            videoAttribute.setFrameRate(15);
            videoAttribute.setSize(customRes);
            targetStream = new ByteArrayInputStream(new IVCompressor().encodeVideoWithAttributes(pFile.getBytes(),
                    VideoFormats.MP4, audioAttribute, videoAttribute));
        } catch(VideoException | IOException ie) {
            logger.error("Some exception occurred during video compression :: {}", ie);
        }
        return targetStream;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = compressVideoSmall(this.uploadFile);
            if(isS3StorageEnabled) {

            } else {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch(Exception e) {
            logger.error("Exception while compression :: {}", e);
        }
    }

}
