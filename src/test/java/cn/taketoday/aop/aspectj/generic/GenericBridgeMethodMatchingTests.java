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

package cn.taketoday.aop.aspectj.generic;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AspectJ pointcut expression matching when working with bridge methods.
 *
 * <p>Depending on the caller's static type either the bridge method or the user-implemented method
 * gets called as the way into the proxy. Therefore, we need tests for calling a bean with
 * static type set to type with generic method and to type with specific non-generic implementation.
 *
 * <p>This class focuses on JDK proxy, while a subclass, GenericBridgeMethodMatchingClassProxyTests,
 * focuses on class proxying.
 *
 * See SPR-3556 for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class GenericBridgeMethodMatchingTests {

	protected DerivedInterface<String> testBean;

	protected GenericCounterAspect counterAspect;


	@SuppressWarnings("unchecked")
	@org.junit.jupiter.api.BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

		counterAspect = (GenericCounterAspect) ctx.getBean("counterAspect");
		counterAspect.count = 0;

		testBean = (DerivedInterface<String>) ctx.getBean("testBean");
	}


	@Test
	public void testGenericDerivedInterfaceMethodThroughInterface() {
		testBean.genericDerivedInterfaceMethod("");
		assertThat(counterAspect.count).isEqualTo(1);
	}

	@Test
	public void testGenericBaseInterfaceMethodThroughInterface() {
		testBean.genericBaseInterfaceMethod("");
		assertThat(counterAspect.count).isEqualTo(1);
	}

}


interface BaseInterface<T> {

	void genericBaseInterfaceMethod(T t);
}


interface DerivedInterface<T> extends BaseInterface<T> {

	public void genericDerivedInterfaceMethod(T t);
}


class DerivedStringParameterizedClass implements DerivedInterface<String> {

	@Override
	public void genericDerivedInterfaceMethod(String t) {
	}

	@Override
	public void genericBaseInterfaceMethod(String t) {
	}
}

@Aspect
class GenericCounterAspect {

	public int count;

	@Before("execution(* *..BaseInterface+.*(..))")
	public void increment() {
		count++;
	}

}

