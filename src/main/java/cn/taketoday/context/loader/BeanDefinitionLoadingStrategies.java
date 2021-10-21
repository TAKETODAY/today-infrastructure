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

package cn.taketoday.context.loader;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY 2021/10/10 22:06
 * @since 4.0
 */
public class BeanDefinitionLoadingStrategies implements BeanDefinitionLoadingStrategy {
  private final ArrayList<BeanDefinitionLoadingStrategy> creationStrategies = new ArrayList<>();

  public BeanDefinitionLoadingStrategies() {
    TodayStrategies detector = TodayStrategies.getDetector();
    List<BeanDefinitionLoadingStrategy> strategies = detector.getStrategies(
            BeanDefinitionLoadingStrategy.class);
    addStrategies(strategies);
  }

  @Override
  public Set<BeanDefinition> loadBeanDefinitions(
          MetadataReader metadata, DefinitionLoadingContext loadingContext) {
    LinkedHashSet<BeanDefinition> definitions = new LinkedHashSet<>();
    for (BeanDefinitionLoadingStrategy strategy : creationStrategies) {
      Set<BeanDefinition> beanDefinitions = strategy.loadBeanDefinitions(metadata, loadingContext);
      if (CollectionUtils.isNotEmpty(beanDefinitions)) {
        definitions.addAll(beanDefinitions);
      }
    }
    return definitions;
  }

  public void addStrategies(@Nullable BeanDefinitionLoadingStrategy... strategies) {
    CollectionUtils.addAll(creationStrategies, strategies);
    creationStrategies.trimToSize();
  }

  public void addStrategies(@Nullable List<BeanDefinitionLoadingStrategy> strategies) {
    CollectionUtils.addAll(creationStrategies, strategies);
    creationStrategies.trimToSize();
  }

  public void setStrategies(@Nullable BeanDefinitionLoadingStrategy... strategies) {
    creationStrategies.clear();
    addStrategies(strategies);
  }

  public void setStrategies(@Nullable List<BeanDefinitionLoadingStrategy> strategies) {
    creationStrategies.clear();
    addStrategies(strategies);
  }

}
