package authSerivce.repository;

import authSerivce.entities.UserInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<UserInfo,String> {

    public UserInfo findByUsername(String username);
    public UserInfo findByEmail(String email);
}
