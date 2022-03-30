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

package cn.taketoday.framework.test.mock.mockito;

import cn.taketoday.util.ObjectUtils;

/**
 * Base class for {@link MockDefinition} and {@link SpyDefinition}.
 *
 * @author Phillip Webb
 * @see DefinitionsParser
 */
abstract class Definition {

  private static final int MULTIPLIER = 31;

  private final String name;

  private final MockReset reset;

  private final boolean proxyTargetAware;

  private final QualifierDefinition qualifier;

  Definition(String name, MockReset reset, boolean proxyTargetAware, QualifierDefinition qualifier) {
    this.name = name;
    this.reset = (reset != null) ? reset : MockReset.AFTER;
    this.proxyTargetAware = proxyTargetAware;
    this.qualifier = qualifier;
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
    boolean result = true;
    result = result && ObjectUtils.nullSafeEquals(this.name, other.name);
    result = result && ObjectUtils.nullSafeEquals(this.reset, other.reset);
    result = result && ObjectUtils.nullSafeEquals(this.proxyTargetAware, other.proxyTargetAware);
    result = result && ObjectUtils.nullSafeEquals(this.qualifier, other.qualifier);
    return result;
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
