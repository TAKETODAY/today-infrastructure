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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.config.initializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.WebApplicationContextAware;
import cn.taketoday.web.servlet.ResourceServlet;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2019-05-15 23:59
 */
@Setter
@Getter
public class ResourceServletInitializer extends WebServletInitializer<ResourceServlet> implements WebApplicationContextAware {

	private WebApplicationContext applicationContext;

	private String resourceServletMapping = "/";

	@Override
	public void setWebApplicationContext(WebApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public ResourceServlet getServlet() {

		ResourceServlet resourceServlet = super.getServlet();
		if (resourceServlet == null) {

			addUrlMappings(StringUtils.split(resourceServletMapping));

			if (!applicationContext.containsBeanDefinition(Constant.RESOURCE_SERVLET)) {
				applicationContext.registerBean(Constant.RESOURCE_SERVLET, ResourceServlet.class);
			}
			resourceServlet = applicationContext.getBean(Constant.RESOURCE_SERVLET, ResourceServlet.class);
			final Logger log = LoggerFactory.getLogger(DispatcherServletInitializer.class);

			log.info("Register Resource Servlet: [{}] With Url Mappings: {}", resourceServlet, getUrlMappings());

			setName(Constant.RESOURCE_SERVLET);
			setServlet(resourceServlet);
		}
		return resourceServlet;
	}

	@Override
	public int getOrder() {
		return HIGHEST_PRECEDENCE - 101;
	}

}
