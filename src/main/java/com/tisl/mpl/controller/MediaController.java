package com.tisl.mpl.controller;

import static com.tisl.mpl.MediaConstants.RATING_REVIEW;
import static com.tisl.mpl.MediaConstants.UPLOAD_STATUS_FAILURE;
import static com.tisl.mpl.MediaConstants.UPLOAD_STATUS_SUCCESS;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tisl.mpl.payload.UploadMediaResponse;
import com.tisl.mpl.service.MediaStorageService;

@RestController
public class MediaController {

    private static final Logger logger = LoggerFactory.getLogger(MediaController.class);

    @Autowired
    private MediaStorageService fileStorageService;

    @PostMapping("/ratingreview/{productCode}/uploadMedia")
    public List<UploadMediaResponse> uploadMedia(@PathVariable final String productCode,
            @RequestHeader(required = true, value = "access-token") final String accessToken,
            @RequestParam(required = true, value = "files") final MultipartFile[] files) {
        return uploadMultipleFiles(RATING_REVIEW, files, productCode, accessToken);
    }

    private List<UploadMediaResponse> uploadMultipleFiles(final String pModuleName, final MultipartFile[] pFiles,
            final String... pArgs) {
        List<UploadMediaResponse> listUploadMediaResponse = null;
        if(fileStorageService.areFilesValidForUpload(pModuleName, pFiles, pArgs)) {
            listUploadMediaResponse = Arrays.asList(pFiles).stream().map(this::uploadFile).collect(Collectors.toList());
        }
        return listUploadMediaResponse;
    }

    private UploadMediaResponse uploadFile(final MultipartFile file) {
        String fileName = null;
        String fileDownloadUri = null;
        UploadMediaResponse uploadMediaResponse = null;
        try {
            fileName = fileStorageService.storeFile(file);
            fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path("/downloadFile/").path(fileName)
                    .toUriString();
            uploadMediaResponse = new UploadMediaResponse(fileName, fileDownloadUri, file.getContentType(),
                    file.getSize(), UPLOAD_STATUS_SUCCESS);
        } catch(final Exception e) {
            logger.error("Error while uploading file {} {}", fileName, e);
            uploadMediaResponse = new UploadMediaResponse(fileName, fileDownloadUri, file.getContentType(),
                    file.getSize(), UPLOAD_STATUS_FAILURE);
        }

        return uploadMediaResponse;
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch(IOException ex) {
            logger.info("Could not determine file type.");
        }

        // Fallback to the default content type if type could not be determined
        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

}
