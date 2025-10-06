package ru.practicum.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.HitDto;
import ru.practicum.dto.StatsDto;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class StatsClient {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    @Value("${stats.server.url:http://localhost:9090}")
    private String url;
    @Value("${stats.server.name:STATS-SERVER}")
    private String statServerName;

    private final Random random = new Random();

    public void postHit(HitDto dto) {
        restTemplate.postForEntity(getUrl() + "/hit", dto, Void.class);
    }

    public List<StatsDto> getStats(String start, String end, List<String> uris, boolean unique) throws RestClientException {

        StringBuilder sb = new StringBuilder(getUrl()).append("/stats")
                .append("?start=").append(start)
                .append("&end=").append(end)
                .append("&unique=").append(unique);

        if (uris != null) {
            for (String uri : uris) {
                sb.append("&uris=").append(uri);
            }
        }

        ResponseEntity<StatsDto[]> response = restTemplate.getForEntity(sb.toString(), StatsDto[].class);

        StatsDto[] body = response.getBody();
        return (body == null) ? Collections.emptyList() : List.of(body);
    }

    private String getUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances("stats-server");
        return instances.get(random.nextInt(instances.size())).getUri().toString();
    }
}