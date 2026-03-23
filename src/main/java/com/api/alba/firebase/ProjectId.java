package com.api.alba.firebase;

import lombok.Getter;

@Getter
public enum ProjectId {

    ALBAM("albam");

    private final String message;

    ProjectId(String message) {
        this.message = message;
    }
}