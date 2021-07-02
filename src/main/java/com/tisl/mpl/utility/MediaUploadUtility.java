package com.tisl.mpl.utility;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import io.github.techgnious.IVCompressor;
import io.github.techgnious.dto.IVAudioAttributes;
import io.github.techgnious.dto.IVSize;
import io.github.techgnious.dto.IVVideoAttributes;
import io.github.techgnious.dto.ResizeResolution;
import io.github.techgnious.dto.VideoFormats;
import io.github.techgnious.exception.VideoException;

public class MediaUploadUtility {
    private static final Logger logger = LoggerFactory.getLogger(MediaUploadUtility.class);

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

    private InputStream compressVideoSmall(MultipartFile pFile) {
        InputStream targetStream = null;
        try {
            targetStream = new ByteArrayInputStream(
                    new IVCompressor().reduceVideoSize(pFile.getBytes(), VideoFormats.MP4, ResizeResolution.R480P));
        } catch(VideoException | IOException ie) {
            logger.error("Some exception occurred during video compression :: {}", ie);
        }
        return targetStream;
    }

    public static void main(String s[]) throws IOException {
        Path path = Paths.get("C:\\Abinash\\RNR_code_changes\\WIN_20210702_22_09_29_Pro.mp4");
        String name = "WIN_20210702_22_09_29_Pro.mp4";
        String originalFileName = "WIN_20210702_22_09_29_Pro.mp4";
        String contentType = "video/mp4";
        byte[] content = null;
        try {
            content = Files.readAllBytes(path);
        } catch(final IOException e) {
        }
        MultipartFile result = new MockMultipartFile(name, originalFileName, contentType, content);
        Path targetLocation = Paths.get("C:\\Abinash\\RNR_code_changes\\WIN_20210702_22_09_29_Pro1.mp4");
        Files.copy(new MediaUploadUtility().compressVideoSmall(result), targetLocation,
                StandardCopyOption.REPLACE_EXISTING);
    }

}
