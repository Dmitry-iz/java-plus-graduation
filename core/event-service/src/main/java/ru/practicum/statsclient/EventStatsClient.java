//package ru.practicum.statsclient;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
//import org.springframework.http.*;
//import org.springframework.retry.support.RetryTemplate;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//import ru.practicum.dto.EndpointHitDTO;
//import ru.practicum.dto.ViewStatsDTO;
//
//import java.net.URI;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//
//import static ru.practicum.constants.Constants.STATS_EVENTS_URL;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class EventStatsClient {
//
//    private static final String STATS_SERVICE_ID = "stats-server";
//    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//    private final DiscoveryClient discoveryClient;
//    private final RetryTemplate retryTemplate;
//    private final @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate;
//
//    public void saveHit(EndpointHitDTO endpointHitDTO) {
//        try {
//            URI uri = buildUri("/hit");
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<EndpointHitDTO> requestEntity = new HttpEntity<>(endpointHitDTO, headers);
//
//            log.debug("Sending hit to stats server: {}", endpointHitDTO);
//            ResponseEntity<Void> response = restTemplate.exchange(
//                    uri, HttpMethod.POST, requestEntity, Void.class);
//
//            log.debug("Hit successfully sent to stats server. Status: {}", response.getStatusCode());
//
//        } catch (Exception e) {
//            log.error("Failed to save hit to stats server: {}", e.getMessage());
//        }
//    }
//
//    public void saveHits(List<EndpointHitDTO> hits) {
//        if (hits == null || hits.isEmpty()) {
//            return;
//        }
//
//        try {
//            URI uri = buildUri("/hit/batch");
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<List<EndpointHitDTO>> requestEntity = new HttpEntity<>(hits, headers);
//
//            log.debug("Sending batch of {} hits to stats server", hits.size());
//            restTemplate.exchange(uri, HttpMethod.POST, requestEntity, Void.class);
//            log.debug("Batch hits successfully sent to stats server");
//
//        } catch (Exception e) {
//            log.warn("Batch save failed, falling back to single saves: {}", e.getMessage());
//
//            for (EndpointHitDTO hit : hits) {
//                try {
//                    saveHit(hit);
//                } catch (Exception ex) {
//                    log.error("Failed to save hit in fallback mode: {}", ex.getMessage());
//                }
//            }
//        }
//    }
//
//    public Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
//        if (eventIds.isEmpty()) {
//            return Collections.emptyMap();
//        }
//
//        try {
//            List<String> uris = eventIds.stream()
//                    .map(id -> STATS_EVENTS_URL + id)
//                    .toList();
//
//            LocalDateTime start = LocalDateTime.now().minusYears(10);
//            LocalDateTime end = LocalDateTime.now().plusYears(10);
//
//            URI uri = buildStatsUri(start, end, uris, true);
//
//            log.debug("Getting stats for {} events from stats server", eventIds.size());
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
//
//            ResponseEntity<ViewStatsDTO[]> response = restTemplate.exchange(
//                    uri, HttpMethod.GET, requestEntity, ViewStatsDTO[].class);
//
//            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                Map<Long, Long> viewsMap = new HashMap<>();
//                for (ViewStatsDTO stat : response.getBody()) {
//                    Long eventId = extractEventIdFromUri(stat.getUri());
//                    viewsMap.put(eventId, stat.getHits());
//                }
//                log.debug("Received stats for {} events", viewsMap.size());
//                return viewsMap;
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to get stats from stats server: {}", e.getMessage());
//        }
//
//        return Collections.emptyMap();
//    }
//
//    public Long getViewsForEvent(Long eventId) {
//        Map<Long, Long> views = getViewsForEvents(List.of(eventId));
//        return views.getOrDefault(eventId, 0L);
//    }
//
//    private URI buildStatsUri(LocalDateTime start, LocalDateTime end,
//                              List<String> uris, Boolean unique) {
//        return retryTemplate.execute(context -> {
//            ServiceInstance instance = getStatsServiceInstance();
//
//            UriComponentsBuilder uriBuilder = UriComponentsBuilder
//                    .fromHttpUrl("http://" + instance.getHost() + ":" + instance.getPort())
//                    .path("/stats")
//                    .queryParam("start", FORMATTER.format(start))
//                    .queryParam("end", FORMATTER.format(end))
//                    .queryParam("unique", unique);
//
//            if (uris != null && !uris.isEmpty()) {
//                for (String uri : uris) {
//                    uriBuilder.queryParam("uris", uri);
//                }
//            }
//
//            return uriBuilder.build().toUri();
//        });
//    }
//
//    private URI buildUri(String path) {
//        return retryTemplate.execute(context -> {
//            ServiceInstance instance = getStatsServiceInstance();
//            return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
//        });
//    }
//
//    private ServiceInstance getStatsServiceInstance() {
//        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);
//
//        if (instances == null || instances.isEmpty()) {
//            String errorMsg = "Stats service (" + STATS_SERVICE_ID + ") not found in Eureka registry";
//            log.error(errorMsg);
//            throw new IllegalStateException(errorMsg);
//        }
//
//        ServiceInstance instance = instances.get(0);
//        log.debug("Found stats service instance: {}:{}", instance.getHost(), instance.getPort());
//
//        return instance;
//    }
//
//    private Long extractEventIdFromUri(String uri) {
//        try {
//            return Long.parseLong(uri.substring(STATS_EVENTS_URL.length()));
//        } catch (NumberFormatException e) {
//            log.warn("Failed to extract eventId from uri: {}", uri);
//            return -1L;
//        }
//    }
//}

package ru.practicum.statsclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDTO;
import ru.practicum.dto.ViewStatsDTO;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ru.practicum.constants.Constants.STATS_EVENTS_URL;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatsClient {

    private static final String STATS_SERVICE_ID = "stats-server"; // ← ИСПРАВЛЕНО: lowercase
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate;

    public void saveHit(EndpointHitDTO endpointHitDTO) {
        try {
            URI uri = buildUri("/hit");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EndpointHitDTO> requestEntity = new HttpEntity<>(endpointHitDTO, headers);

            log.debug("Sending hit to stats server: {}", endpointHitDTO);
            ResponseEntity<Void> response = restTemplate.exchange(
                    uri, HttpMethod.POST, requestEntity, Void.class);

            log.debug("Hit successfully sent to stats server. Status: {}", response.getStatusCode());

        } catch (Exception e) {
            log.error("Failed to save hit to stats server: {}", e.getMessage());
        }
    }

    public void saveHits(List<EndpointHitDTO> hits) {
        if (hits == null || hits.isEmpty()) {
            return;
        }

        try {
            URI uri = buildUri("/hit/batch");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<EndpointHitDTO>> requestEntity = new HttpEntity<>(hits, headers);

            log.debug("Sending batch of {} hits to stats server", hits.size());
            restTemplate.exchange(uri, HttpMethod.POST, requestEntity, Void.class);
            log.debug("Batch hits successfully sent to stats server");

        } catch (Exception e) {
            log.warn("Batch save failed, falling back to single saves: {}", e.getMessage());

            for (EndpointHitDTO hit : hits) {
                try {
                    saveHit(hit);
                } catch (Exception ex) {
                    log.error("Failed to save hit in fallback mode: {}", ex.getMessage());
                }
            }
        }
    }

    public Map<Long, Long> getViewsForEvents(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            List<String> uris = eventIds.stream()
                    .map(id -> STATS_EVENTS_URL + id)
                    .toList();

            LocalDateTime start = LocalDateTime.now().minusYears(10);
            LocalDateTime end = LocalDateTime.now().plusYears(10);

            URI uri = buildStatsUri(start, end, uris, true);

            log.debug("Getting stats for {} events from stats server", eventIds.size());

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<ViewStatsDTO[]> response = restTemplate.exchange(
                    uri, HttpMethod.GET, requestEntity, ViewStatsDTO[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<Long, Long> viewsMap = new HashMap<>();
                for (ViewStatsDTO stat : response.getBody()) {
                    Long eventId = extractEventIdFromUri(stat.getUri());
                    viewsMap.put(eventId, stat.getHits());
                }
                log.debug("Received stats for {} events", viewsMap.size());
                return viewsMap;
            }

        } catch (Exception e) {
            log.error("Failed to get stats from stats server: {}", e.getMessage());
        }

        return Collections.emptyMap();
    }

    public Long getViewsForEvent(Long eventId) {
        Map<Long, Long> views = getViewsForEvents(List.of(eventId));
        return views.getOrDefault(eventId, 0L);
    }

    private URI buildStatsUri(LocalDateTime start, LocalDateTime end,
                              List<String> uris, Boolean unique) {
        return retryTemplate.execute(context -> {
            ServiceInstance instance = getStatsServiceInstance();

            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                    .fromHttpUrl("http://" + instance.getHost() + ":" + instance.getPort())
                    .path("/stats")
                    .queryParam("start", FORMATTER.format(start))
                    .queryParam("end", FORMATTER.format(end))
                    .queryParam("unique", unique);

            if (uris != null && !uris.isEmpty()) {
                for (String uri : uris) {
                    uriBuilder.queryParam("uris", uri);
                }
            }

            return uriBuilder.build().toUri();
        });
    }

    private URI buildUri(String path) {
        return retryTemplate.execute(context -> {
            ServiceInstance instance = getStatsServiceInstance();
            return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
        });
    }

    private ServiceInstance getStatsServiceInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);

        if (instances == null || instances.isEmpty()) {
            String errorMsg = "Stats service (" + STATS_SERVICE_ID + ") not found in Eureka registry";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        ServiceInstance instance = instances.get(0);
        log.debug("Found stats service instance: {}:{}", instance.getHost(), instance.getPort());

        return instance;
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.substring(STATS_EVENTS_URL.length()));
        } catch (NumberFormatException e) {
            log.warn("Failed to extract eventId from uri: {}", uri);
            return -1L;
        }
    }
}