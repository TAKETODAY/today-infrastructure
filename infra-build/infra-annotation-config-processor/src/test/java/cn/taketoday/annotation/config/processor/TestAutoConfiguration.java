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

package cn.taketoday.annotation.config.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.annotation.AliasFor;

/**
 * Alternative to Infra App's {@code @AutoConfiguration} for testing (removes the need
 * for a dependency on the real annotation).
 *
 * @author Moritz Halbritter
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TestAutoConfigureBefore
@TestAutoConfigureAfter
public @interface TestAutoConfiguration {

	@AliasFor(annotation = TestAutoConfigureBefore.class, attribute = "value")
	Class<?>[] before() default {};

	@AliasFor(annotation = TestAutoConfigureBefore.class, attribute = "name")
	String[] beforeName() default {};

	@AliasFor(annotation = TestAutoConfigureAfter.class, attribute = "value")
	Class<?>[] after() default {};

	@AliasFor(annotation = TestAutoConfigureAfter.class, attribute = "name")
	String[] afterName() default {};

}
