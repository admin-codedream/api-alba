package com.api.alba.firebase;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("fcm")
public class FcmController {

    private final FcmService fcmService;

    @GetMapping("/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pushTest(
            @RequestParam(value = "token") String token
    ) {
        fcmService.sendMessage(FcmDto.builder()
                .pushSeq(1L)
                .title("테스트 푸쉬 발송")
                .content("테스트 푸쉬 내용")
                .pushToken(token)
                .pushLink("/setup")
                .project(ProjectId.ALBAM.getMessage()).build());
    }

}
