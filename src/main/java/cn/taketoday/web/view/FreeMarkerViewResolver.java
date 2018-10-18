/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
package cn.taketoday.web.view;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.web.WebApplicationContext;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-06-26 19:16:46
 */
@Slf4j
public class FreeMarkerViewResolver extends AbstractViewResolver {

	private ObjectWrapper wrapper;
	@Getter
	private Configuration configuration;
	private TaglibFactory taglibFactory;

	@Props(prefix = "freemarker.", replace = true)
	private Properties props;

	public static final String KEY_REQUEST = "Request";
	public static final String KEY_SESSION = "Session";
	public static final String KEY_JSP_TAGLIBS = "JspTaglibs";
	public static final String KEY_APPLICATION = "Application";
	public static final String KEY_REQUEST_PARAMETERS = "RequestParameters";

	@Override
	public void initViewResolver(WebApplicationContext applicationContext) throws ConfigurationException {

		this.taglibFactory = new TaglibFactory(servletContext);
		this.configuration = new Configuration(Configuration.VERSION_2_3_28);
		this.wrapper = new DefaultObjectWrapper(Configuration.VERSION_2_3_28);

		configuration.setLocale(locale);
		configuration.setObjectWrapper(wrapper);
		configuration.setDefaultEncoding(encoding);
		configuration.setServletContextForTemplateLoading(servletContext, prefix); // prefix -> /WEB-INF/..

		try {

			configuration.setSettings(props);
		} catch (TemplateException e) {
			throw new ConfigurationException("Set FreeMarker's Properties Error.");
		}

		log.info("Configuration FreeMarker View Resolver Success.");
	}

	public FreeMarkerViewResolver() {

	}

	/**
	 * create Model Attributes.
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws TemplateModelException
	 */
	private final TemplateHashModel createModel(HttpServletRequest request, HttpServletResponse response)
			throws TemplateModelException {

		return new AllHttpScopesHashModel(wrapper, servletContext, request) {
			private static final long serialVersionUID = -0;
			{
				// Create hash model wrapper for servlet context (the application)
				putUnlistedModel(KEY_APPLICATION, new ServletContextHashModel(servletContext, wrapper));
				putUnlistedModel(KEY_JSP_TAGLIBS, FreeMarkerViewResolver.this.taglibFactory);
				// Create hash model wrapper for session
				putUnlistedModel(KEY_SESSION, new HttpSessionHashModel(request.getSession(), wrapper));
				// Create hash model wrapper for request
				putUnlistedModel(KEY_REQUEST, new HttpRequestHashModel(request, response, wrapper));
				putUnlistedModel(KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
			}
		};
	}

	/**
	 * Resolve FreeMarker View.
	 */
	@Override
	public void resolveView(String templateName, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		configuration.getTemplate(templateName + suffix)//
				.process(createModel(request, response), response.getWriter());
	}

}
