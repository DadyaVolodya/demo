package com.example.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Map.Entry.comparingByValue;

@Service
public class WeatherService {
    private final String apiKey = "YOUR_API_KEY";
    private final String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric";
    private final RestTemplate restTemplate = new RestTemplate();

    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();
    private final boolean pollingMode = true; // true for polling mode, false for on-demand mode

    public WeatherData getWeather(String city) {
        if (cache.containsKey(city) && cache.get(city).isValid()) {
            return cache.get(city).data();
        }
        return fetchAndCacheWeather(city);
    }

    private WeatherData fetchAndCacheWeather(String city) {
        try {
            String url = String.format(apiUrl, city, apiKey);
            WeatherData data = restTemplate.getForObject(url, WeatherData.class);
            if (data != null) {
                cacheWeather(city, data);
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching weather data: " + e.getMessage());
        }
    }

    private void cacheWeather(String city, WeatherData data) {
        if (cache.size() >= 10) {
            String oldestCity = cache.entrySet()
                    .stream()
                    .min(Comparator.comparing(entry -> entry.getValue().timestamp()))
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (oldestCity != null) {
                cache.remove(oldestCity);
            }
        }
        cache.put(city, new CachedWeather(data));
    }

    @Scheduled(fixedRate = 600_000) // Update every 10 minutes
    public void updateCachedWeather() {
        if (!pollingMode) return;
        cache.keySet().forEach(this::fetchAndCacheWeather);
    }

    private record CachedWeather(WeatherData data, Instant timestamp) {
        CachedWeather(WeatherData data) {
            this(data, Instant.now());
        }
        boolean isValid() {
            return Instant.now().isBefore(timestamp.plusSeconds(600));
        }
    }

    class WeatherData {
        private String name;
        private Weather weather;
        private Temperature main;
        private int visibility;
        private Wind wind;
        private long dt;
        private Sys sys;
        private int timezone;

        public String getName() { return name; }
        public Weather getWeather() { return weather; }
        public Temperature getMain() { return main; }
        public int getVisibility() { return visibility; }
        public Wind getWind() { return wind; }
        public long getDt() { return dt; }
        public Sys getSys() { return sys; }
        public int getTimezone() { return timezone; }
    }

    class Weather {
        private String main;
        private String description;
        public String getMain() { return main; }
        public String getDescription() { return description; }
    }

    class Temperature {
        private double temp;
        private double feels_like;
        public double getTemp() { return temp; }
        public double getFeels_like() { return feels_like; }
    }

    class Wind {
        private double speed;
        public double getSpeed() { return speed; }
    }

    class Sys {
        private long sunrise;
        private long sunset;
        public long getSunrise() { return sunrise; }
        public long getSunset() { return sunset; }
    }
}
