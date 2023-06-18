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

package cn.taketoday.aot.hint;

/**
 * Gather hints that can be used to optimize the application runtime.
 *
 * <p>Use of reflection can be recorded for individual members of a type, as
 * well as broader {@linkplain MemberCategory member categories}. Access to
 * resources can be specified using patterns or the base name of a resource
 * bundle.
 *
 * <p>Hints that require the need for Java serialization of proxies can be
 * recorded as well.
 *
 * @author Stephane Nicoll
 * @author Janne Valkealahti
 * @since 4.0
 */
public class RuntimeHints {

  private final ReflectionHints reflection = new ReflectionHints();

  private final ResourceHints resources = new ResourceHints();

  private final SerializationHints serialization = new SerializationHints();

  private final ProxyHints proxies = new ProxyHints();

  private final ReflectionHints jni = new ReflectionHints();

  /**
   * Provide access to reflection-based hints.
   *
   * @return reflection hints
   */
  public ReflectionHints reflection() {
    return this.reflection;
  }

  /**
   * Provide access to resource-based hints.
   *
   * @return resource hints
   */
  public ResourceHints resources() {
    return this.resources;
  }

  /**
   * Provide access to serialization-based hints.
   *
   * @return serialization hints
   */
  public SerializationHints serialization() {
    return this.serialization;
  }

  /**
   * Provide access to proxy-based hints.
   *
   * @return proxy hints
   */
  public ProxyHints proxies() {
    return this.proxies;
  }

  /**
   * Provide access to jni-based hints.
   *
   * @return jni hints
   */
  public ReflectionHints jni() {
    return this.jni;
  }

}
