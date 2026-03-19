package com.api.alba.controller;

import com.api.alba.dto.NaverGeocodeResponse;
import com.api.alba.service.NaverMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/naver")
@RequiredArgsConstructor
public class NaverMapController {
    private final NaverMapService naverMapService;

    // 주소 문자열을 네이버 지오코딩 API로 변환합니다.
    @GetMapping("/geocode")
    public NaverGeocodeResponse geocode(@RequestParam String address) {
        return naverMapService.geocode(address);
    }
}
