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

package cn.taketoday.core.conversion.support.annotation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.conversion.ConversionService;

/**
 * Specialized {@link ParameterizedTest parameterized test} for
 * {@link ConversionService}-related testing. Test classes with methods annotated with
 * {@link ParameterizedTest @ParameterizedTest} must have a {@code static}
 * {@code conversionServices} method that is suitable for use as a {@link MethodSource
 * method source}.
 *
 * @author Andy Wilkinson
 */
@ParameterizedTest
@MethodSource("conversionServices")
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface ConversionServiceTest {

}
