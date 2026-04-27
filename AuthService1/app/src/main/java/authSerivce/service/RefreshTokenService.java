package authSerivce.service;

import lombok.AllArgsConstructor;
import authSerivce.entities.RefreshToken;
import authSerivce.entities.UserInfo;
import authSerivce.repository.RefreshTokenRepository;
import authSerivce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RefreshTokenService {

    @Autowired
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private final UserRepository userRepository;

    @Transactional
    public RefreshToken createRefreshToken(String username) {

        UserInfo user = userRepository.findByUsername(username);

        refreshTokenRepository.deleteByUserId(user.getUserId());

        String tokenValue;
        do {
            tokenValue = UUID.randomUUID().toString();
        } while (refreshTokenRepository.existsByToken(tokenValue));

        RefreshToken token = RefreshToken.builder()
                .userInfo(user)
                .token(tokenValue)
                .expiryDate(Instant.now().plusSeconds(60 * 60 * 24 * 7)) // 7 days
                .build();

        return refreshTokenRepository.save(token);
    }



    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired");
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}
