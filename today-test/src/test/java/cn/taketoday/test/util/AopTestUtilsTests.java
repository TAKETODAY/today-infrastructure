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

package cn.taketoday.test.util;

import org.junit.jupiter.api.Test;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static cn.taketoday.test.util.AopTestUtils.getTargetObject;
import static cn.taketoday.test.util.AopTestUtils.getUltimateTargetObject;

/**
 * Unit tests for {@link AopTestUtils}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
class AopTestUtilsTests {

	private final FooImpl foo = new FooImpl();


	@Test
	void getTargetObjectForNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getTargetObject(null));
	}

	@Test
	void getTargetObjectForNonProxiedObject() {
		Foo target = getTargetObject(foo);
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getTargetObjectWrappedInSingleJdkDynamicProxy() {
		Foo target = getTargetObject(jdkProxy(foo));
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getTargetObjectWrappedInSingleCglibProxy() {
		Foo target = getTargetObject(cglibProxy(foo));
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getTargetObjectWrappedInDoubleJdkDynamicProxy() {
		Foo target = getTargetObject(jdkProxy(jdkProxy(foo)));
		assertThat(target).isNotSameAs(foo);
	}

	@Test
	void getTargetObjectWrappedInDoubleCglibProxy() {
		Foo target = getTargetObject(cglibProxy(cglibProxy(foo)));
		assertThat(target).isNotSameAs(foo);
	}

	@Test
	void getUltimateTargetObjectForNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getUltimateTargetObject(null));
	}

	@Test
	void getUltimateTargetObjectForNonProxiedObject() {
		Foo target = getUltimateTargetObject(foo);
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getUltimateTargetObjectWrappedInSingleJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(foo));
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getUltimateTargetObjectWrappedInSingleCglibProxy() {
		Foo target = getUltimateTargetObject(cglibProxy(foo));
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getUltimateTargetObjectWrappedInDoubleJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(jdkProxy(foo)));
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getUltimateTargetObjectWrappedInDoubleCglibProxy() {
		Foo target = getUltimateTargetObject(cglibProxy(cglibProxy(foo)));
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getUltimateTargetObjectWrappedInCglibProxyWrappedInJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(cglibProxy(foo)));
		assertThat(target).isSameAs(foo);
	}

	@Test
	void getUltimateTargetObjectWrappedInCglibProxyWrappedInDoubleJdkDynamicProxy() {
		Foo target = getUltimateTargetObject(jdkProxy(jdkProxy(cglibProxy(foo))));
		assertThat(target).isSameAs(foo);
	}

	private Foo jdkProxy(Foo foo) {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(foo);
		pf.addInterface(Foo.class);
		Foo proxy = (Foo) pf.getProxy();
		assertThat(AopUtils.isJdkDynamicProxy(proxy)).as("Proxy is a JDK dynamic proxy").isTrue();
		assertThat(proxy).isInstanceOf(Foo.class);
		return proxy;
	}

	private Foo cglibProxy(Foo foo) {
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(foo);
		pf.setProxyTargetClass(true);
		Foo proxy = (Foo) pf.getProxy();
		assertThat(AopUtils.isCglibProxy(proxy)).as("Proxy is a CGLIB proxy").isTrue();
		assertThat(proxy).isInstanceOf(FooImpl.class);
		return proxy;
	}


	static interface Foo {
	}

	static class FooImpl implements Foo {
	}

}
