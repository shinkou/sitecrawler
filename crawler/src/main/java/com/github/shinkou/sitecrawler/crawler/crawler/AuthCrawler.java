/*
 * Copyright (C) 2017  Chun-Kwong Wong
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.shinkou.sitecrawler.crawler.crawler;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.FormAuthInfo;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.github.shinkou.sitecrawler.crawler.fetcher.PageFetcher;

public class AuthCrawler extends WebCrawler
{
	private final static Pattern filters
		= Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp4|zip|gz))$");

	private static Pattern allowedUrlPattern;

	protected static Set<String> domains = new HashSet<>();

	private static AuthInfo getAuthInfo(Map<String, String> props)
		throws MalformedURLException
	{
		AuthInfo out = null;

		StringBuilder username = new StringBuilder();
		StringBuilder password = new StringBuilder();
		StringBuilder loginUrl = new StringBuilder();
		StringBuilder usernameFormStr = new StringBuilder();
		StringBuilder passwordFormStr = new StringBuilder();

		props.forEach
		((k, v) -> {
			switch(k)
			{
			// login username
			case "login.username":
				username.append(v);
				break;
			// login password
			case "login.password":
				password.append(v);
				break;
			// login URL where info is submitted to
			case "login.form.action":
				loginUrl.append(v);
				break;
			// name attribute of the username form field
			case "login.form.input.username.name":
				usernameFormStr.append(v);
				break;
			// name attribute of the password form field
			case "login.form.input.password.name":
				passwordFormStr.append(v);
				break;
			default:
			}
		});

		if
		(
			0 == username.length()
			|| 0 == password.length()
			|| 0 == loginUrl.length()
			|| 0 == usernameFormStr.length()
			|| 0 == passwordFormStr.length()
		) 
			return out;

		out = new FormAuthInfo
		(
			username.toString()
			, password.toString()
			, loginUrl.toString()
			, usernameFormStr.toString()
			, passwordFormStr.toString()
		);

		// FIXME:2017-11-17:shinkou:This needs to be fixed in crawler4j
		URL url = new URL(loginUrl.toString());
		out.setPort(url.getPort());

		return out;
	}

	/**
	 * Start crawling with crawlers
	 * @param props authentication properties
	 * @param urls URLs to crawl
	 * @param n number of crawlers
	 */
	public static void startCrawlers
	(
		Map<String, String> props
		, List<String> urls
		, int n
	)
	{
		try
		{
			// configurations
			CrawlConfig config = new CrawlConfig();
			config.setCrawlStorageFolder
			(
				props.getOrDefault("save.dir", "/tmp")
			);
			WebDriver wDriver = null;
			if
			(
				Boolean.parseBoolean
				(
					props.getOrDefault("webdriver.enabled", "false")
				)
			)
			{
				wDriver = new FirefoxDriver();
			}
			PageFetcher pageFetcher = new PageFetcher
			(
				config
				, getAuthInfo(props)
				, wDriver
				, props.get("webdriver.wait.url.pattern")
				, props.get("webdriver.wait.cssSelector")
				, Long.parseLong
				(
					props.getOrDefault("webdriver.wait.timeout", "10")
				)
				, props.get("login.page.url")
			);
			allowedUrlPattern = Pattern.compile
			(
				props.getOrDefault("allowed.url.pattern", "^.*$")
			);
			if (null != wDriver) wDriver.close();
			RobotstxtConfig txtConfig = new RobotstxtConfig();
			txtConfig.setEnabled(false);
			RobotstxtServer txtServer = new RobotstxtServer
			(
				txtConfig
				, pageFetcher
			);
			CrawlController controller = new CrawlController
			(
				config
				, pageFetcher
				, txtServer
			);

			// register domains and seeds
			WebURL wurl = new WebURL();
			urls.forEach
			(url -> {
				wurl.setURL(url);
				domains.add(wurl.getDomain());
				controller.addSeed(url);
			});

			// start crawling
			controller.start(AuthCrawler.class, n);
		}
		catch(Exception e)
		{
			logger.warn(e.getMessage(), e);
		}
	}

	@Override
	protected void onRedirectedStatusCode(Page page)
	{
		logger.info("Redirected to \"{}\".", page.getWebURL().getURL());

		if
		(
			null == page.getContentData()
			|| null == page.getContentEncoding()
		)
			return;

		try
		{
			logger.info
			(
				new String
				(
					page.getContentData()
					, page.getContentEncoding()
				)
			);
		}
		catch(Exception e)
		{
			logger.error("Cannot decode page content.", e);
		}
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url)
	{
		String href = url.getURL().toLowerCase();
		return domains.contains(url.getDomain())
			&& ! filters.matcher(href).matches()
			&& allowedUrlPattern.matcher(href).matches();
	}

	@Override
	public void visit(Page page)
	{
		logger.info("Visited page \"{}\".", page.getWebURL().toString());
	}
}
