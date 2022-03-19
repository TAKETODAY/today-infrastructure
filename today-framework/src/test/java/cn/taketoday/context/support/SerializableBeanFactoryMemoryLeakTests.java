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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

/**
 * Unit tests cornering SPR-7502.
 *
 * @author Chris Beams
 */
public class SerializableBeanFactoryMemoryLeakTests {

	/**
	 * Defensively zero-out static factory count - other tests
	 * may have misbehaved before us.
	 */
	@BeforeAll
	@AfterAll
	public static void zeroOutFactoryCount() throws Exception {
		getSerializableFactoryMap().clear();
	}

	@Test
	public void genericContext() throws Exception {
		assertFactoryCountThroughoutLifecycle(new GenericApplicationContext());
	}

	@Test
	public void abstractRefreshableContext() throws Exception {
		assertFactoryCountThroughoutLifecycle(new ClassPathXmlApplicationContext());
	}

	@Test
	public void genericContextWithMisconfiguredBean() throws Exception {
		GenericApplicationContext ctx = new GenericApplicationContext();
		registerMisconfiguredBeanDefinition(ctx);
		assertFactoryCountThroughoutLifecycle(ctx);
	}

	@Test
	public void abstractRefreshableContextWithMisconfiguredBean() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext() {
			@Override
			protected void customizeBeanFactory(StandardBeanFactory beanFactory) {
				super.customizeBeanFactory(beanFactory);
				registerMisconfiguredBeanDefinition(beanFactory);
			}
		};
		assertFactoryCountThroughoutLifecycle(ctx);
	}

	private void assertFactoryCountThroughoutLifecycle(ConfigurableApplicationContext ctx) throws Exception {
		assertThat(serializableFactoryCount()).isEqualTo(0);
		try {
			ctx.refresh();
			assertThat(serializableFactoryCount()).isEqualTo(1);
			ctx.close();
		}
		catch (BeanCreationException ex) {
			// ignore - this is expected on refresh() for failure case tests
		}
		finally {
			assertThat(serializableFactoryCount()).isEqualTo(0);
		}
	}

	private void registerMisconfiguredBeanDefinition(BeanDefinitionRegistry registry) {
		registry.registerBeanDefinition("misconfigured",
			rootBeanDefinition(Object.class).addPropertyValue("nonexistent", "bogus")
				.getBeanDefinition());
	}

	private int serializableFactoryCount() throws Exception {
		Map<?, ?> map = getSerializableFactoryMap();
		return map.size();
	}

	private static Map<?, ?> getSerializableFactoryMap() throws Exception {
		Field field = StandardBeanFactory.class.getDeclaredField("serializableFactories");
		field.setAccessible(true);
		return (Map<?, ?>) field.get(StandardBeanFactory.class);
	}

}
