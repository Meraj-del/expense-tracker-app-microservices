package authSerivce.service;

import authSerivce.entities.UserRole;
import authSerivce.repository.UserRoleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import authSerivce.entities.UserInfo;
import authSerivce.eventProducer.UserInfoProducer;
import authSerivce.model.UserInfoDto;
import authSerivce.repository.UserRepository;
import authSerivce.util.ValidationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
@Transactional
public class UserDetailsServiceImpl implements UserDetailsService {


    private final UserRepository userRepository;


    private final PasswordEncoder passwordEncoder;


    private final UserInfoProducer userInfoProducer;

    private final UserRoleRepository userRoleRepository;


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

        UserRole userRole = userRoleRepository.findByName("ROLE_USER")
                        .orElseGet(()->{
                            UserRole newUserRole = new UserRole();
                            newUserRole.setName("ROLE_USER");
                            return userRoleRepository.save(newUserRole);
                        });
        newUser.setRoles(new HashSet<>(Set.of(userRole)));

        userRepository.save(newUser);

        UserInfoDto event = UserInfoDto.builder()
                .userId(newUser.getUserId())
                .username(newUser.getUsername())
                .email(newUser.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phoneNumber(dto.getPhoneNumber())
                .build();

        log.info("Publishing user event to Kafka for userId: {}", newUser.getUserId());
        userInfoProducer.sendEventToKafka(event);

        return true;
    }

}
