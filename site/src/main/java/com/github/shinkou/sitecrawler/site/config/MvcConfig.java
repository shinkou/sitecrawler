package com.github.shinkou.sitecrawler.site.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class MvcConfig extends WebMvcConfigurerAdapter
{
	@Override
	public void configurePathMatch(PathMatchConfigurer configurer)
	{
		configurer.setUseSuffixPatternMatch(false);
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry)
	{
		registry.addViewController("/home").setViewName("home");
		registry.addViewController("/").setViewName("home");
		registry.addViewController("/index").setViewName("index");
		registry.addViewController("/login").setViewName("login");
	}
}
