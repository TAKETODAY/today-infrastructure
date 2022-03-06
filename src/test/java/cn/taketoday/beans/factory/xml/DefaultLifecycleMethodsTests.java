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

package cn.taketoday.beans.factory.xml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.io.ClassPathResource;

import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class DefaultLifecycleMethodsTests {

	private final StandardBeanFactory beanFactory = new StandardBeanFactory();


	@BeforeEach
	public void setup() throws Exception {
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("defaultLifecycleMethods.xml", getClass()));
	}


	@Test
	public void lifecycleMethodsInvoked() {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleAware");
		assertThat(bean.isInitCalled()).as("Bean not initialized").isTrue();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		assertThat(bean.isDestroyCalled()).as("Bean destroyed too early").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean not destroyed").isTrue();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
	}

	@Test
	public void lifecycleMethodsDisabled() throws Exception {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("lifecycleMethodsDisabled");
		assertThat(bean.isInitCalled()).as("Bean init method called incorrectly").isFalse();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean destroy method called incorrectly").isFalse();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
	}

	@Test
	public void ignoreDefaultLifecycleMethods() throws Exception {
		StandardBeanFactory bf = new StandardBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new ClassPathResource(
				"ignoreDefaultLifecycleMethods.xml", getClass()));
		bf.preInstantiateSingletons();
		bf.destroySingletons();
	}

	@Test
	public void overrideDefaultLifecycleMethods() throws Exception {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("overrideLifecycleMethods");
		assertThat(bean.isInitCalled()).as("Default init method called incorrectly").isFalse();
		assertThat(bean.isCustomInitCalled()).as("Custom init method not called").isTrue();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Default destroy method called incorrectly").isFalse();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method not called").isTrue();
	}

	@Test
	public void childWithDefaultLifecycleMethods() throws Exception {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("childWithDefaultLifecycleMethods");
		assertThat(bean.isInitCalled()).as("Bean not initialized").isTrue();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		assertThat(bean.isDestroyCalled()).as("Bean destroyed too early").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean not destroyed").isTrue();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
	}

	@Test
	public void childWithLifecycleMethodsDisabled() throws Exception {
		LifecycleAwareBean bean = (LifecycleAwareBean) this.beanFactory.getBean("childWithLifecycleMethodsDisabled");
		assertThat(bean.isInitCalled()).as("Bean init method called incorrectly").isFalse();
		assertThat(bean.isCustomInitCalled()).as("Custom init method called incorrectly").isFalse();
		this.beanFactory.destroySingletons();
		assertThat(bean.isDestroyCalled()).as("Bean destroy method called incorrectly").isFalse();
		assertThat(bean.isCustomDestroyCalled()).as("Custom destroy method called incorrectly").isFalse();
	}


	public static class LifecycleAwareBean {

		private boolean initCalled;

		private boolean destroyCalled;

		private boolean customInitCalled;

		private boolean customDestroyCalled;

		public void init() {
			this.initCalled = true;
		}

		public void destroy() {
			this.destroyCalled = true;
		}

		public void customInit() {
			this.customInitCalled = true;
		}

		public void customDestroy() {
			this.customDestroyCalled = true;
		}

		public boolean isInitCalled() {
			return initCalled;
		}

		public boolean isDestroyCalled() {
			return destroyCalled;
		}

		public boolean isCustomInitCalled() {
			return customInitCalled;
		}

		public boolean isCustomDestroyCalled() {
			return customDestroyCalled;
		}
	}

}
