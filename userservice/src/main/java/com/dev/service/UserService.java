package com.dev.service;

import com.dev.entities.UserInfo;
import com.dev.entities.UserInfoDto;
import com.dev.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserInfoDto createOrUpdateUser(UserInfoDto userInfoDto) {

        Function<UserInfo,UserInfo>updateUser=user->{
            //implement update logic
            user.setUserId(userInfoDto.getUserId());
            user.setFirstName(userInfoDto.getFirstName());
            user.setLastName(userInfoDto.getLastName());
            user.setEmail(userInfoDto.getEmail());
            user.setPhoneNumber(Long.valueOf(userInfoDto.getPhoneNumber()));
            user.setProfilePic(userInfoDto.getProfilePic());

            System.out.println(user);
            return userRepository.save(user);
        };

        Supplier<UserInfo>createUser=()->{
            return userRepository.save(userInfoDto.transformToUserInfo());
        };

        UserInfo userInfo=userRepository.findByUserId(userInfoDto.getUserId())
                .map(updateUser)
                .orElseGet(createUser);

        return new UserInfoDto(
                userInfo.getUserId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getPhoneNumber(),
                userInfo.getEmail(),
                userInfo.getProfilePic()
        );
    }

    public UserInfoDto getUser(UserInfoDto userInfoDto) throws Exception {
        Optional<UserInfo> userInfo=userRepository.findByUserId(userInfoDto.getUserId());
        if(userInfo.isEmpty()){
            throw new Exception("User not found");
        }
        UserInfo user=userInfo.get();
        return new UserInfoDto(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getProfilePic()
        );
    }


}
