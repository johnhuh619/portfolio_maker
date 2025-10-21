package io.resume.make.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;


import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient() {

        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(100)    // 최대 연결 수
                .maxIdleTime(Duration.ofSeconds(20))    // 유휴 연결 유지 시간
                .maxLifeTime(Duration.ofSeconds(60))    // 연결 최대 생명 주기
                .pendingAcquireTimeout(Duration.ofSeconds(60))  // 연결 대기 타임 아웃
                .evictInBackground(Duration.ofSeconds(120))     // 백그라운드 정리 주기
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                )
                .responseTimeout(Duration.ofSeconds(10));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

}