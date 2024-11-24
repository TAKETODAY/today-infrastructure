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

package infra.test.context.env.repeatable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.test.context.TestPropertySource;

/**
 * Composed annotation that declares a property via
 * {@link TestPropertySource @TestPropertySource} used as a meta-meta-annotation.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MetaComposedTestProperty.MetaMetaInlinedTestProperty
@interface MetaComposedTestProperty {

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @TestPropertySource(properties = "enigma = meta meta")
  @interface MetaMetaInlinedTestProperty {
  }

}
