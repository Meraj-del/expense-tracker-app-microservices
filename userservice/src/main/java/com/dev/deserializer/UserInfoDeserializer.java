package com.dev.deserializer;

import com.dev.entities.UserInfoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class UserInfoDeserializer implements Deserializer<UserInfoDto> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public UserInfoDto deserialize(String topic, byte[] data) {
        try {
            return mapper.readValue(data, UserInfoDto.class);
        } catch (Exception e) {
            System.err.println("Failed to deserialize UserInfoDto: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {}

}
