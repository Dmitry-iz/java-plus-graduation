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
//}

package ru.practicum.mainservice.statsclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.statsclient.client.StatsClient;

@Component
public class StatsClientImpl extends StatsClient {

    @Autowired
    public StatsClientImpl(@LoadBalanced RestTemplate restTemplate) {
        // Используем конструктор с RestTemplate и именем сервиса
        super(restTemplate, "stats-server");
    }
}