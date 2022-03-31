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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.event.AfterTestClassEvent;
import cn.taketoday.test.context.event.AfterTestExecutionEvent;
import cn.taketoday.test.context.event.AfterTestMethodEvent;
import cn.taketoday.test.context.event.BeforeTestClassEvent;
import cn.taketoday.test.context.event.BeforeTestExecutionEvent;
import cn.taketoday.test.context.event.BeforeTestMethodEvent;
import cn.taketoday.test.context.event.EventPublishingTestExecutionListener;
import cn.taketoday.test.context.event.PrepareTestInstanceEvent;
import cn.taketoday.test.context.event.TestContextEvent;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EventPublishingTestExecutionListener}.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 4.0
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class EventPublishingTestExecutionListenerTests {

	private final EventPublishingTestExecutionListener listener = new EventPublishingTestExecutionListener();

	@Mock
	private TestContext testContext;

	@Mock
	private ApplicationContext applicationContext;

	@Captor
	private ArgumentCaptor<Function<TestContext, ? extends ApplicationEvent>> eventFactory;


	@BeforeEach
	void configureMock(TestInfo testInfo) {
		// Force Mockito to invoke the interface default method
		willCallRealMethod().given(testContext).publishEvent(any());
		given(testContext.getApplicationContext()).willReturn(applicationContext);
		// Only allow events to be published for test methods named "publish*".
		given(testContext.hasApplicationContext()).willReturn(testInfo.getTestMethod().get().getName().startsWith("publish"));
	}

	@Test
	void publishBeforeTestClassEvent() {
		assertEvent(BeforeTestClassEvent.class, listener::beforeTestClass);
	}

	@Test
	void publishPrepareTestInstanceEvent() {
		assertEvent(PrepareTestInstanceEvent.class, listener::prepareTestInstance);
	}

	@Test
	void publishBeforeTestMethodEvent() {
		assertEvent(BeforeTestMethodEvent.class, listener::beforeTestMethod);
	}

	@Test
	void publishBeforeTestExecutionEvent() {
		assertEvent(BeforeTestExecutionEvent.class, listener::beforeTestExecution);
	}

	@Test
	void publishAfterTestExecutionEvent() {
		assertEvent(AfterTestExecutionEvent.class, listener::afterTestExecution);
	}

	@Test
	void publishAfterTestMethodEvent() {
		assertEvent(AfterTestMethodEvent.class, listener::afterTestMethod);
	}

	@Test
	void publishAfterTestClassEvent() {
		assertEvent(AfterTestClassEvent.class, listener::afterTestClass);
	}

	@Test
	void doesNotPublishBeforeTestClassEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(BeforeTestClassEvent.class, listener::beforeTestClass);
	}

	@Test
	void doesNotPublishPrepareTestInstanceEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(PrepareTestInstanceEvent.class, listener::prepareTestInstance);
	}

	@Test
	void doesNotPublishBeforeTestMethodEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(BeforeTestMethodEvent.class, listener::beforeTestMethod);
	}

	@Test
	void doesNotPublishBeforeTestExecutionEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(BeforeTestExecutionEvent.class, listener::beforeTestExecution);
	}

	@Test
	void doesNotPublishAfterTestExecutionEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(AfterTestExecutionEvent.class, listener::afterTestExecution);
	}

	@Test
	void doesNotPublishAfterTestMethodEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(AfterTestMethodEvent.class, listener::afterTestMethod);
	}

	@Test
	void doesNotPublishAfterTestClassEventIfApplicationContextHasNotBeenLoaded() {
		assertNoEvent(AfterTestClassEvent.class, listener::afterTestClass);
	}

	private void assertEvent(Class<? extends TestContextEvent> eventClass, Consumer<TestContext> callback) {
		callback.accept(testContext);

		// The listener attempted to publish the event...
		verify(testContext, times(1)).publishEvent(eventFactory.capture());

		// The listener successfully published the event...
		verify(applicationContext, times(1)).publishEvent(any());

		// Verify the type of event that was published.
		ApplicationEvent event = eventFactory.getValue().apply(testContext);
		assertThat(event).isInstanceOf(eventClass);
		assertThat(event.getSource()).isEqualTo(testContext);
	}

	private void assertNoEvent(Class<? extends TestContextEvent> eventClass, Consumer<TestContext> callback) {
		callback.accept(testContext);

		// The listener attempted to publish the event...
		verify(testContext, times(1)).publishEvent(eventFactory.capture());

		// But the event was not actually published since the ApplicationContext
		// was not available.
		verify(applicationContext, never()).publishEvent(any());

		// In any case, we can still verify the type of event that would have
		// been published.
		ApplicationEvent event = eventFactory.getValue().apply(testContext);
		assertThat(event).isInstanceOf(eventClass);
		assertThat(event.getSource()).isEqualTo(testContext);
	}

}
