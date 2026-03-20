package com.api.alba.dto.notice;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class NoticeUpdateRequest {
    @NotBlank(message = "title is required.")
    @Size(max = 200, message = "title must be 200 characters or fewer.")
    private String title;

    @NotBlank(message = "content is required.")
    @Size(max = 5000, message = "content must be 5000 characters or fewer.")
    private String content;

    private Boolean pinned;
}
