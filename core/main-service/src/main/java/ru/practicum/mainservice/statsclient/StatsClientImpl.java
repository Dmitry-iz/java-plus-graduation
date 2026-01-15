//package ru.practicum.mainservice.statsclient;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import ru.practicum.statsclient.client.StatsClient;
//
//@Component
//public class StatsClientImpl extends StatsClient {
//
//    @Autowired
//    public StatsClientImpl(@Value("${stats.server.url}") String serverUrl) {
//        super(serverUrl);
//    }
//}
//
//package ru.practicum.mainservice.statsclient;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import ru.practicum.statsclient.client.StatsClient;
//
//@Component
//public class StatsClientImpl extends StatsClient {
//
//    @Autowired
//    public StatsClientImpl(@Value("${stats.server.url}") String serverUrl) {
//        super(serverUrl);
//    }
////}
//
//package ru.practicum.mainservice.statsclient;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.client.loadbalancer.LoadBalanced;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//import ru.practicum.statsclient.client.StatsClient;
//
//@Component
//public class StatsClientImpl extends StatsClient {
//
//    @Autowired
//    public StatsClientImpl(@LoadBalanced RestTemplate restTemplate) {
//
//        super(restTemplate, "stats-server");
//    }
//}


package ru.practicum.mainservice.statsclient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDTO;
import ru.practicum.dto.ViewStatsDTO;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClientImpl {

    private static final String STATS_SERVICE_ID = "STATS-SERVER";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final RestTemplate restTemplate; // Используется @Primary бин
    // ИЛИ с @Qualifier, если хотите быть более явными:
    // private final @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate;

    /**
     * Сохраняет информацию о просмотре события
     */
    public void saveHit(EndpointHitDTO endpointHitDTO) {
        try {
            URI uri = buildUri("/hit");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<EndpointHitDTO> requestEntity = new HttpEntity<>(endpointHitDTO, headers);

            log.debug("Sending hit to stats server: {}", endpointHitDTO);
            restTemplate.postForEntity(uri, requestEntity, Void.class);
            log.debug("Hit successfully sent to stats server");

        } catch (Exception e) {
            log.error("Failed to save hit to stats server: {}", e.getMessage(), e);
        }
    }

    /**
     * Сохраняет список просмотров (пакетно)
     */
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
            restTemplate.postForEntity(uri, requestEntity, Void.class);
            log.debug("Batch hits successfully sent to stats server");

        } catch (Exception e) {
            log.warn("Batch save failed, falling back to single saves: {}", e.getMessage());

            // Fallback: сохраняем по одному
            for (EndpointHitDTO hit : hits) {
                try {
                    saveHit(hit);
                } catch (Exception ex) {
                    log.error("Failed to save hit in fallback mode: {}", ex.getMessage());
                }
            }
        }
    }

    /**
     * Получает статистику просмотров
     */
    public List<ViewStatsDTO> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        validateDates(start, end);

        try {
            URI uri = buildStatsUri(start, end, uris, unique);

            log.debug("Getting stats from stats server: start={}, end={}, uris={}, unique={}",
                    start, end, uris, unique);

            ViewStatsDTO[] response = restTemplate.getForObject(uri, ViewStatsDTO[].class);
            List<ViewStatsDTO> stats = response != null ? Arrays.asList(response) : List.of();

            log.debug("Received {} stats records from stats server", stats.size());
            return stats;

        } catch (Exception e) {
            log.error("Failed to get stats from stats server: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Строит URI для получения статистики
     */
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
                uriBuilder.queryParam("uris", String.join(",", uris));
            }

            return uriBuilder.build().toUri();
        });
    }

    /**
     * Строит базовый URI для stats-сервера
     */
    private URI buildUri(String path) {
        return retryTemplate.execute(context -> {
            ServiceInstance instance = getStatsServiceInstance();
            return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
        });
    }

    /**
     * Получает инстанс stats-сервера из Eureka
     */
    private ServiceInstance getStatsServiceInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);

        if (instances == null || instances.isEmpty()) {
            String errorMsg = "Stats service (" + STATS_SERVICE_ID + ") not found in Eureka registry";
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        // Берем первый доступный инстанс
        ServiceInstance instance = instances.get(0);
        log.debug("Found stats service instance: {}:{}", instance.getHost(), instance.getPort());

        return instance;
    }

    /**
     * Валидация дат
     */
    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Dates must not be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }
}