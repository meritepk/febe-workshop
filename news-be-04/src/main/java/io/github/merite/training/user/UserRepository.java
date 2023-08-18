package io.github.merite.training.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUserName(String userName);

    @Query(value = "SELECT u FROM Users u WHERE u.userName = ?1")
    User findByUserName2(String userName);

    @Query(value = "SELECT * FROM users WHERE user_name = ?", nativeQuery = true)
    User findByUserName3(String userName);
}
