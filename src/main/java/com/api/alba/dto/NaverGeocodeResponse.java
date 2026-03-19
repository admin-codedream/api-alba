package com.api.alba.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NaverGeocodeResponse {
    private String status;
    private Meta meta;
    private List<Address> addresses;
    private String errorMessage;

    @Getter
    @Setter
    public static class Meta {
        private Integer totalCount;
        private Integer count;
        private Integer page;
    }

    @Getter
    @Setter
    public static class Address {
        private String roadAddress;
        private String jibunAddress;
        private String englishAddress;
        private String x;
        private String y;
        private String distance;
    }
}
