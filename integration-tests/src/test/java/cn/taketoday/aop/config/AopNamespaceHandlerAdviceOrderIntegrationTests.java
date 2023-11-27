/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.aop.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for advice invocation order for advice configured via the
 * AOP namespace.
 *
 * @author Sam Brannen
 * @see cn.taketoday.aop.framework.autoproxy.AspectJAutoProxyAdviceOrderIntegrationTests
 */
class AopNamespaceHandlerAdviceOrderIntegrationTests {

	@Nested
	@JUnitConfig(locations = "AopNamespaceHandlerAdviceOrderIntegrationTests-afterFirst.xml")
	@DirtiesContext
	class AfterAdviceFirstTests {

		@Test
		void afterAdviceIsInvokedFirst(@Autowired Echo echo, @Autowired InvocationTrackingAspect aspect) throws Exception {
			assertThat(aspect.invocations).isEmpty();
			assertThat(echo.echo(42)).isEqualTo(42);
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after", "after returning");

			aspect.invocations.clear();
			assertThatExceptionOfType(Exception.class).isThrownBy(() -> echo.echo(new Exception()));
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after", "after throwing");
		}
	}

	@Nested
	@JUnitConfig(locations = "AopNamespaceHandlerAdviceOrderIntegrationTests-afterLast.xml")
	@DirtiesContext
	class AfterAdviceLastTests {

		@Test
		void afterAdviceIsInvokedLast(@Autowired Echo echo, @Autowired InvocationTrackingAspect aspect) throws Exception {
			assertThat(aspect.invocations).isEmpty();
			assertThat(echo.echo(42)).isEqualTo(42);
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after returning", "after");

			aspect.invocations.clear();
			assertThatExceptionOfType(Exception.class).isThrownBy(() -> echo.echo(new Exception()));
			assertThat(aspect.invocations).containsExactly("around - start", "before", "around - end", "after throwing", "after");
		}
	}


	static class Echo {

		Object echo(Object obj) throws Exception {
			if (obj instanceof Exception) {
				throw (Exception) obj;
			}
			return obj;
		}
	}

	static class InvocationTrackingAspect {

		List<String> invocations = new ArrayList<>();

		Object around(ProceedingJoinPoint joinPoint) throws Throwable {
			invocations.add("around - start");
			try {
				return joinPoint.proceed();
			}
			finally {
				invocations.add("around - end");
			}
		}

		void before() {
			invocations.add("before");
		}

		void afterReturning() {
			invocations.add("after returning");
		}

		void afterThrowing() {
			invocations.add("after throwing");
		}

		void after() {
			invocations.add("after");
		}
	}

}
