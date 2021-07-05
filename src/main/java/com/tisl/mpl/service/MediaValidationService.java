package com.tisl.mpl.service;

import static com.tisl.mpl.MediaConstants.RATING_REVIEW;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.tisl.mpl.exception.CommerceServiceException;
import com.tisl.mpl.exception.CommerceValidationException;
import com.tisl.mpl.exception.MediaValidationException;
import com.tisl.mpl.payload.MediaValidationResponse;
import com.tisl.mpl.property.MediaStorageProperties;

@Service
public class MediaValidationService {

    private static final Logger logger = LoggerFactory.getLogger(MediaValidationService.class);
    @Autowired
    private MediaStorageProperties fileStorageProperties;
    MediaValidationResponse mediaValidationResponse = null;

    public MediaValidationResponse getMediaValidationResponse(final String pModuleName, final MultipartFile[] files,
            final String productCode, final String accessToken) {
        if(RATING_REVIEW.equals(pModuleName)) {
            mediaValidationResponse = runRatingReviewFileValidations(files, productCode, accessToken);
        }
        return mediaValidationResponse;
    }

    private MediaValidationResponse runRatingReviewFileValidations(final MultipartFile[] files,
            final String productCode, final String accessToken) {
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
            mediaValidationResponse = validateFilesAgainstCommerceDB(imageFilesCount, videoFilesCount, productCode,
                    accessToken);
        } else {
            throw new MediaValidationException("Invalid Media File.");
        }
        return mediaValidationResponse;
    }

    private MediaValidationResponse validateFilesAgainstCommerceDB(final int pImageFilesCount,
            final int pVideoFilesCount, final String productCode, final String accessToken) {
        try {
            String commerceMediaValidationUri = fileStorageProperties.getCommerceMediaValidationUri();
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            final HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            Map<String, Object> queryParams = new HashMap<>();
            queryParams.put("productCode", productCode);
            queryParams.put("access_token", accessToken);
            ResponseEntity<MediaValidationResponse> responseEntity = restTemplate.exchange(commerceMediaValidationUri,
                    HttpMethod.GET, requestEntity, MediaValidationResponse.class, queryParams);

            if(responseEntity.getStatusCode().is2xxSuccessful()) {
                MediaValidationResponse commerceMediaValidationResponse = responseEntity.getBody();
                if(commerceMediaValidationResponse.getImagesCount() + pImageFilesCount <= fileStorageProperties
                        .getImageFilesCount()
                        && commerceMediaValidationResponse.getVideosCount() + pVideoFilesCount <= fileStorageProperties
                                .getVideoFilesCount()) {
                    mediaValidationResponse = commerceMediaValidationResponse;
                } else {
                    throw new CommerceValidationException("Media validation Failed");
                }
            } else {
                throw new CommerceServiceException("Commerce Service Exception");
            }
        } catch(final CommerceValidationException cve) {
            logger.error("validateFilesAgainstCommerceDB :: Media validation Failed {}", cve);
            throw cve;
        } catch(final Exception e) {
            logger.error("validateFilesAgainstCommerceDB :: Error getting response from commerce {}", e);
            throw new CommerceServiceException("Commerce Service Exception");
        }
        return mediaValidationResponse;
    }

}
