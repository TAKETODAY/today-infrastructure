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

package cn.taketoday.aop.aspectj;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for target selection matching (see SPR-3783).
 * <p>Thanks to Tomasz Blachowicz for the bug report!
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public class TargetPointcutSelectionTests {

	public TestInterface testImpl1;

	public TestInterface testImpl2;

	public TestAspect testAspectForTestImpl1;

	public TestAspect testAspectForAbstractTestImpl;

	public TestInterceptor testInterceptor;


	@BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testImpl1 = (TestInterface) ctx.getBean("testImpl1");
		testImpl2 = (TestInterface) ctx.getBean("testImpl2");
		testAspectForTestImpl1 = (TestAspect) ctx.getBean("testAspectForTestImpl1");
		testAspectForAbstractTestImpl = (TestAspect) ctx.getBean("testAspectForAbstractTestImpl");
		testInterceptor = (TestInterceptor) ctx.getBean("testInterceptor");

		testAspectForTestImpl1.count = 0;
		testAspectForAbstractTestImpl.count = 0;
		testInterceptor.count = 0;
	}


	@Test
	public void targetSelectionForMatchedType() {
		testImpl1.interfaceMethod();
		assertThat(testAspectForTestImpl1.count).as("Should have been advised by POJO advice for impl").isEqualTo(1);
		assertThat(testAspectForAbstractTestImpl.count).as("Should have been advised by POJO advice for base type").isEqualTo(1);
		assertThat(testInterceptor.count).as("Should have been advised by advisor").isEqualTo(1);
	}

	@Test
	public void targetNonSelectionForMismatchedType() {
		testImpl2.interfaceMethod();
		assertThat(testAspectForTestImpl1.count).as("Shouldn't have been advised by POJO advice for impl").isEqualTo(0);
		assertThat(testAspectForAbstractTestImpl.count).as("Should have been advised by POJO advice for base type").isEqualTo(1);
		assertThat(testInterceptor.count).as("Shouldn't have been advised by advisor").isEqualTo(0);
	}


	public static interface TestInterface {

		public void interfaceMethod();
	}


	// Reproducing bug requires that the class specified in target() pointcut doesn't
	// include the advised method's implementation (instead a base class should include it)
	public static abstract class AbstractTestImpl implements TestInterface {

		@Override
		public void interfaceMethod() {
		}
	}


	public static class TestImpl1 extends AbstractTestImpl {
	}


	public static class TestImpl2 extends AbstractTestImpl {
	}


	public static class TestAspect {

		public int count;

		public void increment() {
			count++;
		}
	}


	public static class TestInterceptor extends TestAspect implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation mi) throws Throwable {
			increment();
			return mi.proceed();
		}
	}

}
