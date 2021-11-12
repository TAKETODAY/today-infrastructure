/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.beans.factory;

import java.util.Objects;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.style.ToStringBuilder;

/**
 * @author TODAY 2021/10/16 22:30
 * @since 4.0
 */
abstract class AbstractResolvableTypeObjectSupplier<T> implements ObjectSupplier<T> {

  protected final ResolvableType requiredType;
  protected final boolean allowEagerInit;
  protected final boolean includeNonSingletons;

  AbstractResolvableTypeObjectSupplier(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    this.requiredType = requiredType;
    this.includeNonSingletons = includeNonSingletons;
    this.allowEagerInit = allowEagerInit;
  }

  @Override
  public T getIfAvailable() throws BeansException {
    return getIfAvailable(requiredType, includeNonSingletons, allowEagerInit);
  }

  abstract T getIfAvailable(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit);

  @Override
  public Class<?> getRequiredType() {
    return requiredType.resolve();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof AbstractResolvableTypeObjectSupplier<?> that))
      return false;
    return allowEagerInit == that.allowEagerInit
            && includeNonSingletons == that.includeNonSingletons
            && Objects.equals(requiredType, that.requiredType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requiredType, allowEagerInit, includeNonSingletons);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("requiredType", requiredType)
            .append("includeNonSingletons", includeNonSingletons)
            .append("allowEagerInit", allowEagerInit)
            .toString();
  }

}
