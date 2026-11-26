package com.salesanalytics.service;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RealtimeEventService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedDeque<Map<String, Object>> recentActivity = new ConcurrentLinkedDeque<>();
    private final CacheManager cacheManager;

    public RealtimeEventService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        publish("SYSTEM_READY", "Real-time SSE & Spring Cache telemetry online", "system");
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout; client reconnects automatically
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "status", "CONNECTED",
                            "activeClients", emitters.size(),
                            "timestamp", Instant.now().toString()
                    )));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    public void publish(String type, String message, String actor) {
        Map<String, Object> event = Map.of(
                "id", UUID.randomUUID().toString().substring(0, 8),
                "type", type,
                "message", message,
                "actor", actor == null ? "system" : actor,
                "timestamp", Instant.now().toString()
        );
        recentActivity.addFirst(event);
        while (recentActivity.size() > 50) {
            recentActivity.pollLast();
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("activity").data(event));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public List<Map<String, Object>> getRecentActivity() {
        return new ArrayList<>(recentActivity);
    }

    public Map<String, Object> getTelemetry() {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        return Map.of(
                "status", "UP",
                "activeSseClients", emitters.size(),
                "recentEventsCount", recentActivity.size(),
                "jvmUsedMemoryMb", usedMb,
                "jvmMaxMemoryMb", maxMb,
                "uptimeSeconds", uptimeSec,
                "cacheNames", cacheManager.getCacheNames(),
                "timestamp", Instant.now().toString()
        );
    }
}
