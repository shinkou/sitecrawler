package com.github.shinkou.sitecrawler.site.controllers;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TheController
{
	@Value("${application.name}")
	protected String m_appName;

	@Value("${application.version}")
	protected String m_appVersion;

	@Value("${application.build.timestamp}")
	protected String m_appBuildTimestamp;

	protected Map<String, String> m_info;

	@PostConstruct
	protected void init()
	{
		m_info = new HashMap<>();
		m_info.put("application.name", m_appName);
		m_info.put("application.version", m_appVersion);
		m_info.put("application.build.timestamp", m_appBuildTimestamp);
	}

	@RequestMapping("/info")
	@ResponseBody
	public Map<String, String> info()
	{
		return m_info;
	}

	@RequestMapping(value = {"/page/{title}"})
	public String page(@PathVariable String title, Model model)
	{
		model.addAttribute("title", title);
		return "page";
	}
}
