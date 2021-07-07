package com.tisl.mpl.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "file")
public class MediaStorageProperties {
    private String uploadDir;
    private int maxImageSize;
    private int maxVideoSize;
    private int imageFilesCount;
    private int videoFilesCount;
    private String commerceMediaValidationUri;
    private String commerceMediaSubmitUri;
    private String s3bucketName;
    private boolean s3StorageEnabled;
    private String tmpStoragePath;

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public int getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(int maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public int getMaxVideoSize() {
        return maxVideoSize;
    }

    public void setMaxVideoSize(int maxVideoSize) {
        this.maxVideoSize = maxVideoSize;
    }

    public int getImageFilesCount() {
        return imageFilesCount;
    }

    public void setImageFilesCount(int imageFilesCount) {
        this.imageFilesCount = imageFilesCount;
    }

    public int getVideoFilesCount() {
        return videoFilesCount;
    }

    public void setVideoFilesCount(int videoFilesCount) {
        this.videoFilesCount = videoFilesCount;
    }

    public String getCommerceMediaValidationUri() {
        return commerceMediaValidationUri;
    }

    public void setCommerceMediaValidationUri(String commerceMediaValidationUri) {
        this.commerceMediaValidationUri = commerceMediaValidationUri;
    }

    public boolean isS3StorageEnabled() {
        return s3StorageEnabled;
    }

    public void setS3StorageEnabled(boolean s3StorageEnabled) {
        this.s3StorageEnabled = s3StorageEnabled;
    }

    public String getCommerceMediaSubmitUri() {
        return commerceMediaSubmitUri;
    }

    public void setCommerceMediaSubmitUri(String commerceMediaSubmitUri) {
        this.commerceMediaSubmitUri = commerceMediaSubmitUri;
    }

    public String getS3bucketName() {
        return s3bucketName;
    }

    public void setS3bucketName(String s3bucketName) {
        this.s3bucketName = s3bucketName;
    }

    public String getTmpStoragePath() {
        return tmpStoragePath;
    }

    public void setTmpStoragePath(String tmpStoragePath) {
        this.tmpStoragePath = tmpStoragePath;
    }

}
