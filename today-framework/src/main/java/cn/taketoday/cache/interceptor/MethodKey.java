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

package cn.taketoday.cache.interceptor;

import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/23 13:45</a>
 * @since 4.0
 */
public final class MethodKey implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  private final int hash;
  public final transient Method targetMethod;
  public final Class<? extends Annotation> annotationClass;

  public MethodKey(Method targetMethod, Class<? extends Annotation> annotationClass) {
    this.targetMethod = targetMethod;
    this.hash = targetMethod.hashCode();
    this.annotationClass = annotationClass;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof MethodKey methodKey))
      return false;
    return hash == methodKey.hash
            && Objects.equals(targetMethod, methodKey.targetMethod)
            && Objects.equals(annotationClass, methodKey.annotationClass);
  }

  @Override
  public int hashCode() {
    return this.hash;
  }
}
