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

package cn.taketoday.context.loader;

import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.core.bytecode.tree.ClassNode;

/**
 * @author TODAY 2021/10/10 22:27
 * @since 4.0
 */
public abstract class AbstractBeanDefinitionCreationStrategy implements BeanDefinitionCreationStrategy {

  @Override
  public Set<BeanDefinition> loadBeanDefinitions(ClassNode classNode, BeanDefinitionCreationContext creationContext) {
    if (supports(classNode, creationContext)) {
      return createInternal(classNode, creationContext);
    }
    return null;
  }

  protected abstract boolean supports(
          ClassNode classNode, BeanDefinitionCreationContext creationContext);

  protected abstract Set<BeanDefinition> createInternal(
          ClassNode classNode, BeanDefinitionCreationContext creationContext);
}
