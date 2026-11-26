package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.service.RealtimeEventService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
@CrossOrigin(origins = "*")
public class RealtimeController {

    private final RealtimeEventService realtimeEventService;

    public RealtimeController(RealtimeEventService realtimeEventService) {
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return realtimeEventService.subscribe();
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActivity() {
        return ResponseEntity.ok(ApiResponse.ok("Recent real-time activity", realtimeEventService.getRecentActivity()));
    }

    @GetMapping("/telemetry")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTelemetry() {
        return ResponseEntity.ok(ApiResponse.ok("System telemetry", realtimeEventService.getTelemetry()));
    }
}
