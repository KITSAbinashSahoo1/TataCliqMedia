package com.tisl.mpl.service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.tisl.mpl.exception.MediaStorageException;
import com.tisl.mpl.property.MediaStorageProperties;

@Service
public class S3StorageService {
    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);
    String s3BucketName;
    AmazonS3 s3Client = null;

    @Autowired
    public S3StorageService(final MediaStorageProperties fileStorageProperties) {
        this.s3BucketName = fileStorageProperties.getS3bucketName();
        // AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        // s3Client.setRegion(Region.getRegion(Regions.AP_SOUTH_1));
        // createBucket(this.s3BucketName);
    }

    public void putObject(final String pFileNameWithFolderPath, final File pFile) {
        try {
            s3Client.putObject(this.s3BucketName, pFileNameWithFolderPath, pFile);
        } catch(final Exception e) {
            logger.error("Error occurred while saving to S3 :: {}", e);
            throw new MediaStorageException("Could not save to cloud");
        }
    }

    public void createBucket(final String bucketName) {
        try {
            if(s3Client.doesBucketExistV2(bucketName)) {
                if(logger.isDebugEnabled()) {
                    logger.debug("Bucket {} already exists", bucketName);
                }
            } else {
                logger.info("Bucket {} does not exist. Going to create it.", bucketName);
                s3Client.createBucket(bucketName);
            }
        } catch(AmazonS3Exception e) {
            logger.error("createBucket :: S3 access exception {}", e);
            throw new MediaStorageException("Could not save to cloud");
        }
    }

}
