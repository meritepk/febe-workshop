package io.github.merite.training.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService, ApplicationListener<AbstractAuthenticationEvent> {

    public static final String STATUS_ACTIVE = "ACTIVE";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UserRepository repository;

    @Value("${login.attempts.max:3}")
    private int loginAttemptsMax = 3;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Cacheable("users")
    public UserDetails loadUserByUsername(String jti, String userName) throws UsernameNotFoundException {
        return loadUserByUsername(userName);
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = repository.findByUserName(userName);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid user: " + userName);
        }
        boolean enabled = STATUS_ACTIVE.equals(user.getStatus());
        boolean locked = user.getLoginAttempts() == null || user.getLoginAttempts() < loginAttemptsMax;
        return new org.springframework.security.core.userdetails.User(user.getUserName(), user.getPassword(), enabled,
                true, true, locked, AuthorityUtils.commaSeparatedStringToAuthorityList(user.getRoles()));
    }

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        logger.debug("::security:: authentication event: {}", event);
        if (event instanceof AuthenticationSuccessEvent success) {
            logger.info("::security:: authentication successful for user: {}", success.getAuthentication().getName());
        } else if (event instanceof InteractiveAuthenticationSuccessEvent success) {
            logger.info("::security:: login successful for user: {}", success.getAuthentication().getName());
        } else if (event instanceof AbstractAuthenticationFailureEvent failure) {
            logger.warn("::security:: authentication failed for user: {}, reason: {}",
                    failure.getAuthentication().getName(), String.valueOf(failure.getException()));
        } else {
            logger.info("::security:: authentication event for user: {}, {}", event.getAuthentication().getName(),
                    event);
        }
    }
}
