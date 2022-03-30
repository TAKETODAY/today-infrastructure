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

package cn.taketoday.test.context.junit4.spr9051;

import org.junit.Test;
import org.junit.runner.RunWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.ApplicationJUnit4ClassRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This set of tests refutes the claims made in
 * <a href="https://jira.spring.io/browse/SPR-9051" target="_blank">SPR-9051</a>.
 *
 * <p><b>The Claims</b>:
 *
 * <blockquote>
 * When a {@code @ContextConfiguration} test class references a config class
 * missing an {@code @Configuration} annotation, {@code @Bean} dependencies are
 * wired successfully but the bean lifecycle is not applied (no init methods are
 * invoked, for example). Adding the missing {@code @Configuration} annotation
 * solves the problem, however the problem and solution isn't obvious since
 * wiring/injection appeared to work.
 * </blockquote>
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.0
 */
@RunWith(ApplicationJUnit4ClassRunner.class)
@ContextConfiguration(classes = AnnotatedConfigClassesWithoutAtConfigurationTests.AnnotatedFactoryBeans.class)
public class AnnotatedConfigClassesWithoutAtConfigurationTests {

	/**
	 * This is intentionally <b>not</b> annotated with {@code @Configuration}.
	 * Consequently, this class contains what we call <i>annotated factory bean
	 * methods</i> instead of standard bean definition methods.
	 */
	static class AnnotatedFactoryBeans {

		static final AtomicInteger enigmaCallCount = new AtomicInteger();


		@Bean
		public String enigma() {
			return "enigma #" + enigmaCallCount.incrementAndGet();
		}

		@Bean
		public LifecycleBean lifecycleBean() {
			// The following call to enigma() literally invokes the local
			// enigma() method, not a CGLIB proxied version, since these methods
			// are essentially factory bean methods.
			LifecycleBean bean = new LifecycleBean(enigma());
			assertThat(bean.isInitialized()).isFalse();
			return bean;
		}
	}


	@Autowired
	private String enigma;

	@Autowired
	private LifecycleBean lifecycleBean;


	@Test
	public void testSPR_9051() throws Exception {
		assertThat(enigma).isNotNull();
		assertThat(lifecycleBean).isNotNull();
		assertThat(lifecycleBean.isInitialized()).isTrue();
		Set<String> names = new HashSet<>();
		names.add(enigma.toString());
		names.add(lifecycleBean.getName());
		assertThat(new HashSet<>(Arrays.asList("enigma #1", "enigma #2"))).isEqualTo(names);
	}
}
