package com.tisl.mpl;

import static com.tisl.mpl.MediaConstants.PATH_SEPARATOR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.tisl.mpl.service.S3StorageService;

import io.github.techgnious.IVCompressor;
import io.github.techgnious.dto.IVAudioAttributes;
import io.github.techgnious.dto.IVSize;
import io.github.techgnious.dto.IVVideoAttributes;
import io.github.techgnious.dto.ResizeResolution;
import io.github.techgnious.dto.VideoFormats;
import io.github.techgnious.exception.VideoException;

public class VideoCompressor implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(VideoCompressor.class);
    @Autowired
    private S3StorageService s3StorageService;

    MultipartFile uploadFile;
    Path targetLocation;
    String tmpStoragePath;
    boolean isS3StorageEnabled;

    public VideoCompressor(final MultipartFile pFile, final Path targetLocation, final String tmpStoragePath,
            final boolean isS3StorageEnabled) {
        this.uploadFile = pFile;
        this.targetLocation = targetLocation;
        this.tmpStoragePath = tmpStoragePath;
        this.isS3StorageEnabled = isS3StorageEnabled;
    }

    @Override
    public void run() {
        try {
            String result = new IVCompressor().reduceVideoSizeAndSaveToAPath(this.uploadFile.getBytes(),
                    this.uploadFile.getOriginalFilename(), VideoFormats.MP4, ResizeResolution.R480P,
                    this.tmpStoragePath);
            logger.info(result);
            String convertedFile = this.tmpStoragePath + PATH_SEPARATOR + this.uploadFile.getOriginalFilename();
            if(isS3StorageEnabled) {
                s3StorageService.putObject(
                        this.targetLocation.toUri().getRawPath() + this.uploadFile.getOriginalFilename(),
                        new File(convertedFile));
            } else {
                Files.copy(Paths.get(convertedFile), this.targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.delete(Paths.get(convertedFile));
        } catch(VideoException | IOException ie) {
            logger.error("Some exception occurred during video compression :: {}", ie);
        }
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

}
