package com.github.shinkou.sitecrawler.site.config;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter
{
	@Value("${site.users}")
	protected String m_accounts;

	@Override
	protected void configure(HttpSecurity http) throws Exception
	{
		http.csrf().disable();
		http.authorizeRequests()
				.antMatchers("/", "/home").permitAll()
				.anyRequest().authenticated()
				.and()
			.formLogin()
				.loginPage("/login")
				.permitAll()
				.and()
			.logout()
				.permitAll();
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth)
		throws Exception
	{
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, String>> accs = mapper.readValue
		(
			m_accounts
			, new TypeReference<List<Map<String, String>>>(){}
		);

		for(Map<String, String> acc: accs)
		{
			String username = acc.get("username");
			String password = acc.get("password");
			String roles = acc.get("roles");

			if (null == username || null == password || null == roles)
				continue;

			auth.inMemoryAuthentication()
				.withUser(username).password(password).roles(roles);
		};
	}
}
