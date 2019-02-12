/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright ©  TODAY & 2017 - 2019 All Rights Reserved.
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

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.servlet.ViewDispatcher;

import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author TODAY <br>
 *         2019-02-03 14:43
 */
@MissingBean
public class ViewDispatcherInitializer extends WebServletInitializer<ViewDispatcher> implements WebApplicationContextAware {

	private WebApplicationContext applicationContext;

	@Override
	public void setWebApplicationContext(WebApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public ViewDispatcher getServlet() {

		ViewDispatcher viewDispatcher = super.getServlet();
		if (viewDispatcher == null) {

			final Set<String> urls = ViewDispatcher.getMappings().keySet();

			if (urls.size() > 0) {// register
				ServletContext servletContext = getServletContext();

				if (!applicationContext.containsBeanDefinition(Constant.VIEW_DISPATCHER)) {
					applicationContext.registerBean(Constant.VIEW_DISPATCHER, ViewDispatcher.class);
				}
				viewDispatcher = applicationContext.getBean(Constant.VIEW_DISPATCHER, ViewDispatcher.class);

				final String contextPath = servletContext.getContextPath();

				setUrlMappings(urls.stream()//
						.map(ac -> ac.replaceFirst(contextPath, Constant.BLANK))//
						.collect(Collectors.toSet()));

				final Logger log = LoggerFactory.getLogger(ViewDispatcherInitializer.class);

				log.info("Register View Dispatcher Servlet: [{}] With Url Mappings: {}", viewDispatcher, getUrlMappings());

				setServletName(Constant.VIEW_DISPATCHER);
				setServlet(viewDispatcher);
			}
		}
		return viewDispatcher;
	}

	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE - 100;
	}
}
