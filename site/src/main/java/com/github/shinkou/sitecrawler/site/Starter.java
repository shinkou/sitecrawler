package com.github.shinkou.sitecrawler.site;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource({"classpath:/site.properties", "${conf.path:classpath:/conf/site.conf}"})
public class Starter
{
	public static ConfigurableApplicationContext appContext;

	public static void main(String[] args)
	{
		appContext = SpringApplication.run(Starter.class, args);
		appContext.registerShutdownHook();
	}
}
