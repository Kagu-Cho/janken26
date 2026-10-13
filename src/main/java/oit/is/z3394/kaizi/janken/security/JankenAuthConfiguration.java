package oit.is.z3394.kaizi.janken.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class JankenAuthConfiguration {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/janken", "/janken/**").authenticated()
            .anyRequest().permitAll())
        .formLogin(form -> form
            .permitAll())
        .logout(logout -> logout
            .logoutSuccessUrl("/")
            .permitAll());

    return http.build();
  }

  @Bean
  public UserDetailsService userDetailsService() {

    UserDetails user1 = User.withUsername("user1")
        .password("{bcrypt}$2y$10$7P.R22aFi50WC/8cEauO6O6WUrTRZDKVC/pUMQckHdVknVW.EUOly")
        .roles("USER")
        .build();

    UserDetails user2 = User.withUsername("user2")
        .password("{bcrypt}$2y$10$cSRkKszv4KKHrwdT4o9/7OTwU1519QXl03hIFC5KT/ErXuXrIFIjS")
        .roles("USER")
        .build();

    return new InMemoryUserDetailsManager(user1, user2);
  }
}
