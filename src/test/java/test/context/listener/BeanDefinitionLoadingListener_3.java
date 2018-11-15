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
package test.context.listener;

import cn.taketoday.context.annotation.ContextListener;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.event.BeanDefinitionLoadingEvent;
import cn.taketoday.context.listener.ApplicationListener;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 * 
 *         2018-11-08 20:38
 */
@Slf4j
@Order(3)
@ContextListener
public class BeanDefinitionLoadingListener_3 implements ApplicationListener<BeanDefinitionLoadingEvent> {

	@Override
	public void onApplicationEvent(BeanDefinitionLoadingEvent event) {
		log.debug("BeanDefinitionLoadingListener_3");
	}

}
