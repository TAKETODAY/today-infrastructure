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

package cn.taketoday.test.context.junit.jupiter.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.event.ApplicationEvents;
import cn.taketoday.test.context.event.BeforeTestExecutionEvent;
import cn.taketoday.test.context.event.BeforeTestMethodEvent;
import cn.taketoday.test.context.event.PrepareTestInstanceEvent;
import cn.taketoday.test.context.event.RecordApplicationEvents;
import cn.taketoday.test.context.event.TestContextEvent;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the example {@link PublishedEvents} extension to the
 * {@link ApplicationEvents} feature.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@RecordApplicationEvents
@ExtendWith(PublishedEventsExtension.class)
class PublishedEventsIntegrationTests {

  @Test
  void test(PublishedEvents publishedEvents) {
    assertThat(publishedEvents).isNotNull();
    assertThat(publishedEvents.ofType(TestContextEvent.class)).hasSize(3);
    assertThat(publishedEvents.ofType(PrepareTestInstanceEvent.class)).hasSize(1);
    assertThat(publishedEvents.ofType(BeforeTestMethodEvent.class)).hasSize(1);
    assertThat(publishedEvents.ofType(BeforeTestExecutionEvent.class)).hasSize(1);
    assertThat(publishedEvents.ofType(TestContextEvent.class).ofSubType(BeforeTestExecutionEvent.class)).hasSize(1);
  }

  @Configuration
  static class Config {
  }

}
