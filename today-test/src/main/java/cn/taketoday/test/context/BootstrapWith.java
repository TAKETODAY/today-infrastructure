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

package cn.taketoday.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @BootstrapWith} defines class-level metadata that is used to determine
 * how to bootstrap the <em>TestContext Framework</em>.
 *
 * <p>This annotation may also be used as a <em>meta-annotation</em> to create
 * custom <em>composed annotations</em>. a locally
 * declared {@code @BootstrapWith} annotation (i.e., one that is <em>directly
 * present</em> on the current test class) will override any meta-present
 * declarations of {@code @BootstrapWith}.
 *
 * <p> this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * @author Sam Brannen
 * @see BootstrapContext
 * @see TestContextBootstrapper
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BootstrapWith {

  /**
   * The {@link TestContextBootstrapper} to use to bootstrap the <em>Infra
   * TestContext Framework</em>.
   */
  Class<? extends TestContextBootstrapper> value() default TestContextBootstrapper.class;

}
