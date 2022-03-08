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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanReference;

/**
 * Base implementation of {@link ComponentDefinition} that provides a basic implementation of
 * {@link #getDescription} which delegates to {@link #getName}. Also provides a base implementation
 * of {@link #toString} which delegates to {@link #getDescription} in keeping with the recommended
 * implementation strategy. Also provides default implementations of {@link #getInnerBeanDefinitions}
 * and {@link #getBeanReferences} that return an empty array.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractComponentDefinition implements ComponentDefinition {

  /**
   * Delegates to {@link #getName}.
   */
  @Override
  public String getDescription() {
    return getName();
  }

  /**
   * Returns an empty array.
   */
  @Override
  public BeanDefinition[] getBeanDefinitions() {
    return new BeanDefinition[0];
  }

  /**
   * Returns an empty array.
   */
  @Override
  public BeanDefinition[] getInnerBeanDefinitions() {
    return new BeanDefinition[0];
  }

  /**
   * Returns an empty array.
   */
  @Override
  public BeanReference[] getBeanReferences() {
    return new BeanReference[0];
  }

  /**
   * Delegates to {@link #getDescription}.
   */
  @Override
  public String toString() {
    return getDescription();
  }

}
