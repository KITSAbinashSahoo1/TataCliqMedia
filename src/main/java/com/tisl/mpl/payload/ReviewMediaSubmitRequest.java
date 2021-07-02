package com.tisl.mpl.payload;

import java.util.List;

public class ReviewMediaSubmitRequest {
    private List<UploadMediaResponse> reviewMediaItems;

    public List<UploadMediaResponse> getReviewMediaItems() {
        return reviewMediaItems;
    }

    public void setReviewMediaItems(List<UploadMediaResponse> reviewMediaItems) {
        this.reviewMediaItems = reviewMediaItems;
    }
}
