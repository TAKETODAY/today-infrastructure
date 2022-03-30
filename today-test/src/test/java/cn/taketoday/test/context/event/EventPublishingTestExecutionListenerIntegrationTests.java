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

package cn.taketoday.test.context.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.aop.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.scheduling.annotation.AsyncConfigurer;
import cn.taketoday.scheduling.annotation.EnableAsync;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.stereotype.Component;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.event.AfterTestClassEvent;
import cn.taketoday.test.context.event.AfterTestExecutionEvent;
import cn.taketoday.test.context.event.AfterTestMethodEvent;
import cn.taketoday.test.context.event.BeforeTestClassEvent;
import cn.taketoday.test.context.event.BeforeTestExecutionEvent;
import cn.taketoday.test.context.event.BeforeTestMethodEvent;
import cn.taketoday.test.context.event.EventPublishingTestExecutionListener;
import cn.taketoday.test.context.event.PrepareTestInstanceEvent;
import cn.taketoday.test.context.event.TestContextEvent;
import cn.taketoday.test.context.event.annotation.AfterTestClass;
import cn.taketoday.test.context.event.annotation.AfterTestExecution;
import cn.taketoday.test.context.event.annotation.AfterTestMethod;
import cn.taketoday.test.context.event.annotation.BeforeTestClass;
import cn.taketoday.test.context.event.annotation.BeforeTestExecution;
import cn.taketoday.test.context.event.annotation.BeforeTestMethod;
import cn.taketoday.test.context.event.annotation.PrepareTestInstance;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.util.ReflectionUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link EventPublishingTestExecutionListener} and
 * accompanying {@link TestContextEvent} annotations.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 */
public class EventPublishingTestExecutionListenerIntegrationTests {

	private static final String THREAD_NAME_PREFIX = "Test-";

	private static final CountDownLatch countDownLatch = new CountDownLatch(1);

	private final TestContextManager testContextManager = new TestContextManager(ExampleTestCase.class);

	private final TestContext testContext = testContextManager.getTestContext();

	// Note that the following invocation of getApplicationContext() forces eager
	// loading of the test's ApplicationContext which consequently results in the
	// publication of all test execution events. Otherwise, TestContext#publishEvent
	// would never fire any events for ExampleTestCase.
	private final TestExecutionListener listener = testContext.getApplicationContext().getBean(TestExecutionListener.class);

	private final Object testInstance = new ExampleTestCase();

	private final Method traceableTestMethod = ReflectionUtils.findMethod(ExampleTestCase.class, "traceableTest");


	@AfterEach
	public void closeApplicationContext() {
		this.testContext.markApplicationContextDirty(null);
	}

	@Test
	public void beforeTestClassAnnotation() throws Exception {
		testContextManager.beforeTestClass();
		verify(listener, only()).beforeTestClass(testContext);
	}

	@Test
	public void prepareTestInstanceAnnotation() throws Exception {
		testContextManager.prepareTestInstance(testInstance);
		verify(listener, only()).prepareTestInstance(testContext);
	}

	@Test
	public void beforeTestMethodAnnotation() throws Exception {
		testContextManager.beforeTestMethod(testInstance, traceableTestMethod);
		verify(listener, only()).beforeTestMethod(testContext);
	}

	/**
	 * The {@code @BeforeTestMethod} condition in
	 * {@link TestEventListenerConfiguration#beforeTestMethod(BeforeTestMethodEvent)}
	 * only matches if the test method is annotated with {@code @Traceable}, and
	 * {@link ExampleTestCase#standardTest()} is not.
	 */
	@Test
	public void beforeTestMethodAnnotationWithFailingCondition() throws Exception {
		Method standardTest = ReflectionUtils.findMethod(ExampleTestCase.class, "standardTest");
		testContextManager.beforeTestMethod(testInstance, standardTest);
		verify(listener, never()).beforeTestMethod(testContext);
	}

	/**
	 * An exception thrown from an event listener executed in the current thread
	 * should fail the test method.
	 */
	@Test
	public void beforeTestMethodAnnotationWithFailingEventListener() throws Exception {
		Method method = ReflectionUtils.findMethod(ExampleTestCase.class, "testWithFailingEventListener");
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() ->
						testContextManager.beforeTestMethod(testInstance, method))
				.withMessageContaining("Boom!");
		verify(listener, only()).beforeTestMethod(testContext);
	}

	/**
	 * An exception thrown from an event listener that is executed asynchronously
	 * should not fail the test method.
	 */
	@Test
	public void beforeTestMethodAnnotationWithFailingAsyncEventListener() throws Exception {
		TrackingAsyncUncaughtExceptionHandler.asyncException = null;

		String methodName = "testWithFailingAsyncEventListener";
		Method method = ReflectionUtils.findMethod(ExampleTestCase.class, methodName);

		testContextManager.beforeTestMethod(testInstance, method);

		assertThat(countDownLatch.await(2, TimeUnit.SECONDS)).isTrue();

		verify(listener, only()).beforeTestMethod(testContext);
		assertThat(TrackingAsyncUncaughtExceptionHandler.asyncException.getMessage())
				.startsWith("Asynchronous exception for test method [" + methodName + "] in thread [" + THREAD_NAME_PREFIX);
	}

	@Test
	public void beforeTestExecutionAnnotation() throws Exception {
		testContextManager.beforeTestExecution(testInstance, traceableTestMethod);
		verify(listener, only()).beforeTestExecution(testContext);
	}

	@Test
	public void afterTestExecutionAnnotation() throws Exception {
		testContextManager.afterTestExecution(testInstance, traceableTestMethod, null);
		verify(listener, only()).afterTestExecution(testContext);
	}

	@Test
	public void afterTestMethodAnnotation() throws Exception {
		testContextManager.afterTestMethod(testInstance, traceableTestMethod, null);
		verify(listener, only()).afterTestMethod(testContext);
	}

	@Test
	public void afterTestClassAnnotation() throws Exception {
		testContextManager.afterTestClass();
		verify(listener, only()).afterTestClass(testContext);
	}


	@Target(METHOD)
	@Retention(RUNTIME)
	@interface Traceable {
	}

	@ExtendWith(ApplicationExtension.class)
	@ContextConfiguration(classes = TestEventListenerConfiguration.class)
	public static class ExampleTestCase {

		@Traceable
		@Test
		public void traceableTest() {
			/* no-op */
		}

		@Test
		public void standardTest() {
			/* no-op */
		}

		@Test
		public void testWithFailingEventListener() {
			/* no-op */
		}

		@Test
		public void testWithFailingAsyncEventListener() {
			/* no-op */
		}

	}

	@Configuration
	@EnableAsync(proxyTargetClass = true)
	static class TestEventListenerConfiguration implements AsyncConfigurer {

		@Override
		public Executor getAsyncExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
			executor.initialize();
			return executor;
		}

		@Override
		public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
			return new TrackingAsyncUncaughtExceptionHandler();
		}

		@Bean
		public TestExecutionListener listener() {
			return mock(TestExecutionListener.class);
		}

		/**
		 * The {@code @Async} test event listener method must reside in a separate
		 * component since {@code @Async} is not supported on methods in
		 * {@code @Configuration} classes.
		 */
		@Bean
		AsyncTestEventComponent asyncTestEventComponent() {
			return new AsyncTestEventComponent(listener());
		}

		@BeforeTestClass("#root.event.source.testClass.name matches '.+TestCase'")
		public void beforeTestClass(BeforeTestClassEvent e) throws Exception {
			listener().beforeTestClass(e.getSource());
		}

		@PrepareTestInstance("#a0.testContext.testClass.name matches '.+TestCase'")
		public void prepareTestInstance(PrepareTestInstanceEvent e) throws Exception {
			listener().prepareTestInstance(e.getSource());
		}

		@BeforeTestMethod("#p0.testContext.testMethod.isAnnotationPresent(T(cn.taketoday.test.context.event.EventPublishingTestExecutionListenerIntegrationTests.Traceable))")
		public void beforeTestMethod(BeforeTestMethodEvent e) throws Exception {
			listener().beforeTestMethod(e.getSource());
		}

		@BeforeTestMethod("event.testContext.testMethod.name == 'testWithFailingEventListener'")
		public void beforeTestMethodWithFailure(BeforeTestMethodEvent event) throws Exception {
			listener().beforeTestMethod(event.getSource());
			throw new RuntimeException("Boom!");
		}

		@BeforeTestExecution("'yes'")
		public void beforeTestExecution(BeforeTestExecutionEvent e) throws Exception {
			listener().beforeTestExecution(e.getSource());
		}

		@AfterTestExecution("'1'")
		public void afterTestExecution(AfterTestExecutionEvent e) throws Exception {
			listener().afterTestExecution(e.getSource());
		}

		@AfterTestMethod("event.testContext.testMethod.isAnnotationPresent(T(cn.taketoday.test.context.event.EventPublishingTestExecutionListenerIntegrationTests.Traceable))")
		public void afterTestMethod(AfterTestMethodEvent e) throws Exception {
			listener().afterTestMethod(e.getSource());
		}

		@AfterTestClass("#afterTestClassEvent.testContext.testClass.name matches '.+TestCase'")
		public void afterTestClass(AfterTestClassEvent afterTestClassEvent) throws Exception {
			listener().afterTestClass(afterTestClassEvent.getSource());
		}

	}

	/**
	 * MUST be annotated with {@code @Component} due to a change in Spring 5.1 that
	 * does not consider beans in a package starting with "cn.taketoday" to be
	 * event listeners unless they are also components.
	 *
	 * @see cn.taketoday.context.event.EventListenerMethodProcessor#isSpringContainerClass
	 */
	@Component
	static class AsyncTestEventComponent {

		final TestExecutionListener listener;


		AsyncTestEventComponent(TestExecutionListener listener) {
			this.listener = listener;
		}

		@BeforeTestMethod("event.testContext.testMethod.name == 'testWithFailingAsyncEventListener'")
		@Async
		public void beforeTestMethodWithAsyncFailure(BeforeTestMethodEvent event) throws Exception {
			this.listener.beforeTestMethod(event.getSource());
			throw new RuntimeException(String.format("Asynchronous exception for test method [%s] in thread [%s]",
					event.getTestContext().getTestMethod().getName(), Thread.currentThread().getName()));
		}

	}

	static class TrackingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

		static volatile Throwable asyncException;


		@Override
		public void handleUncaughtException(Throwable exception, Method method, Object... params) {
			asyncException = exception;
			countDownLatch.countDown();
		}

	}

}
