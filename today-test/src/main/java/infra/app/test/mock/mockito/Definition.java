/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.mock.mockito;

import infra.util.ObjectUtils;

/**
 * Base class for {@link MockDefinition} and {@link SpyDefinition}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefinitionsParser
 * @since 4.0
 */
abstract class Definition {

  private static final int MULTIPLIER = 31;

  private final String name;

  private final MockReset reset;

  private final boolean proxyTargetAware;

  private final QualifierDefinition qualifier;

  Definition(String name, MockReset reset, boolean proxyTargetAware, QualifierDefinition qualifier) {
    this.name = name;
    this.qualifier = qualifier;
    this.proxyTargetAware = proxyTargetAware;
    this.reset = (reset != null) ? reset : MockReset.AFTER;
  }

  /**
   * Return the name for bean.
   *
   * @return the name or {@code null}
   */
  String getName() {
    return this.name;
  }

  /**
   * Return the mock reset mode.
   *
   * @return the reset mode
   */
  MockReset getReset() {
    return this.reset;
  }

  /**
   * Return if AOP advised beans should be proxy target aware.
   *
   * @return if proxy target aware
   */
  boolean isProxyTargetAware() {
    return this.proxyTargetAware;
  }

  /**
   * Return the qualifier or {@code null}.
   *
   * @return the qualifier
   */
  QualifierDefinition getQualifier() {
    return this.qualifier;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
      return false;
    }
    Definition other = (Definition) obj;
    return ObjectUtils.nullSafeEquals(this.name, other.name)
            && ObjectUtils.nullSafeEquals(this.proxyTargetAware, other.proxyTargetAware)
            && ObjectUtils.nullSafeEquals(this.reset, other.reset)
            && ObjectUtils.nullSafeEquals(this.qualifier, other.qualifier);
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.name);
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.reset);
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.proxyTargetAware);
    result = MULTIPLIER * result + ObjectUtils.nullSafeHashCode(this.qualifier);
    return result;
  }

}
