package authSerivce.controller;

import lombok.AllArgsConstructor;
import authSerivce.entities.RefreshToken;
import authSerivce.request.AuthRequestDTO;
import authSerivce.request.RefreshTokenRequestDTO;
import authSerivce.response.JwtResponseDTO;
import authSerivce.service.JwtService;
import authSerivce.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@AllArgsConstructor
@RestController
public class TokenController {


    private final AuthenticationManager authenticationManager;


    private final RefreshTokenService refreshTokenService;


    private final JwtService jwtService;

    @PostMapping("/auth/v1/login")
    public ResponseEntity<JwtResponseDTO> login(
            @RequestBody AuthRequestDTO dto
    ) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getUsername(),
                            dto.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
            );
        }


        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(dto.getUsername());

        return ResponseEntity.ok(
                JwtResponseDTO.builder()
                        .accessToken(jwtService.generateToken(dto.getUsername()))
                        .refreshToken(refreshToken.getToken())
                        .build()
        );
    }


    @PostMapping("/auth/v1/refreshToken")
    public ResponseEntity<JwtResponseDTO> refresh(
            @RequestBody RefreshTokenRequestDTO dto
    ) {

        return refreshTokenService.findByToken(dto.getToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserInfo)
                .map(user -> ResponseEntity.ok(
                        JwtResponseDTO.builder()
                                .accessToken(
                                        jwtService.generateToken(user.getUsername())
                                )
                                .refreshToken(dto.getToken())
                                .build()
                ))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Invalid refresh token"
                        )
                );
    }

}
