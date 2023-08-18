package io.github.merite.training.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.WebUtils;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import io.github.merite.training.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@EnableMethodSecurity
@Configuration
public class WebSecurityConfiguration {

    @Value("${spring.web.security.ignored:/error,/ui/**,/favicon.ico}")
    private String[] ignored = { "/error", "/ui/**", "/favicon.ico" };

    @Value("${spring.web.security.ignored.get:/api/v1/news}")
    private String[] ignoredGet = { "/api/v1/news" };

    @Value("${spring.web.security.api:/api/**}")
    private String api = "/api/**";

    @Value("${spring.web.security.session.cookie.name:SESSIONID}")
    private String sessionId = "SESSIONID";

    @Value("${spring.web.security.jwt.secret.key:fBnKDJkuDDBeejkgYCK+zz4pcyc+bfrYeTTkOqyj7Uo}")
    private String secretKey = "fBnKDJkuDDBeejkgYCK+zz4pcyc+bfrYeTTkOqyj7Uo";

    @Value("${spring.web.security.session.expiry.seconds:28800}")
    private int sessionExpirySeconds = 28800;

    private final UserService userService;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public WebSecurityConfiguration(UserService userService) {
        this.userService = userService;
        SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(secretKey), "RSA");
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKeySpec));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec).build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(ignored).requestMatchers(HttpMethod.GET, ignoredGet);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.formLogin(config -> config.successHandler(authenticationSuccessHandler()));
        http.exceptionHandling(config -> config.defaultAuthenticationEntryPointFor(authenticationEntryPoint(),
                AntPathRequestMatcher.antMatcher(api)));
        http.csrf(config -> config.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        http.authorizeHttpRequests(config -> config.requestMatchers("/actuator/**").hasAnyAuthority("ACTUATOR")
                .anyRequest().authenticated());
        http.sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.oauth2ResourceServer(config -> config.opaqueToken(Customizer.withDefaults()));
        http.logout(config -> config.addLogoutHandler(new CookieClearingLogoutHandler(sessionId)));
        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized");
    }

    private AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, auth) -> response.addCookie(createSessionCookie(encode(auth)));
    }

    private Cookie createSessionCookie(String token) {
        Cookie cookie = new Cookie(sessionId, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        return cookie;
    }

    private String encode(Authentication auth) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(auth.getName())
                .id(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(sessionExpirySeconds).toInstant(ZoneOffset.UTC))
                .build();
        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(header, claims));
        return jwt.getTokenValue();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return request -> resolveBearerToken(WebUtils.getCookie(request, sessionId));
    }

    private String resolveBearerToken(Cookie cookie) {
        String token = null;
        if (cookie != null) {
            token = cookie.getValue();
        }
        return token;
    }

    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return token -> introspectorToken(token);
    }

    private OAuth2AuthenticatedPrincipal introspectorToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            UserDetails userDetails = userService.loadUserByUsername(jwt.getId(), jwt.getSubject());
            return new DefaultOAuth2User(userDetails.getAuthorities(), Map.of("sub", userDetails.getUsername()), "sub");
        } catch (Exception e) {
            throw new CredentialsExpiredException(e.getMessage(), e);
        }
    }
}
