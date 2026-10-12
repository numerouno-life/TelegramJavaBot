package ru.conf;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "work_day_status",
                "work_time_date",
                "work_schedule_all",
                "lunch_break_day",
                "lunch_break_all",
                "user_admin",
                "user_blocked"
        );
    }
}
