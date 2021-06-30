package com.tisl.mpl.service;

import static com.tisl.mpl.MediaConstants.RATING_REVIEW;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.tisl.mpl.exception.MediaNotFoundException;
import com.tisl.mpl.exception.MediaStorageException;
import com.tisl.mpl.payload.CommerceMediaValidationResponse;
import com.tisl.mpl.property.MediaStorageProperties;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

@Service
public class MediaStorageService {
    private static final Logger logger = LoggerFactory.getLogger(MediaStorageService.class);
    private final Path fileStorageLocation;
    private MediaStorageProperties fileStorageProperties;
    private RestTemplate restTemplate;

    @Autowired
    public MediaStorageService(MediaStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        this.fileStorageProperties = fileStorageProperties;
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch(Exception ex) {
            throw new MediaStorageException("Could not create the directory where the uploaded files will be stored.",
                    ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if(fileName.contains("..")) {
                throw new MediaStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch(IOException ex) {
            throw new MediaStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()) {
                return resource;
            } else {
                throw new MediaNotFoundException("File not found " + fileName);
            }
        } catch(MalformedURLException ex) {
            throw new MediaNotFoundException("File not found " + fileName, ex);
        }
    }

    public boolean areFilesValidForUpload(final String pModuleName, final MultipartFile[] files,
            final String... pArgs) {
        if(RATING_REVIEW.equals(pModuleName)) {
            return runRatingReviewFileValidations(files, pArgs);
        } else {
            return true;
        }
    }

    private boolean runRatingReviewFileValidations(final MultipartFile[] files, final String... pArgs) {
        boolean allFilesValid = false;
        int imageFilesCount = 0;
        int videoFilesCount = 0;
        for(MultipartFile file : files) {
            int fileSize = (int)(file.getSize() / (1024 * 1024));
            if(file.getContentType().startsWith("image/") && fileSize <= fileStorageProperties.getMaxImageSize()) {
                allFilesValid = true;
                imageFilesCount++;
            } else if(file.getContentType().startsWith("video/")
                    && fileSize <= fileStorageProperties.getMaxVideoSize()) {
                allFilesValid = true;
                videoFilesCount++;
            } else {
                allFilesValid = false;
                if(logger.isDebugEnabled()) {
                    logger.debug("The file with fileName {}, file type {} and file size {} is not valid",
                            file.getName(), file.getContentType(), fileSize);
                }
                break;
            }
        }
        if(allFilesValid && imageFilesCount <= fileStorageProperties.getImageFilesCount()
                && videoFilesCount <= fileStorageProperties.getVideoFilesCount()) {
            return validateFilesAgainstCommerceDB(imageFilesCount, videoFilesCount, pArgs);
        }
        return allFilesValid;
    }

    private boolean validateFilesAgainstCommerceDB(final int pImageFilesCount, final int pVideoFilesCount,
            final String... pArgs) {
        boolean validFiles = false;
        try {
            String commerceMediaValidationUri = fileStorageProperties.getCommerceMediaValidationUri();
            String accessToken = pArgs[1];
            restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            final HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("productCode", pArgs[0]);
            queryParams.put("access_token", accessToken);
            ResponseEntity<CommerceMediaValidationResponse> responseEntity = restTemplate.exchange(
                    commerceMediaValidationUri, HttpMethod.GET, requestEntity, CommerceMediaValidationResponse.class,
                    queryParams);

            if(responseEntity.getStatusCode().is2xxSuccessful()) {
                CommerceMediaValidationResponse commerceMediaValidationResponse = responseEntity.getBody();
                if(commerceMediaValidationResponse.getImagesCount() + pImageFilesCount <= fileStorageProperties
                        .getImageFilesCount()
                        && commerceMediaValidationResponse.getVideosCount() + pVideoFilesCount <= fileStorageProperties
                                .getVideoFilesCount()) {
                    validFiles = true;
                }
            }
        } catch(final Exception e) {
            logger.error("validateFilesAgainstCommerceDB :: Error getting response from commerce {}", e);
        }
        return validFiles;
    }

    private void compressVideo() {
        try {
            FFmpeg ffmpeg = new FFmpeg("/path/to/ffmpeg");
            FFprobe ffprobe = new FFprobe("/path/to/ffprobe");
            FFmpegBuilder builder = new FFmpegBuilder().setInput("input.mp4") // Filename, or a
                                                                              // FFmpegProbeResult
                    .overrideOutputFiles(true) // Override the output if it exists
                    .addOutput("output.mp4") // Filename for the destination
                    .setFormat("mp4") // Format is inferred from filename, or can be set
                    .setTargetSize(250_000) // Aim for a 250KB file
                    .disableSubtitle() // No subtiles
                    .setAudioChannels(1) // Mono audio
                    .setAudioCodec("aac") // using the aac codec
                    .setAudioSampleRate(48_000) // at 48KHz
                    .setAudioBitRate(32768) // at 32 kbit/s
                    .setVideoCodec("libx264") // Video using x264
                    .setVideoFrameRate(24, 1) // at 24 frames per second
                    .setVideoResolution(640, 480) // at 640x480 resolution
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use
                                                                  // experimental specs
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            // Run a one-pass encode
            executor.createJob(builder).run();

            // Or run a two-pass encode (which is better quality at the cost of being slower)
            executor.createTwoPassJob(builder).run();
        } catch(IOException ie) {
            logger.error("Some exception occurred during video compression :: {}", ie);
        }
    }

}
