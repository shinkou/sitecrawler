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
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

public class AuthCrawler extends WebCrawler
{
	private final static Pattern filters
		= Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp4|zip|gz))$");

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
			case "username":
				username.append(v);
				break;
			// login password
			case "password":
				password.append(v);
				break;
			// login URL where info is submitted to
			case "form.action":
				loginUrl.append(v);
				break;
			// name attribute of the username form field
			case "form.input.username.name":
				usernameFormStr.append(v);
				break;
			// name attribute of the password form field
			case "form.input.password.name":
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
				props.getOrDefault("dir.save", "/tmp")
			);
			AuthInfo authinfo = getAuthInfo(props);
			if (null != authinfo) config.addAuthInfo(authinfo);
			PageFetcher pageFetcher = new PageFetcher(config);
			RobotstxtConfig txtConfig = new RobotstxtConfig();
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
	public boolean shouldVisit(Page referringPage, WebURL url)
	{
		String href = url.getURL().toLowerCase();
		return domains.contains(url.getDomain())
			&& ! filters.matcher(href).matches();
	}

	@Override
	public void visit(Page page)
	{
		logger.info("Visited page \"{}\".", page.getWebURL().toString());
	}
}
