/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.config;

import java.util.Set;

import javax.servlet.ServletContext;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Today
 * @date 2018年6月24日 下午10:06:27
 */
@Setter
@Getter
public final class ConfigurationFactory {

	
	private Set<Class<?>>	actions;

	private String			location			= System.getProperty("java.io.tmpdir");
	private long			maxFileSize			= 102400000;			// every single request
	private long			maxRequestSize		= 102400000;			// total size
	private int				fileSizeThreshold	= 1000000000;			// cache

	private String			prefix				= "/WEB-INF";
	private String			suffix				= ".jsp";
	private String			encoding			= "UTF-8";
	public String			contextPath			= "";
	private ServletContext	servletContext;

	private WebConfig		viewConfig			= ViewConfig.create();
	private WebConfig		actionConfig		= ActionConfig.create();

	private final static ConfigurationFactory configurationFactory = new ConfigurationFactory();

	private String[]		defaultUrlPatterns	= { "*.gif", "*.jpg", "*.jpeg", "*.png", "*.swf", "*.js", "*.css",
			"*.ico", "*.rar", "*.zip", "*.txt", "*.flv", "*.mid", "*.doc", "*.ppt", "*.pdf", "*.xls", "*.mp3", "*.wma",
			"*.map", "*.woff2", "*.woff", "*.docx" };

	
	/**
	 * @return
	 */
	public static final ConfigurationFactory createFactory() {
		return configurationFactory;
	}

	private ConfigurationFactory() {

	}

	public WebConfig createViewConfig() {
		return viewConfig;
	}

	public WebConfig createActionConfig() {
		return actionConfig;
	}

	public String contextPath() {
		return contextPath;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setServletContext(ServletContext servletContext) {
		this.contextPath = servletContext.getContextPath(); // contextPath
		this.servletContext = servletContext;
	}

	public final String getContextPath() {
		return contextPath;
	}

	public final String[] getDefaultUrlPatterns() {
		return defaultUrlPatterns;
	}


}
