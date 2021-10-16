/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.beans.BeansException;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.style.ToStringBuilder;

/**
 * @author TODAY 2021/10/16 22:30
 * @since 4.0
 */
abstract class AbstractResolvableTypeObjectSupplier<T> implements ObjectSupplier<T> {

  protected final ResolvableType requiredType;
  protected final boolean includeNoneRegistered;
  protected final boolean includeNonSingletons;

  AbstractResolvableTypeObjectSupplier(ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    this.requiredType = requiredType;
    this.includeNoneRegistered = includeNoneRegistered;
    this.includeNonSingletons = includeNonSingletons;
  }

  @Override
  public T getIfAvailable() throws BeansException {
    return getIfAvailable(requiredType, includeNoneRegistered, includeNonSingletons);
  }

  abstract T getIfAvailable(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons);

  @Override
  public Class<?> getRequiredType() {
    return requiredType.resolve();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ResolvableTypeObjectSupplier))
      return false;
    ResolvableTypeObjectSupplier<?> that = (ResolvableTypeObjectSupplier<?>) o;
    return includeNoneRegistered == that.includeNoneRegistered
            && includeNonSingletons == that.includeNonSingletons
            && Objects.equals(requiredType, that.requiredType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requiredType, includeNoneRegistered, includeNonSingletons);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("requiredType", requiredType)
            .append("includeNonSingletons", includeNonSingletons)
            .append("includeNoneRegistered", includeNoneRegistered)
            .toString();
  }

}
