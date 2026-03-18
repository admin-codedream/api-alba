package com.api.alba.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardTodayResponse {
    private int checkedInCount;
    private int workingCount;
}
