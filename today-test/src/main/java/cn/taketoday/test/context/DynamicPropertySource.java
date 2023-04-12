/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.test.annotation.DirtiesContext;

/**
 * Method-level annotation for integration tests that need to add properties with
 * dynamic values to the {@code Environment}'s set of {@code PropertySources}.
 *
 * <p>This annotation and its supporting infrastructure were originally designed
 * to allow properties from
 * <a href="https://www.testcontainers.org/">Testcontainers</a> based tests to be
 * exposed easily to Infra integration tests. However, this feature may also be
 * used with any form of external resource whose lifecycle is maintained outside
 * the test's {@code ApplicationContext}.
 *
 * <p>Methods annotated with {@code @DynamicPropertySource} must be {@code static}
 * and must have a single {@link DynamicPropertyRegistry} argument which is used
 * to add <em>name-value</em> pairs to the {@code Environment}'s set of
 * {@code PropertySources}. Values are dynamic and provided via a
 * {@link java.util.function.Supplier} which is only invoked when the property
 * is resolved. Typically, method references are used to supply values, as in the
 * example below.
 *
 * <p>dynamic properties from methods annotated with
 * {@code @DynamicPropertySource} will be <em>inherited</em> from enclosing test
 * classes, analogous to inheritance from superclasses and interfaces. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * <p><strong>NOTE</strong>: if you use {@code @DynamicPropertySource} in a base
 * class and discover that tests in subclasses fail because the dynamic properties
 * change between subclasses, you may need to annotate your base class with
 * {@link DirtiesContext @DirtiesContext} to
 * ensure that each subclass gets its own {@code ApplicationContext} with the
 * correct dynamic properties.
 *
 * <h3>Precedence</h3>
 * <p>Dynamic properties have higher precedence than those loaded from
 * {@link TestPropertySource @TestPropertySource}, the operating system's
 * environment, Java system properties, or property sources added by the
 * application declaratively by using
 * {@link cn.taketoday.context.annotation.PropertySource @PropertySource}
 * or programmatically. Thus, dynamic properties can be used to selectively
 * override properties loaded via {@code @TestPropertySource}, system property
 * sources, and application property sources.
 *
 * <h3>Example</h3>
 * <pre class="code">
 * &#064;ApplicationJUnitConfig(...)
 * &#064;Testcontainers
 * class ExampleIntegrationTests {
 *
 *     &#064;Container
 *     static RedisContainer redis = new RedisContainer();
 *
 *     // ...
 *
 *     &#064;DynamicPropertySource
 *     static void redisProperties(DynamicPropertyRegistry registry) {
 *         registry.add("redis.host", redis::getContainerIpAddress);
 *         registry.add("redis.port", redis::getMappedPort);
 *     }
 *
 * }</pre>
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see DynamicPropertyRegistry
 * @see ContextConfiguration
 * @see TestPropertySource
 * @see cn.taketoday.core.env.PropertySource
 * @see DirtiesContext
 * @since 4.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicPropertySource {
}
