package authSerivce.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import authSerivce.entities.UserInfo;
import authSerivce.eventProducer.UserInfoProducer;
import authSerivce.model.UserInfoDto;
import authSerivce.repository.UserRepository;
import authSerivce.util.ValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.UUID;

@Service
@AllArgsConstructor
@Transactional
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private final UserInfoProducer userInfoProducer;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo user = userRepository.findByUsername(username);
        if(user == null) throw new UsernameNotFoundException("User not found: " + username);
        return new CustomUserDetails(user);
    }

    public Boolean signupUser(UserInfoDto dto) {

        String error = ValidationUtil.validate(dto);
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }

        UserInfo existingUser = userRepository.findByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username already taken"
            );
        }

        UserInfo existingEmail = userRepository.findByEmail(dto.getEmail());
        if (existingEmail != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email already registered"
            );
        }

        UserInfo newUser = new UserInfo();
        newUser.setUserId(UUID.randomUUID().toString());
        newUser.setUsername(dto.getUsername());
        newUser.setEmail(dto.getEmail());
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));
        newUser.setRoles(new HashSet<>());

        userRepository.save(newUser);

        UserInfoDto event = UserInfoDto.builder()
                .userId(newUser.getUserId())
                .username(newUser.getUsername())
                .email(newUser.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phoneNumber(dto.getPhoneNumber())
                .build();

        System.out.println("Sending Event: " + event);
        userInfoProducer.sendEventToKafka(event);

        return true;
    }

}
