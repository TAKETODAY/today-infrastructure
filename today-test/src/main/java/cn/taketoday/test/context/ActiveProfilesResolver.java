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

package cn.taketoday.test.context;

/**
 * Strategy interface for programmatically resolving which <em>active bean
 * definition profiles</em> should be used when loading an
 * {@link cn.taketoday.context.ApplicationContext ApplicationContext}
 * for a test class.
 *
 * <p>A custom {@code ActiveProfilesResolver} can be registered via the
 * {@link ActiveProfiles#resolver resolver} attribute of {@code @ActiveProfiles}.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * @author Sam Brannen
 * @author Michail Nikolaev
 * @see ActiveProfiles
 * @since 4.0
 */
@FunctionalInterface
public interface ActiveProfilesResolver {

  /**
   * Resolve the <em>bean definition profiles</em> to use when loading an
   * {@code ApplicationContext} for the given {@linkplain Class test class}.
   *
   * @param testClass the test class for which the profiles should be resolved;
   * never {@code null}
   * @return the list of bean definition profiles to use when loading the
   * {@code ApplicationContext}; never {@code null}
   * @see ActiveProfiles#resolver
   * @see ActiveProfiles#inheritProfiles
   */
  String[] resolve(Class<?> testClass);

}
