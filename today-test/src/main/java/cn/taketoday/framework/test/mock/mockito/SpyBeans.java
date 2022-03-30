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

package cn.taketoday.framework.test.mock.mockito;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several {@link SpyBean @SpyBean} annotations.
 * <p>
 * Can be used natively, declaring several nested {@link SpyBean @SpyBean} annotations.
 * Can also be used in conjunction with Java 8's support for <em>repeatable
 * annotations</em>, where {@link SpyBean @SpyBean} can simply be declared several times
 * on the same {@linkplain ElementType#TYPE type}, implicitly generating this container
 * annotation.
 *
 * @author Phillip Webb
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface SpyBeans {

  /**
   * Return the contained {@link SpyBean @SpyBean} annotations.
   *
   * @return the spy beans
   */
  SpyBean[] value();

}
