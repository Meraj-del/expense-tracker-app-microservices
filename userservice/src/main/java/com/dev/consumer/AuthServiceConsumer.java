package com.dev.consumer;

import com.dev.entities.UserInfo;
import com.dev.entities.UserInfoDto;
import com.dev.redisIdempotency.RedisLockService;
import com.dev.repository.UserRepository;
import com.dev.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceConsumer {

    private UserService userService;

    private RedisLockService redisLockService;

    @Autowired
    AuthServiceConsumer(UserService userService, RedisLockService redisLockService) {
        this.userService = userService;
        this.redisLockService = redisLockService;
    }

    @KafkaListener(topics = "${spring.kafka.topic.name}",
            groupId = "${spring.kafka.consumer.group-id}")
    @Transactional //if Db fails->transaction roll back kafka offset won't commit , message will retry safeky
    public void listner(UserInfoDto eventData){

//TODO : Make it Transactional to handle idempotency and validate email, phone etc: can use Raddis  distributed lock by using user id
        String lockKey = "lock:user:" + eventData.getUserId();

        boolean locked = redisLockService.acquireLock(lockKey, 5000);

        if (!locked) {
            System.out.println("Duplicate processing detected for: " + eventData.getUserId());
            return;
        }
        try {
            userService.createOrUpdateUser(eventData);
            System.out.println("Saved to DB: " + eventData);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }

}
