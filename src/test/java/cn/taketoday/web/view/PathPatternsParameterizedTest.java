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

package cn.taketoday.web.view;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Annotation for tests parameterized to use either
 * {@link PathPatternParser} or
 * {@link PathMatcher} for URL pattern matching.
 *
 * @author Rossen Stoyanchev
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// Do not auto-close arguments since ConfigurableWebApplicationContext implements
// AutoCloseable and is shared between parameterized test invocations.
@org.junit.jupiter.params.ParameterizedTest(autoCloseArguments = false)
@org.junit.jupiter.params.provider.MethodSource("pathPatternsArguments")
public @interface PathPatternsParameterizedTest {
}
