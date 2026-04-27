package authSerivce.repository;

import authSerivce.entities.RefreshToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken,Integer> {

    Optional<RefreshToken> findByToken(String token);
    boolean existsByToken(String token);

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.userInfo.id = :userId")
    void deleteByUserId(String userId);
}
