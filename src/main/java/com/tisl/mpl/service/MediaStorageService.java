package com.tisl.mpl.service;

import static com.tisl.mpl.MediaConstants.UPLOAD_STATUS_FAILURE;
import static com.tisl.mpl.MediaConstants.UPLOAD_STATUS_SUCCESS;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tisl.mpl.exception.MediaNotFoundException;
import com.tisl.mpl.exception.MediaStorageException;
import com.tisl.mpl.payload.ReviewMediaSubmitRequest;
import com.tisl.mpl.payload.UploadMediaResponse;
import com.tisl.mpl.property.MediaStorageProperties;
import com.tisl.mpl.utility.MediaUploadUtility;

@Service
public class MediaStorageService {
    private static final Logger logger = LoggerFactory.getLogger(MediaStorageService.class);
    private final Path fileStorageLocation;
    @Autowired
    private MediaStorageProperties fileStorageProperties;
    @Autowired
    private MediaUploadUtility mediaUploadUtility;

    @Autowired
    public MediaStorageService(final MediaStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch(Exception ex) {
            throw new MediaStorageException("Could not create the directory where the uploaded files will be stored.",
                    ex);
        }
    }

    public List<UploadMediaResponse> storeFiles(final MultipartFile[] pFiles, final String pProductCode) {
        List<UploadMediaResponse> uploadMediaResponseList = new ArrayList<>();
        for(MultipartFile file : pFiles) {
            // Normalize file name
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            String fileContentType = null;
            if(file.getContentType().startsWith("image/")) {
                fileContentType = "Image";
            } else {
                fileContentType = "Video";
            }
            UploadMediaResponse uploadMediaResponse = new UploadMediaResponse(fileName, fileContentType, file.getSize(),
                    pProductCode);
            try {
                // Check if the file's name contains invalid characters
                if(fileName.contains("..")) {
                    throw new MediaStorageException("Sorry! Filename contains invalid path sequence " + fileName);
                }
                String fileDownloadUri = null;
                if(fileStorageProperties.isS3StorageEnabled()) {
                    logger.info("S3 storage enabled");
                } else {
                    Path targetLocation = this.fileStorageLocation.resolve(fileName);
                    fileDownloadUri = saveFileInLocalDisk(file, fileName, targetLocation);
                    mediaUploadUtility.compressVideoAndSave(file, targetLocation, false);
                }
                logger.info("File uploaded successfully");
                uploadMediaResponse.setMediaUrl(fileDownloadUri);
                uploadMediaResponse.setUploadStatus(UPLOAD_STATUS_SUCCESS);
                uploadMediaResponseList.add(uploadMediaResponse);
            } catch(IOException ex) {
                logger.error("Could not store file " + fileName + ". Please try again!.. Exception is:: {}", ex);
                uploadMediaResponse.setUploadStatus(UPLOAD_STATUS_FAILURE);
                uploadMediaResponseList.add(uploadMediaResponse);
            }
        }
        return uploadMediaResponseList;
    }

    private String saveFileInLocalDisk(final MultipartFile file, final String fileName, final Path targetLocation)
            throws IOException {
        // Copy file to the target location (Replacing existing file with the same name)
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/downloadFile/").path(fileName).toUriString();
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

    public List<UploadMediaResponse> storeFilesAndUpdateCommerce(final MultipartFile[] pFiles, final String productCode,
            final String accessToken) {
        List<UploadMediaResponse> uploadMediaResponseList = storeFiles(pFiles, productCode);
        updateMediaUploadInfoInCommerce(uploadMediaResponseList, accessToken);
        return uploadMediaResponseList;
    }

    private void updateMediaUploadInfoInCommerce(final List<UploadMediaResponse> pUploadMediaResponseList,
            final String accessToken) {
        List<UploadMediaResponse> successList = pUploadMediaResponseList.stream()
                .filter(x -> UPLOAD_STATUS_SUCCESS.equals(x.getUploadStatus())).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(successList)) {
            try {
                String commerceMediaSubmitUri = fileStorageProperties.getCommerceMediaSubmitUri();
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Type", "application/json");
                ReviewMediaSubmitRequest reviewMediaSubmitRequest = new ReviewMediaSubmitRequest();
                reviewMediaSubmitRequest.setReviewMediaItems(successList);
                final HttpEntity<String> requestEntity = new HttpEntity<>(
                        new ObjectMapper().writeValueAsString(reviewMediaSubmitRequest), headers);
                Map<String, Object> queryParams = new HashMap<>();
                queryParams.put("access_token", accessToken);
                ResponseEntity<String> responseEntity = restTemplate.exchange(commerceMediaSubmitUri, HttpMethod.POST,
                        requestEntity, String.class, queryParams);

                if(responseEntity.getStatusCode().is2xxSuccessful()) {
                    logger.info("Media details updated in commerce");
                }
            } catch(final Exception e) {
                logger.error("validateFilesAgainstCommerceDB :: Error getting response from commerce {}", e);
            }
        }
    }

}
