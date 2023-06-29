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

package cn.taketoday.test.context.aot;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.lang.Nullable;

/**
 * Holder for metadata specific to ahead-of-time (AOT) support in the <em>Infra
 * TestContext Framework</em>.
 *
 * <p>AOT test attributes are supported in two modes of operation: build-time
 * and run-time. At build time, test components can {@linkplain #setAttribute contribute}
 * attributes during the AOT processing phase. At run time, test components can
 * {@linkplain #getString(String) retrieve} attributes that were contributed at
 * build time. If {@link AotDetector#useGeneratedArtifacts()} returns {@code true},
 * run-time mode applies.
 *
 * <p>For example, if a test component computes something at build time that
 * cannot be computed at run time, the result of the build-time computation can
 * be stored as an AOT attribute and retrieved at run time without repeating the
 * computation.
 *
 * <p>An {@link AotContextLoader} would typically contribute an attribute in
 * {@link AotContextLoader#loadContextForAotProcessing loadContextForAotProcessing()};
 * whereas, an {@link AotTestExecutionListener} would typically contribute an attribute
 * in {@link AotTestExecutionListener#processAheadOfTime processAheadOfTime()}.
 * Any other test component &mdash; such as a
 * {@link cn.taketoday.test.context.TestContextBootstrapper TestContextBootstrapper}
 * &mdash; can choose to contribute an attribute at any point in time. Note that
 * contributing an attribute during standard JVM test execution will not have any
 * adverse side effect since AOT attributes will be ignored in that scenario. In
 * any case, you should use {@link AotDetector#useGeneratedArtifacts()} to determine
 * if invocations of {@link #setAttribute(String, String)} and
 * {@link #removeAttribute(String)} are permitted.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface AotTestAttributes {

  /**
   * Get the current instance of {@code AotTestAttributes} to use.
   * <p>See the class-level {@link AotTestAttributes Javadoc} for details on
   * the two supported modes.
   */
  static AotTestAttributes getInstance() {
    return new DefaultAotTestAttributes(AotTestAttributesFactory.getAttributes());
  }

  /**
   * Set a {@code String} attribute for later retrieval during AOT run-time execution.
   * <p>In general, users should take care to prevent overlaps with other
   * metadata attributes by using fully-qualified names, perhaps using a
   * class or package name as a prefix.
   *
   * @param name the unique attribute name
   * @param value the associated attribute value
   * @throws UnsupportedOperationException if invoked during
   * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
   * @throws IllegalArgumentException if the provided value is {@code null} or
   * if an attempt is made to override an existing attribute
   * @see #setAttribute(String, boolean)
   * @see #removeAttribute(String)
   * @see AotDetector#useGeneratedArtifacts()
   */
  void setAttribute(String name, String value);

  /**
   * Set a {@code boolean} attribute for later retrieval during AOT run-time execution.
   * <p>In general, users should take care to prevent overlaps with other
   * metadata attributes by using fully-qualified names, perhaps using a
   * class or package name as a prefix.
   *
   * @param name the unique attribute name
   * @param value the associated attribute value
   * @throws UnsupportedOperationException if invoked during
   * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
   * @throws IllegalArgumentException if an attempt is made to override an
   * existing attribute
   * @see #setAttribute(String, String)
   * @see #removeAttribute(String)
   * @see Boolean#toString(boolean)
   * @see AotDetector#useGeneratedArtifacts()
   */
  default void setAttribute(String name, boolean value) {
    setAttribute(name, Boolean.toString(value));
  }

  /**
   * Remove the attribute stored under the provided name.
   *
   * @param name the unique attribute name
   * @throws UnsupportedOperationException if invoked during
   * {@linkplain AotDetector#useGeneratedArtifacts() AOT run-time execution}
   * @see AotDetector#useGeneratedArtifacts()
   * @see #setAttribute(String, String)
   */
  void removeAttribute(String name);

  /**
   * Retrieve the attribute value for the given name as a {@link String}.
   *
   * @param name the unique attribute name
   * @return the associated attribute value, or {@code null} if not found
   * @see #getBoolean(String)
   * @see #setAttribute(String, String)
   */
  @Nullable
  String getString(String name);

  /**
   * Retrieve the attribute value for the given name as a {@code boolean}.
   *
   * @param name the unique attribute name
   * @return {@code true} if the attribute is set to "true" (ignoring case),
   * {@code} false otherwise
   * @see #getString(String)
   * @see #setAttribute(String, String)
   * @see Boolean#parseBoolean(String)
   */
  default boolean getBoolean(String name) {
    return Boolean.parseBoolean(getString(name));
  }

}
