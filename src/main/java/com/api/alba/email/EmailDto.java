package com.api.alba.email;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailDto {

    private String recipient;
    private String emailTitle;
    private String emailContent;

}
