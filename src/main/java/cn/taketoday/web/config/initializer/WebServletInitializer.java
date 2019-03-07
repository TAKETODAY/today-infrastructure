/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.config.initializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;

import org.slf4j.LoggerFactory;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-02-03 12:28
 */
@Setter
@Getter
public class WebServletInitializer<T extends Servlet> extends WebComponentInitializer<ServletRegistration.Dynamic> {

	private T servlet;

	private int loadOnStartup = -1;

	private MultipartConfigElement multipartConfig;
	private ServletSecurityElement servletSecurity;

	public WebServletInitializer() {

	}

	public WebServletInitializer(T servlet) {
		this.servlet = servlet;
	}

	/**
	 * Returns the {@link MultipartConfigElement multi-part configuration} to be
	 * applied or {@code null}.
	 * 
	 * @return the multipart config
	 */
	public MultipartConfigElement getMultipartConfig() {
		return this.multipartConfig;
	}

	@Override
	protected ServletRegistration.Dynamic addRegistration(ServletContext servletContext) {

		final T servlet = getServlet();
		if (servlet != null) {
			return servletContext.addServlet(getName(), servlet);
		}
		return null;
	}

	/**
	 * Configure registration settings. Subclasses can override this method to
	 * perform additional configuration if required.
	 * 
	 * @param registration
	 *            the registration
	 */
	@Override
	protected void configureRegistration(ServletRegistration.Dynamic registration) {

		LoggerFactory.getLogger(WebServletInitializer.class).debug("Configure servlet registration: [{}]", this);

		super.configureRegistration(registration);

		String[] urlMappings = StringUtils.toStringArray(getUrlMappings());

		if (StringUtils.isArrayEmpty(urlMappings)) {
			urlMappings = Constant.DEFAULT_MAPPINGS;
		}

		registration.addMapping(urlMappings);
		registration.setLoadOnStartup(this.loadOnStartup);

		if (this.multipartConfig != null) {
			registration.setMultipartConfig(this.multipartConfig);
		}

		if (this.servletSecurity != null) {
			registration.setServletSecurity(servletSecurity);
		}
	}

	public T getServlet() {
		return servlet;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\n\t\"servlet\":\"").append(servlet)//
				.append("\",\n\t\"name\":\"").append(getName())//
				.append("\",\n\t\"loadOnStartup\":\"").append(loadOnStartup)//
				.append("\",\n\t\"multipartConfig\":\"").append(multipartConfig)//
				.append("\",\n\t\"servletSecurity\":\"").append(servletSecurity)//
				.append("\",\n\t\"initParameters\":\"").append(getInitParameters())//
				.append("\",\n\t\"order\":\"").append(getOrder())//
				.append("\",\n\t\"urlMappings\":\"").append(getUrlMappings())//
				.append("\",\n\t\"asyncSupported\":\"").append(isAsyncSupported())//
				.append("\"\n}").toString();
	}

}
