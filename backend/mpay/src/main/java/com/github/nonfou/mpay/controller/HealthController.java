package com.github.nonfou.mpay.controller;

import com.github.nonfou.mpay.common.response.ApiResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/_internal")
public class HealthController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("service", "mpay-gateway");
        payload.put("timestamp", Instant.now().toString());
        return ApiResponse.success(payload);
    }
}
