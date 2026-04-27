package com.dev.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoDto {

    @NonNull
    private String userId;

    @NonNull
    private String firstName;

    @NonNull
    private String lastName;

    @NonNull
    private Long phoneNumber;

    @NonNull
    private String email;

    private String profilePic;

    public UserInfo transformToUserInfo(){
        return UserInfo.builder().userId(userId).firstName(firstName).lastName(lastName).email(email).phoneNumber(phoneNumber).profilePic(profilePic).build();
    }

}
