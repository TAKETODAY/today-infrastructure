/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop;

import java.io.Serial;
import java.io.Serializable;

/**
 * Canonical ClassFilter instance that matches all classes.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:13
 * @since 3.0
 */
final class TrueClassFilter implements ClassFilter, Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  public static final TrueClassFilter INSTANCE = new TrueClassFilter();

  /**
   * Enforce Singleton pattern.
   */
  private TrueClassFilter() { }

  @Override
  public boolean matches(Class<?> clazz) {
    return true;
  }

  /**
   * Required to support serialization. Replaces with canonical
   * instance on deserialization, protecting Singleton pattern.
   * Alternative to overriding {@code equals()}.
   */
  @Serial
  private Object readResolve() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "ClassFilter.TRUE";
  }

}
