/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.event.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import infra.context.event.EventListener;
import infra.core.annotation.AliasFor;
import infra.context.annotation.Configuration;
import infra.test.context.TestExecutionListener;
import infra.test.context.TestExecutionListeners;
import infra.test.context.event.EventPublishingTestExecutionListener;
import infra.test.context.event.PrepareTestInstanceEvent;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link EventListener @EventListener} annotation used to consume a
 * {@link PrepareTestInstanceEvent} published by the
 * {@link EventPublishingTestExecutionListener
 * EventPublishingTestExecutionListener}.
 *
 * <p>This annotation may be used on {@code @EventListener}-compliant methods within
 * a Infra test {@link infra.context.ApplicationContext ApplicationContext}
 * &mdash; for example, on methods in a
 * {@link Configuration @Configuration}
 * class. A method annotated with this annotation will be invoked as part of the
 * {@link TestExecutionListener#prepareTestInstance}
 * lifecycle.
 *
 * <p>Event processing can optionally be made {@linkplain #value conditional} via
 * a SpEL expression &mdash; for example,
 * {@code @PrepareTestInstance("event.testContext.testClass.name matches '.+IntegrationTests'")}.
 *
 * <p>The {@code EventPublishingTestExecutionListener} must be registered in order
 * for this annotation to have an effect &mdash; for example, via
 * {@link TestExecutionListeners @TestExecutionListeners}.
 * Note, however, that the {@code EventPublishingTestExecutionListener} is registered
 * by default.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @see PrepareTestInstanceEvent
 * @since 4.0
 */
@Retention(RUNTIME)
@Target({ METHOD, ANNOTATION_TYPE })
@Documented
@EventListener(PrepareTestInstanceEvent.class)
public @interface PrepareTestInstance {

  /**
   * Alias for {@link EventListener#condition}.
   */
  @AliasFor(annotation = EventListener.class, attribute = "condition")
  String value() default "";

}
