package com.dev.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
    @Id
    @Column(unique = true,nullable = false)
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

}
