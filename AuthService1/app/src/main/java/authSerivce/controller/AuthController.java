package authSerivce.controller;

import authSerivce.entities.RefreshToken;
import authSerivce.model.UserInfoDto;
import authSerivce.response.JwtResponseDTO;
import authSerivce.service.JwtService;
import authSerivce.service.RefreshTokenService;
import authSerivce.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@Slf4j
public class AuthController {


    private final JwtService jwtService;


    private final RefreshTokenService refreshTokenService;


    private final UserDetailsServiceImpl userDetailsService;

    @GetMapping("/auth/v1/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminOnly() {
        return ResponseEntity.ok("Admin access granted");
    }

    @PostMapping("/auth/v1/signup")
    public ResponseEntity<?> registerUser(@RequestBody UserInfoDto userInfoDto)
    {
        try {
            Boolean isSignUped = userDetailsService.signupUser(userInfoDto);
            if(Boolean.FALSE.equals(isSignUped)){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User already exists or invalid data");
            }

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userInfoDto.getUsername());
            String jwtToken = jwtService.generateToken(userInfoDto.getUsername());

            JwtResponseDTO response = JwtResponseDTO.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken.getToken()).build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Signup failed for username: {}. Error: {}",
                    userInfoDto.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Exception in User Service: " + e.getMessage());
        }
    }

    @GetMapping("/auth/v1/ping")
    public ResponseEntity<String> ping() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String userId = authentication.getName();
            return ResponseEntity.ok(userId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
    }
}
