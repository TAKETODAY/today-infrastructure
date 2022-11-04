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

package cn.taketoday.transaction.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests proving that regardless the proxy strategy used (JDK interface-based vs. CGLIB
 * subclass-based), discovery of advice-oriented annotations is consistent.
 *
 * For example, Infra @Transactional may be declared at the interface or class level,
 * and whether interface or subclass proxies are used, the @Transactional annotation must
 * be discovered in a consistent fashion.
 *
 * @author Chris Beams
 */
@SuppressWarnings("resource")
class ProxyAnnotationDiscoveryTests {

	@Test
	void annotatedServiceWithoutInterface_PTC_true() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCTrue.class, AnnotatedServiceWithoutInterface.class);
		ctx.refresh();
		AnnotatedServiceWithoutInterface s = ctx.getBean(AnnotatedServiceWithoutInterface.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(AnnotatedServiceWithoutInterface.class);
	}

	@Test
	void annotatedServiceWithoutInterface_PTC_false() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCFalse.class, AnnotatedServiceWithoutInterface.class);
		ctx.refresh();
		AnnotatedServiceWithoutInterface s = ctx.getBean(AnnotatedServiceWithoutInterface.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(AnnotatedServiceWithoutInterface.class);
	}

	@Test
	void nonAnnotatedService_PTC_true() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCTrue.class, AnnotatedServiceImpl.class);
		ctx.refresh();
		NonAnnotatedService s = ctx.getBean(NonAnnotatedService.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(AnnotatedServiceImpl.class);
	}

	@Test
	void nonAnnotatedService_PTC_false() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCFalse.class, AnnotatedServiceImpl.class);
		ctx.refresh();
		NonAnnotatedService s = ctx.getBean(NonAnnotatedService.class);
		assertThat(AopUtils.isJdkDynamicProxy(s)).isTrue();
		assertThat(s).isNotInstanceOf(AnnotatedServiceImpl.class);
	}

	@Test
	void annotatedService_PTC_true() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCTrue.class, NonAnnotatedServiceImpl.class);
		ctx.refresh();
		AnnotatedService s = ctx.getBean(AnnotatedService.class);
		assertThat(AopUtils.isCglibProxy(s)).isTrue();
		assertThat(s).isInstanceOf(NonAnnotatedServiceImpl.class);
	}

	@Test
	void annotatedService_PTC_false() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(PTCFalse.class, NonAnnotatedServiceImpl.class);
		ctx.refresh();
		AnnotatedService s = ctx.getBean(AnnotatedService.class);
		assertThat(AopUtils.isJdkDynamicProxy(s)).isTrue();
		assertThat(s).isNotInstanceOf(NonAnnotatedServiceImpl.class);
	}
}

@Configuration
@EnableTransactionManagement(proxyTargetClass=false)
class PTCFalse { }

@Configuration
@EnableTransactionManagement(proxyTargetClass=true)
class PTCTrue { }

interface NonAnnotatedService {
	void m();
}

interface AnnotatedService {
	@Transactional void m();
}

class NonAnnotatedServiceImpl implements AnnotatedService {
	@Override
	public void m() { }
}

class AnnotatedServiceImpl implements NonAnnotatedService {
	@Override
	@Transactional public void m() { }
}

class AnnotatedServiceWithoutInterface {
	@Transactional public void m() { }
}
