/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.tools;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that registers a JUnit Jupiter extension for test classes or test
 * methods that need to use a forked classloader with compiled code.
 *
 * <p>The extension allows the compiler to define classes without polluting the
 * test {@link ClassLoader}.
 *
 * <p>NOTE: this annotation cannot be used in conjunction with
 * {@link org.junit.jupiter.api.TestTemplate @TestTemplate} methods.
 * Consequently, {@link org.junit.jupiter.api.RepeatedTest @RepeatedTest} and
 * {@link org.junit.jupiter.params.ParameterizedTest @ParameterizedTest} methods
 * are not supported.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ExtendWith(CompileWithForkedClassLoaderExtension.class)
public @interface CompileWithForkedClassLoader {
}
