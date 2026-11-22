package com.Shawn.dream_shops.security.config;

import com.Shawn.dream_shops.security.jwt.AuthTokenFilter;
import com.Shawn.dream_shops.security.jwt.JwtAuthEntryPoint;
import com.Shawn.dream_shops.security.service.ShopUserDetailsService;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class ShopConfig {

    @Autowired
    private final ShopUserDetailsService userDetailsService;
    @Autowired
    private final JwtAuthEntryPoint authEntryPoint;

    @Value("${api.prefix}")
    private String apiPrefix;

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthTokenFilter authTokenFilter()
    {
        return new AuthTokenFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider()
    {
        var authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authEntryPoint))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, apiPrefix + "/users/add").permitAll()
                        .requestMatchers(HttpMethod.POST, apiPrefix + "/categories/**",
                                apiPrefix + "/images/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, apiPrefix + "/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, apiPrefix + "/categories/**",
                                apiPrefix + "/images/**").hasRole("ADMIN")
                        .requestMatchers(apiPrefix + "/carts/**", apiPrefix + "/cartItems/**",
                                apiPrefix + "/orders/**", apiPrefix + "/users/**").authenticated()
                        .anyRequest().permitAll());

        // 不允許 csrf 跨站存取
        // 設置 Session is stateless 防止造成 xss


        http.authenticationProvider(daoAuthenticationProvider());

        http.addFilterBefore(authTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        // authTokenFilter 就是 JwtToken

        return http.build();
    }
}
