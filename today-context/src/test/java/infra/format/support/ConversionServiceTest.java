/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.format.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.conversion.ConversionService;

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
public @interface ConversionServiceTest {

}
