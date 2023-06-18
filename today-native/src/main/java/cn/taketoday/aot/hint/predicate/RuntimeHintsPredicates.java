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

package cn.taketoday.aot.hint.predicate;

import cn.taketoday.aot.hint.ProxyHints;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.SerializationHints;

/**
 * Static generator of predicates that test whether the given {@link RuntimeHints}
 * instance matches the expected behavior for reflection, resource, serialization,
 * or proxy generation.
 *
 * <p>This utility class can be used by {@link RuntimeHintsRegistrar} to conditionally
 * register hints depending on what's present already. This can also be used as a
 * testing utility for checking proper registration of hints:
 * <pre class="code">
 * Predicate&lt;RuntimeHints&gt; predicate = RuntimeHintsPredicates.reflection().onMethod(MyClass.class, "someMethod").invoke();
 * assertThat(predicate).accepts(runtimeHints);
 * </pre>
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0
 */
public abstract class RuntimeHintsPredicates {

  private static final ReflectionHintsPredicates reflection = new ReflectionHintsPredicates();

  private static final ResourceHintsPredicates resource = new ResourceHintsPredicates();

  private static final SerializationHintsPredicates serialization = new SerializationHintsPredicates();

  private static final ProxyHintsPredicates proxies = new ProxyHintsPredicates();

  private RuntimeHintsPredicates() {
  }

  /**
   * Return a predicate generator for {@link ReflectionHints reflection hints}.
   *
   * @return the predicate generator
   */
  public static ReflectionHintsPredicates reflection() {
    return reflection;
  }

  /**
   * Return a predicate generator for {@link ResourceHints resource hints}.
   *
   * @return the predicate generator
   */
  public static ResourceHintsPredicates resource() {
    return resource;
  }

  /**
   * Return a predicate generator for {@link SerializationHints serialization hints}.
   *
   * @return the predicate generator
   */
  public static SerializationHintsPredicates serialization() {
    return serialization;
  }

  /**
   * Return a predicate generator for {@link ProxyHints proxy hints}.
   *
   * @return the predicate generator
   */
  public static ProxyHintsPredicates proxies() {
    return proxies;
  }

}
