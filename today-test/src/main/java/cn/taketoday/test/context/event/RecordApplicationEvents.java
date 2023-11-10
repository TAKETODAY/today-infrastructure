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

package cn.taketoday.test.context.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @RecordApplicationEvents} is a class-level annotation that is used to
 * instruct the <em>Infra TestContext Framework</em> to record all
 * {@linkplain cn.taketoday.context.ApplicationEvent application events}
 * that are published in the {@link cn.taketoday.context.ApplicationContext
 * ApplicationContext} during the execution of a single test, either from the
 * test thread or its descendants.
 *
 * <p>The recorded events can be accessed via the {@link ApplicationEvents} API
 * within your tests.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>This annotation will be inherited from an enclosing test class by default. See
 * {@link cn.taketoday.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Sam Brannen
 * @see ApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RecordApplicationEvents {
}
