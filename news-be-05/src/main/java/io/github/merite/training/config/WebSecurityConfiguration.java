package io.github.merite.training.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

@EnableMethodSecurity
@Configuration
public class WebSecurityConfiguration {

    @Value("${spring.web.security.ignored:/error,/ui/**}")
    private String[] ignored = { "/error", "/ui/**" };

    @Value("${spring.web.security.ignored.get:/api/v1/news}")
    private String[] ignoredGet = { "/api/v1/news" };

    @Value("${spring.web.security.api:/api/**}")
    private String api = "/api/**";

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(ignored).requestMatchers(HttpMethod.GET, ignoredGet);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.formLogin(Customizer.withDefaults());
        http.exceptionHandling(config -> config.defaultAuthenticationEntryPointFor(
                (request, response, ex) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not Authorized"),
                AntPathRequestMatcher.antMatcher(api)));
        http.csrf(config -> config.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        http.authorizeHttpRequests(config -> config.anyRequest().authenticated());
        return http.build();
    }
}
