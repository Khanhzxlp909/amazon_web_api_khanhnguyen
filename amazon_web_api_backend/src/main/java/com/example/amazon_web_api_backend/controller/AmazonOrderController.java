package com.example.amazon_web_api_backend.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class AmazonOrderController {

    @Value("${amazon.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://sellingpartnerapi-na.amazon.com";
    private LoadingCache<String, Map> orderDetailCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
            new CacheLoader<String, Map>() {
              @Override
              public Map load(String orderId) throws Exception {
                String detailUrl = BASE_URL + "/orders/v0/orders/" + orderId;
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/json");
                headers.set("x-amz-access-token", accessToken);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> detailRes = restTemplate.exchange(detailUrl, HttpMethod.GET, entity, Map.class);
                return detailRes.getBody();
              }
            });

    private LoadingCache<String, List<Map<String, Object>>> orderItemsCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(
            new CacheLoader<String, List<Map<String, Object>>>() {
              @Override
              public List<Map<String, Object>> load(String orderId) throws Exception {
                String itemsUrl = BASE_URL + "/orders/v0/orders/" + orderId + "/orderItems";
                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/json");
                headers.set("x-amz-access-token", accessToken);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> itemsRes = restTemplate.exchange(itemsUrl, HttpMethod.GET, entity, Map.class);
                Map<String, Object> payload = (Map<String, Object>) itemsRes.getBody().get("payload");

                if (payload != null && payload.get("OrderItems") instanceof List<?>) {
                  return (List<Map<String, Object>>) payload.get("OrderItems");
                } else {
                  return List.of();
                }
              }
            });

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        String listOrdersUrl = BASE_URL + "/orders/v0/orders";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("x-amz-access-token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(listOrdersUrl)
                .queryParam("MarketplaceIds", "ATVPDKIKX0DER")
                .queryParam("CreatedAfter", "2024-01-01T00:00:00Z")
                .queryParam("MaxResultsPerPage", 5);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, entity, Map.class
            );

            Map<String, Object> payloads = (Map<String, Object>) response.getBody().get("payload");
            List<Map<String, Object>> orders = payloads != null
                    ? (List<Map<String, Object>>) payloads.get("Orders")
                    : new ArrayList<>();

            List<Map<String, Object>> fullOrders = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();

            if (orders != null) {
                for (Map<String, Object> order : orders) {
                    String orderId = (String) order.get("AmazonOrderId");
                    Map<String, Object> fullOrder = new HashMap<>();
                    fullOrder.put("summary", order);

                    // Get order detail from cache
                    try {
                        fullOrder.put("detail", orderDetailCache.get(orderId));
                    } catch (Exception ex) {
                        fullOrder.put("detail", Map.of("error", ex.getMessage()));
                    }

                    // Get order items from cache
                    try {
                        fullOrder.put("items", orderItemsCache.get(orderId));
                    } catch (Exception ex) {
                        fullOrder.put("items", Map.of("error", ex.getMessage()));
                    }

                    fullOrders.add(fullOrder);
                }
            }

            return ResponseEntity.ok(fullOrders);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching full Amazon orders: " + e.getMessage());
        }
    }
}
