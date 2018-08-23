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
package cn.taketoday.web.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.resolver.ParameterResolver;
import cn.taketoday.web.view.AbstractViewResolver;
import cn.taketoday.web.view.ViewResolver;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 *         2018-07-01 17:29:16 <br>
 *         2018-08-21 20:33 change
 */
@Slf4j
public abstract class AbstractHandler<T> implements DispatchHandler<T> {

	protected String				contextPath;

	protected WebApplicationContext	applicationContext;

	/** view **/
	protected ViewResolver			viewResolver;
	/** parameter **/
	protected ParameterResolver		parameterResolver;

	@Override
	public void doInit(WebApplicationContext applicationContext) throws ConfigurationException {

		this.applicationContext = applicationContext;
		try {

			viewResolver = applicationContext.getBean(Constant.VIEW_RESOLVER, AbstractViewResolver.class);
			parameterResolver = applicationContext.getBean(Constant.PARAMETER_RESOLVER, ParameterResolver.class);

			applicationContext.removeBean(Constant.VIEW_RESOLVER);
			applicationContext.removeBean(Constant.PARAMETER_RESOLVER);

		} catch (NoSuchBeanDefinitionException ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
		this.contextPath = applicationContext.getServletContext().getContextPath();
		viewResolver.initViewResolver(applicationContext);// view resolver init
		parameterResolver.doInit(applicationContext);// parameter resolver init -> scan extensions
	}

	/**
	 * Download file to client.
	 * 
	 * @param request
	 *            current request
	 * @param response
	 *            current response
	 * @param download
	 *            file
	 * @throws IOException
	 */
	protected void downloadFile(HttpServletRequest request, HttpServletResponse response, File download)
			throws IOException {

		response.setContentLengthLong(download.length());
		response.setHeader("Content-Transfer-Encoding", "binary");
		response.setHeader("Content-Type", "pplication/force-download;");
		response.setHeader("Content-Disposition",
				"attachment;filename=\"" + URLEncoder.encode(download.getName(), "UTF-8") + "\"");

		@Cleanup
		InputStream in = new FileInputStream(download.getAbsolutePath());
		@Cleanup
		OutputStream out = response.getOutputStream();
		byte[] b = new byte[4096];
		int len = 0;
		while ((len = in.read(b)) != -1) {
			out.write(b, 0, len);
		}
		out.flush();
		response.flushBuffer();
	}
	
}
