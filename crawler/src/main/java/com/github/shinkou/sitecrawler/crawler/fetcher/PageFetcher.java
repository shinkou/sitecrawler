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
package com.github.shinkou.sitecrawler.crawler.fetcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.BasicNameValuePair;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.BasicAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.FormAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.NtAuthInfo;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class PageFetcher extends edu.uci.ics.crawler4j.fetcher.PageFetcher
{
	protected WebDriver m_webDriver;
	protected String m_webDriverWaitUrlPattern;
	protected String m_webDriverWaitCssSel;
	protected long m_webDriverWaitTimeout;
	protected String m_preLoginUrl;

	protected HashMap<String, String> m_cookies;

	public PageFetcher
	(
		CrawlConfig config
		, AuthInfo authInfo
		, WebDriver webDriver
		, String webDriverWaitUrlPattern
		, String webDriverWaitCssSel
		, long webDriverWaitTimeout
		, String preLoginUrl
	)
	{
		super(config);

		m_webDriver = webDriver;
		m_webDriverWaitUrlPattern = webDriverWaitUrlPattern;
		m_webDriverWaitCssSel = webDriverWaitCssSel;
		m_webDriverWaitTimeout = webDriverWaitTimeout;
		m_preLoginUrl = preLoginUrl;

		m_cookies = new HashMap<>();

		connectionMonitorThread.suspend();

		if (authInfo != null)
		{
			config.addAuthInfo(authInfo);
			doAuthetication(config.getAuthInfos());
		}

		connectionMonitorThread.resume();
	}

	private void doAuthetication(List<AuthInfo> authInfos)
	{
		for(AuthInfo authInfo: authInfos)
		{
			if
			(
				authInfo.getAuthenticationType()
					== AuthInfo.AuthenticationType.BASIC_AUTHENTICATION
			)
			{
				doBasicLogin((BasicAuthInfo) authInfo);
			}
			else if
			(
				authInfo.getAuthenticationType()
					== AuthInfo.AuthenticationType.NT_AUTHENTICATION
			)
			{
				doNtLogin((NtAuthInfo) authInfo);
			}
			else
			{
				doFormLogin((FormAuthInfo) authInfo);
			}
		}
	}

	public PageFetcher(CrawlConfig config)
	{
		this(config, null, null, null, null, 0, null);
	}

	/**
	 * BASIC authentication<br/>
	 * Official Example: https://hc.apache
	 * .org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples
	 * /client/ClientAuthentication.java
	 * */
	private void doBasicLogin(BasicAuthInfo authInfo)
	{
		logger.info
		(
			"BASIC authentication for: " + authInfo.getLoginTarget()
		);

		HttpHost targetHost = new HttpHost
		(
			authInfo.getHost()
			, authInfo.getPort()
			, authInfo.getProtocol()
		);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials
		(
			new AuthScope(targetHost.getHostName(), targetHost.getPort())
			, new UsernamePasswordCredentials
			(
				authInfo.getUsername()
				, authInfo.getPassword()
			)
		);

		httpClient = HttpClients.custom()
			.setDefaultCredentialsProvider(credsProvider).build();
	}

	/**
	 * Do NT auth for Microsoft AD sites.
	 */
	private void doNtLogin(NtAuthInfo authInfo)
	{
		logger.info("NT authentication for: " + authInfo.getLoginTarget());

		HttpHost targetHost = new HttpHost
		(
			authInfo.getHost()
			, authInfo.getPort()
			, authInfo.getProtocol()
		);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();

		try
		{
			credsProvider.setCredentials
			(
				new AuthScope
				(
					targetHost.getHostName()
					, targetHost.getPort()
				)
				, new NTCredentials
				(
					authInfo.getUsername()
					, authInfo.getPassword()
					, InetAddress.getLocalHost().getHostName()
					, authInfo.getDomain()
				)
			);
		}
		catch(UnknownHostException e)
		{
			logger.error("Error creating NT credentials", e);
		}

		httpClient = HttpClients.custom()
			.setDefaultCredentialsProvider(credsProvider).build();
	}

	/**
	 * FORM authentication<br/>
	 * Official Example:
	 *	https://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http
	 *	/examples/client/ClientFormLogin.java
	 */
	private void doFormLogin(FormAuthInfo authInfo)
	{
		logger.info
		(
			"FORM authentication for: " + authInfo.getLoginTarget()
		);

		if (null == m_webDriver)
			doNormalFormLogin(authInfo);
		else
			doWebdriverFormLogin(authInfo);

		// add obtained cookies
		if (null != m_cookies && ! m_cookies.isEmpty())
		{
			String s = String.join
			(
				"; "
				, m_cookies.entrySet().stream().map
				(
					e -> e.getKey() + "=" + e.getValue()
				)
				.collect(Collectors.toList())
			);

			Set<Header> hdrs = new HashSet<>(config.getDefaultHeaders());
			hdrs.add(new BasicHeader("Cookie", s));

			config.setDefaultHeaders(hdrs);
		}
	}

	private void followRedirect(HttpClient httpClient, HttpResponse res)
		throws IOException
	{
		logger.debug("Status Line: {}", res.getStatusLine().toString());

		Arrays.stream(res.getHeaders("Set-Cookie")).forEach
		(hdr -> {
			NameValuePair nvp = BasicHeaderValueParser.parseNameValuePair
			(
				hdr.getValue()
				, BasicHeaderValueParser.INSTANCE
			);
			m_cookies.put(nvp.getName(), nvp.getValue());
		});

		if
		(
			3 != res.getStatusLine().getStatusCode() / 100
			|| 0 == res.getHeaders("Location").length
		)
			return;

		// visit all redirected locations
		Header hdr = res.getFirstHeader("Location");

		String postLoginUrl = hdr.getValue();

		logger.info("Redirecting to location: {}", postLoginUrl);

		followRedirect
		(
			httpClient
			, httpClient.execute(new HttpGet(postLoginUrl))
		);
	}

	private void doNormalFormLogin(FormAuthInfo authInfo)
	{
		try
		{
			// visit page containing the login form to obtain cookies
			if (null != m_preLoginUrl && 0 < m_preLoginUrl.length())
			{
				HttpResponse httpres
					= httpClient.execute(new HttpGet(m_preLoginUrl));

				followRedirect(httpClient, httpres);
			}

			// login
			String fullUri = authInfo.getProtocol() + "://"
				+ authInfo.getHost() + ":" + authInfo.getPort()
				+ authInfo.getLoginTarget();
			HttpPost httpPost = new HttpPost(fullUri);
			List<NameValuePair> formParams = new ArrayList<>();
			formParams.add
			(
				new BasicNameValuePair
				(
					authInfo.getUsernameFormStr()
					, authInfo.getUsername()
				)
			);
			formParams.add
			(
				new BasicNameValuePair
				(
					authInfo.getPasswordFormStr()
					, authInfo.getPassword()
				)
			);
			UrlEncodedFormEntity entity
				= new UrlEncodedFormEntity(formParams, "UTF-8");
			httpPost.setEntity(entity);
			CloseableHttpResponse res = httpClient.execute(httpPost);

			logger.debug
			(
				"Successfully Logged in with user: "
					+ authInfo.getUsername() + " to: " + authInfo.getHost()
			);

			// post login redirect(s)
			followRedirect(httpClient, res);
		}
		catch(UnsupportedEncodingException e)
		{
			logger.error
			(
				"Encountered a non supported encoding while trying to"
					+ " login to: " + authInfo.getHost()
				, e
			);
		}
		catch(ClientProtocolException e)
		{
			logger.error
			(
				"While trying to login to: " + authInfo.getHost()
					+ " - Client protocol not supported"
				, e
			);
		}
		catch(IOException e)
		{
			logger.error
			(
				"While trying to login to: " + authInfo.getHost()
					+ " - Error making request"
				, e
			);
		}
	}

	private void doWebdriverFormLogin(FormAuthInfo authInfo)
	{
		// visit page containing the login form to obtain cookies
		if (null != m_preLoginUrl && 0 < m_preLoginUrl.length())
			m_webDriver.get(m_preLoginUrl);

		// login
		JavascriptExecutor je = (JavascriptExecutor) m_webDriver;
		je.executeScript
		(
			"document.querySelector(\"form input[name='"
				+ authInfo.getUsernameFormStr() + "']\").value = \""
				+ authInfo.getUsername() + "\";"
				+ "document.querySelector(\"form input[name='"
				+ authInfo.getPasswordFormStr() + "']\").value = \""
				+ authInfo.getPassword() + "\";"
				+ "document.querySelector(\"form\").submit();"
		);
		m_webDriver.manage().getCookies()
			.forEach(c -> m_cookies.put(c.getName(), c.getValue()));

		// post login wait
		if
		(
			0 < m_webDriverWaitTimeout
			&& (
				null != m_webDriverWaitUrlPattern
				|| null != m_webDriverWaitCssSel
			)
		)
		{
			WebDriverWait wdWait = new WebDriverWait
			(
				m_webDriver
				, m_webDriverWaitTimeout
			);
			if (null != m_webDriverWaitUrlPattern)
			{
				wdWait.until
				(
					ExpectedConditions.urlMatches
					(
						m_webDriverWaitUrlPattern
					)
				);
			}
			if (null != m_webDriverWaitCssSel)
			{
				wdWait.until
				(
					ExpectedConditions.visibilityOfElementLocated
					(
						By.cssSelector
						(
							m_webDriverWaitCssSel
						)
					)
				);
			}
		}

		m_webDriver.manage().getCookies()
			.forEach(c -> m_cookies.put(c.getName(), c.getValue()));
	}

	/**
	 * get cookies obtained after authentication
	 * @return cookies
	 */
	public Map<String, String> getCookies()
	{
		return m_cookies;
	}

	@Override
	protected HttpUriRequest newHttpUriRequest(String url)
	{
		HttpUriRequest out = new HttpGet(url);
		config.getDefaultHeaders().forEach
		(
			hdr -> out.setHeader(hdr.getName(), hdr.getValue())
		);
		return out;
	}
}
