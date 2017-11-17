package com.github.shinkou.sitecrawler.crawler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.shinkou.sitecrawler.crawler.crawler.AuthCrawler;

public class Starter
{
	private static Logger logger = LoggerFactory.getLogger(Starter.class);

	private static Map<String, String> getSettings()
		throws FileNotFoundException, IOException
	{
		Map<String, String> mapOut = new HashMap<>();

		String confpath = System.getProperty("conf.path");

		InputStream istream = null == confpath
			? Starter.class.getClassLoader()
				.getResourceAsStream("crawler.conf")
			: new FileInputStream(confpath);

		Properties props = new Properties();
		props.load(istream);

		for(String n: props.stringPropertyNames())
			mapOut.put(n, props.getProperty(n));

		return mapOut;
	}

	public static void main(String[] args)
	{
		try
		{
			AuthCrawler.startCrawlers
			(
				getSettings()
				, Arrays.asList(args)
				, Integer.parseInt(System.getProperty("instances", "1"))
			);
		}
		catch(FileNotFoundException e)
		{
			logger.error("Cannot retrieve configurations from file.", e);
		}
		catch(IOException e)
		{
			logger.error("Cannot retrieve configurations.", e);
		}
	}
}
