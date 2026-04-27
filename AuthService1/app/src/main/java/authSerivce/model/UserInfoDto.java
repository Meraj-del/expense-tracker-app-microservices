package authSerivce.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDto {

    private String userId;
    private String username;
    private String email;
    private String password;

    @NonNull
    private String firstName;
    @NonNull
    private String lastName;
    @NonNull
    private Long phoneNumber;
}