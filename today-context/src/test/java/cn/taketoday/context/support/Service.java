/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.support;

import cn.taketoday.beans.factory.BeanCreationNotAllowedException;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextAware;
import cn.taketoday.context.MessageSource;
import cn.taketoday.context.MessageSourceAware;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 */
public class Service implements ApplicationContextAware, MessageSourceAware, DisposableBean {

	private ApplicationContext applicationContext;

	private MessageSource messageSource;

	private Resource[] resources;

	private boolean properlyDestroyed = false;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		if (this.messageSource != null) {
			throw new IllegalArgumentException("MessageSource should not be set twice");
		}
		this.messageSource = messageSource;
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	public Resource[] getResources() {
		return resources;
	}


	@Override
	public void destroy() {
		this.properlyDestroyed = true;
		Thread thread = new Thread() {
			@Override
			public void run() {
				Assert.state(applicationContext.getBean("messageSource") instanceof StaticMessageSource,
						"Invalid MessageSource bean");
				try {
					applicationContext.getBean("service2");
					// Should have thrown BeanCreationNotAllowedException
					properlyDestroyed = false;
				}
				catch (BeanCreationNotAllowedException ex) {
					// expected
				}
			}
		};
		thread.start();
		try {
			thread.join();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	public boolean isProperlyDestroyed() {
		return properlyDestroyed;
	}

}
