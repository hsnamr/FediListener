package com.activitypub.listener.activitypub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityPubClient {
    
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${activitypub.user-agent:ActivityPubListener/1.0}")
    private String userAgent;
    
    @Value("${activitypub.request-timeout:30000}")
    private int requestTimeout;
    
    private WebClient getWebClient() {
        return webClientBuilder
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * Discover actor using WebFinger protocol
     * GET /.well-known/webfinger?resource=acct:user@instance.com
     */
    public Mono<WebFingerResponse> discoverActor(String resource) {
        String[] parts = resource.replace("acct:", "").split("@");
        if (parts.length != 2) {
            return Mono.error(new IllegalArgumentException("Invalid resource format: " + resource));
        }
        
        String username = parts[0];
        String instance = parts[1];
        String webfingerUrl = String.format("https://%s/.well-known/webfinger?resource=acct:%s", instance, resource);
        
        log.debug("Discovering actor via WebFinger: {}", webfingerUrl);
        
        return getWebClient()
                .get()
                .uri(webfingerUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofMillis(requestTimeout))
                .map(json -> {
                    WebFingerResponse response = new WebFingerResponse();
                    response.setSubject(json.get("subject").asText());
                    
                    if (json.has("links")) {
                        json.get("links").forEach(link -> {
                            String rel = link.get("rel").asText();
                            if ("self".equals(rel) && link.has("href")) {
                                response.setActorUrl(link.get("href").asText());
                            }
                        });
                    }
                    return response;
                })
                .doOnError(error -> log.error("Error discovering actor: {}", error.getMessage()));
    }
    
    /**
     * Retrieve actor profile
     * GET /users/{username} with Accept: application/activity+json
     */
    public Mono<JsonNode> getActorProfile(String actorUrl) {
        log.debug("Retrieving actor profile: {}", actorUrl);
        
        return getWebClient()
                .get()
                .uri(actorUrl)
                .header(HttpHeaders.ACCEPT, "application/activity+json, application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofMillis(requestTimeout))
                .doOnError(error -> log.error("Error retrieving actor profile: {}", error.getMessage()));
    }
    
    /**
     * Get actor's outbox (first page).
     * GET /users/{username}/outbox?page=true
     */
    public Mono<JsonNode> getActorOutbox(String outboxUrl, boolean page) {
        String url = page ? outboxUrl + "?page=true" : outboxUrl;
        return getOutboxPage(url);
    }

    /**
     * Fetch outbox/collection by full URL (supports first page or "next" page URL).
     */
    public Mono<JsonNode> getOutboxPage(String fullUrl) {
        log.debug("Retrieving outbox page: {}", fullUrl);
        return getWebClient()
                .get()
                .uri(fullUrl)
                .header(HttpHeaders.ACCEPT, "application/activity+json, application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofMillis(requestTimeout))
                .doOnError(error -> log.error("Error retrieving outbox: {}", error.getMessage()));
    }
    
    /**
     * Get actor's inbox (if accessible)
     * GET /users/{username}/inbox?page=true
     */
    public Mono<JsonNode> getActorInbox(String inboxUrl, boolean page) {
        String url = page ? inboxUrl + "?page=true" : inboxUrl;
        log.debug("Retrieving actor inbox: {}", url);
        
        return getWebClient()
                .get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, "application/activity+json, application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofMillis(requestTimeout))
                .doOnError(error -> log.error("Error retrieving inbox: {}", error.getMessage()));
    }
    
    /**
     * Get NodeInfo for an instance
     * GET /.well-known/nodeinfo
     */
    public Mono<JsonNode> getNodeInfo(String instanceUrl) {
        String nodeinfoUrl = instanceUrl + "/.well-known/nodeinfo";
        log.debug("Retrieving NodeInfo: {}", nodeinfoUrl);
        
        return getWebClient()
                .get()
                .uri(nodeinfoUrl)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofMillis(requestTimeout))
                .doOnError(error -> log.error("Error retrieving NodeInfo: {}", error.getMessage()));
    }
    
    public static class WebFingerResponse {
        private String subject;
        private String actorUrl;
        
        public String getSubject() {
            return subject;
        }
        
        public void setSubject(String subject) {
            this.subject = subject;
        }
        
        public String getActorUrl() {
            return actorUrl;
        }
        
        public void setActorUrl(String actorUrl) {
            this.actorUrl = actorUrl;
        }
    }
}
