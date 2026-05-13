package com.dev.consumer;

import com.dev.entities.UserInfo;
import com.dev.entities.UserInfoDto;
import com.dev.redisIdempotency.RedisLockService;
import com.dev.repository.UserRepository;
import com.dev.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceConsumer {

    private final UserService userService;

    private final RedisLockService redisLockService;

    @PostConstruct
    public void init() {
        log.info("AuthServiceConsumer initialized. Listening to Kafka topic for user events.");
    }

    @PreDestroy
    public void cleanup() {
        log.info("AuthServiceConsumer shutting down. Stopping Kafka listener cleanly.");
    }

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void listner(UserInfoDto eventData) {

        String lockKey = "lock:user:" + eventData.getUserId();

        boolean locked = redisLockService.acquireLock(lockKey, 5000);

        if (!locked) {
            log.warn("Duplicate processing detected for userId: {}", eventData.getUserId());
            return;
        }
        try {
            userService.createOrUpdateUser(eventData);
            log.info("User saved to DB: {}", eventData);
        } catch (Exception e) {
            log.error("Failed to process user event for userId: {}",
                    eventData.getUserId(), e);
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }

}
