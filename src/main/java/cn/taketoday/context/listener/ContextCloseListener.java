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
package cn.taketoday.context.listener;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.DisposableBean;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2018-09-09 23:20
 */
@Slf4j
@ContextListener
@Order(Ordered.LOWEST_PRECEDENCE)
public class ContextCloseListener implements ApplicationListener<ContextCloseEvent> {

	@Override
	public void onApplicationEvent(ContextCloseEvent event) {

		ApplicationContext applicationContext = event.getApplicationContext();

		log.info("Closing: [{}] at [{}].", applicationContext,
				new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(event.getTimestamp())));

		BeanDefinitionRegistry beanDefinitionRegistry = applicationContext.getEnvironment().getBeanDefinitionRegistry();

		try {

			for (String name : beanDefinitionRegistry.getBeanDefinitionNames()) {
				Object bean = applicationContext.getSingleton(name);

				if (bean == null) {
					continue;
				}
				if (bean instanceof DisposableBean) {
					((DisposableBean) bean).destroy();
				}

				// PreDestroy
				Method[] declaredMethods = bean.getClass().getDeclaredMethods();
				for (Method method : declaredMethods) {
					if (method.isAnnotationPresent(PreDestroy.class)) {
						method.invoke(bean);
					}
				}
			}
		} //
		catch (Throwable ex) {
			log.error("Closing Context ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		} finally {
			beanDefinitionRegistry.getDependency().clear();
			applicationContext.getEnvironment().getProperties().clear();
			applicationContext.getSingletonsMap().clear();
			beanDefinitionRegistry.getBeanDefinitionsMap().clear();
		}
	}

}
