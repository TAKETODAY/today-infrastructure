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

package cn.taketoday.framework.context.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.framework.ApplicationArguments;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.availability.AvailabilityChangeEvent;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.framework.context.event.ApplicationContextInitializedEvent;
import cn.taketoday.framework.context.event.ApplicationEnvironmentPreparedEvent;
import cn.taketoday.framework.context.event.ApplicationPreparedEvent;
import cn.taketoday.framework.context.event.ApplicationReadyEvent;
import cn.taketoday.framework.context.event.ApplicationStartedEvent;
import cn.taketoday.framework.context.event.ApplicationStartingEvent;
import cn.taketoday.framework.context.event.EventPublishingRunListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EventPublishingRunListener}
 *
 * @author Brian Clozel
 */
class EventPublishingRunListenerTests {

	private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	private Application application;

	private EventPublishingRunListener runListener;

	private TestApplicationListener eventListener;

	@BeforeEach
	void setup() {
		this.eventListener = new TestApplicationListener();
		this.application = mock(Application.class);
		given(this.application.getListeners()).willReturn(Collections.singleton(this.eventListener));
		this.runListener = new EventPublishingRunListener(this.application, null);
	}

	@Test
	void shouldPublishLifecycleEvents() {
		StaticApplicationContext context = new StaticApplicationContext();
		assertThat(this.eventListener.receivedEvents()).isEmpty();
		this.runListener.starting(this.bootstrapContext, getClass(), new ApplicationArguments());
		checkApplicationEvents(ApplicationStartingEvent.class);
		this.runListener.environmentPrepared(this.bootstrapContext, null);
		checkApplicationEvents(ApplicationEnvironmentPreparedEvent.class);
		this.runListener.contextPrepared(context);
		checkApplicationEvents(ApplicationContextInitializedEvent.class);
		this.runListener.contextLoaded(context);
		checkApplicationEvents(ApplicationPreparedEvent.class);
		context.refresh();
		this.runListener.started(context, null);
		checkApplicationEvents(ApplicationStartedEvent.class, AvailabilityChangeEvent.class);
		this.runListener.ready(context, null);
		checkApplicationEvents(ApplicationReadyEvent.class, AvailabilityChangeEvent.class);
	}

	void checkApplicationEvents(Class<?>... eventClasses) {
		assertThat(this.eventListener.receivedEvents()).extracting("class").contains((Object[]) eventClasses);
	}

	static class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

		private Deque<ApplicationEvent> events = new ArrayDeque<>();

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			this.events.add(event);
		}

		List<ApplicationEvent> receivedEvents() {
			List<ApplicationEvent> receivedEvents = new ArrayList<>();
			while (!this.events.isEmpty()) {
				receivedEvents.add(this.events.pollFirst());
			}
			return receivedEvents;
		}

	}

}
