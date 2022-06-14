package az.kapitalbank.marketplace.config;

import static az.kapitalbank.marketplace.constant.CommonConstant.SECURITY_POLICY;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.headers().contentSecurityPolicy(SECURITY_POLICY);
        http.csrf().disable();
        http.authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .csrf().disable()
                .cors().disable()
                .httpBasic().disable()
                .formLogin().disable()
                .logout().disable()
                .oauth2Client();
    }
}
