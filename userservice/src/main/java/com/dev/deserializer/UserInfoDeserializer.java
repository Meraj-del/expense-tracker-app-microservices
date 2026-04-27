package com.dev.deserializer;

import com.dev.entities.UserInfoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class UserInfoDeserializer implements Deserializer<UserInfoDto> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public UserInfoDto deserialize(String topic, byte[] data) {

        ObjectMapper mapper = new ObjectMapper();
        UserInfoDto user = null;

        try {
            user = mapper.readValue(data, UserInfoDto.class);
        } catch (Exception e) {
            System.out.println("Can't deserialize UserInfoDto");
            e.printStackTrace();
        }

        return user;
    }

    @Override
    public void close() {}

}
