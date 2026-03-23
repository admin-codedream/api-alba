package com.api.alba.firebase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FcmDto {

    private Long pushSeq;
    private String pushToken;
    private String title;
    private String content;
    private String pushLink;
    private String project;

}
