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
package cn.taketoday.context.listener;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.utils.ClassUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2018-09-09 23:20
 */
@Slf4j
@ContextListener
@Order(Ordered.LOWEST_PRECEDENCE - Ordered.HIGHEST_PRECEDENCE)
public class ContextCloseListener implements ApplicationListener<ContextCloseEvent> {

	@Override
	public void onApplicationEvent(ContextCloseEvent event) {

		ApplicationContext applicationContext = event.getApplicationContext();
		log.info("Closing: [{}] at [{}]", applicationContext,
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(event.getTimestamp())));

		// environment
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		BeanDefinitionRegistry beanDefinitionRegistry = environment.getBeanDefinitionRegistry();

		try {

			for (String name : applicationContext.getBeanDefinitionsMap().keySet()) {
				applicationContext.destroyBean(name);
			}
			
		} finally {
			ClassUtils.clearCache();
			applicationContext.getSingletonsMap().clear();
			beanDefinitionRegistry.getBeanDefinitionsMap().clear();
		}
	}

}
