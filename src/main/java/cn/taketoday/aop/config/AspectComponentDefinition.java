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

package cn.taketoday.aop.config;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanReference;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.beans.factory.parsing.ComponentDefinition}
 * that holds an aspect definition, including its nested pointcuts.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #getNestedComponents()
 * @see PointcutComponentDefinition
 * @since 4.0
 */
public class AspectComponentDefinition extends CompositeComponentDefinition {

  private final BeanDefinition[] beanDefinitions;

  private final BeanReference[] beanReferences;

  public AspectComponentDefinition(String aspectName, @Nullable BeanDefinition[] beanDefinitions,
          @Nullable BeanReference[] beanReferences, @Nullable Object source) {

    super(aspectName, source);
    this.beanDefinitions = (beanDefinitions != null ? beanDefinitions : new BeanDefinition[0]);
    this.beanReferences = (beanReferences != null ? beanReferences : new BeanReference[0]);
  }

  @Override
  public BeanDefinition[] getBeanDefinitions() {
    return this.beanDefinitions;
  }

  @Override
  public BeanReference[] getBeanReferences() {
    return this.beanReferences;
  }

}
