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

package infra.test.context.junit.jupiter;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.ApplicationContextInitializer;
import infra.core.annotation.AliasFor;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextLoader;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;

/**
 * {@code @ApplicationJUnitConfig} is a <em>composed annotation</em> that combines
 * {@link ExtendWith @ExtendWith(ApplicationExtension.class)} from JUnit Jupiter with
 * {@link ContextConfiguration @ContextConfiguration} from the <em>Infra TestContext
 * Framework</em>.
 *
 * @author Sam Brannen
 * @see ExtendWith
 * @see InfraExtension
 * @see ContextConfiguration
 * @see JUnitWebConfig
 * @since 4.0
 */
@Inherited
@Documented
@ContextConfiguration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(InfraExtension.class)
public @interface JUnitConfig {

  /**
   * Alias for {@link ContextConfiguration#classes}.
   */
  @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
  Class<?>[] value() default {};

  /**
   * Alias for {@link ContextConfiguration#classes}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  Class<?>[] classes() default {};

  /**
   * Alias for {@link ContextConfiguration#locations}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  String[] locations() default {};

  /**
   * Alias for {@link ContextConfiguration#initializers}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  Class<? extends ApplicationContextInitializer>[] initializers() default {};

  /**
   * Alias for {@link ContextConfiguration#inheritLocations}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  boolean inheritLocations() default true;

  /**
   * Alias for {@link ContextConfiguration#inheritInitializers}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  boolean inheritInitializers() default true;

  /**
   * Alias for {@link ContextConfiguration#loader}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  Class<? extends ContextLoader> loader() default ContextLoader.class;

  /**
   * Alias for {@link ContextConfiguration#name}.
   */
  @AliasFor(annotation = ContextConfiguration.class)
  String name() default "";

}
