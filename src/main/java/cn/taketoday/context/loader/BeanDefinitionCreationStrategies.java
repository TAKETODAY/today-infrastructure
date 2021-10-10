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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.TodayStrategies;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2021/10/10 22:06
 * @since 4.0
 */
public class BeanDefinitionCreationStrategies implements BeanDefinitionCreationStrategy {
  private final ArrayList<BeanDefinitionCreationStrategy> creationStrategies = new ArrayList<>();

  {
    TodayStrategies detector = TodayStrategies.getDetector();
    List<BeanDefinitionCreationStrategy> strategies = detector.getStrategies(
            BeanDefinitionCreationStrategy.class);
    addStrategies(strategies);
  }

  @Override
  public Set<BeanDefinition> loadBeanDefinitions(
          ClassNode classNode, BeanDefinitionCreationContext creationContext) {
    LinkedHashSet<BeanDefinition> definitions = new LinkedHashSet<>();
    for (BeanDefinitionCreationStrategy strategy : creationStrategies) {
      Set<BeanDefinition> beanDefinitions = strategy.loadBeanDefinitions(classNode, creationContext);
      if (CollectionUtils.isNotEmpty(beanDefinitions)) {
        definitions.addAll(beanDefinitions);
      }
    }
    return definitions;
  }

  public void addStrategies(@Nullable BeanDefinitionCreationStrategy... strategies) {
    CollectionUtils.addAll(creationStrategies, strategies);
    creationStrategies.trimToSize();
  }

  public void addStrategies(@Nullable List<BeanDefinitionCreationStrategy> strategies) {
    CollectionUtils.addAll(creationStrategies, strategies);
    creationStrategies.trimToSize();
  }

  public void setStrategies(@Nullable BeanDefinitionCreationStrategy... strategies) {
    creationStrategies.clear();
    addStrategies(strategies);
  }

  public void setStrategies(@Nullable List<BeanDefinitionCreationStrategy> strategies) {
    creationStrategies.clear();
    addStrategies(strategies);
  }

}
