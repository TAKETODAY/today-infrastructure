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

package cn.taketoday.beans.factory.support;

import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * Simple marker class for an individually autowired property value, to be added
 * to {@link BeanDefinition#getPropertyValues()} for a specific bean property.
 *
 * <p>At runtime, this will be replaced with a {@link DependencyDescriptor}
 * for the corresponding bean property's write method, eventually to be resolved
 * through a {@link AutowireCapableBeanFactory#resolveDependency} step.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AutowireCapableBeanFactory#resolveDependency
 * @see BeanDefinition#getPropertyValues()
 * @see BeanDefinitionBuilder#addAutowiredProperty
 * @since 4.0 2022/3/6 21:15
 */
public final class AutowiredPropertyMarker implements Serializable {

  /**
   * The canonical instance for the autowired marker value.
   */
  public static final Object INSTANCE = new AutowiredPropertyMarker();

  private AutowiredPropertyMarker() {
  }

  @Serial
  private Object readResolve() {
    return INSTANCE;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return (this == obj);
  }

  @Override
  public int hashCode() {
    return AutowiredPropertyMarker.class.hashCode();
  }

  @Override
  public String toString() {
    return "(autowired)";
  }

}
