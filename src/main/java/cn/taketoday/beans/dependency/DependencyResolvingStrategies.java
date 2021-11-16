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

package cn.taketoday.beans.dependency;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 22:50</a>
 * @since 4.0
 */
public class DependencyResolvingStrategies implements DependencyResolvingStrategy {
  private static final Logger log = LoggerFactory.getLogger(DependencyResolvingStrategies.class);

  private final StrategiesDetector strategiesDetector;
  private final ArrayList<DependencyResolvingStrategy> resolvingStrategies = new ArrayList<>();

  public DependencyResolvingStrategies() {
    this.strategiesDetector = TodayStrategies.getDetector();
  }

  public DependencyResolvingStrategies(StrategiesDetector strategiesDetector) {
    Assert.notNull(strategiesDetector, "StrategiesDetector must not be null");
    this.strategiesDetector = strategiesDetector;
  }

  @Override
  public void resolveDependency(
          DependencyInjectionPoint injectionPoint, DependencyResolvingContext resolvingContext) {
    Class<?> dependencyType = injectionPoint.getDependencyType();
    Object dependency = findProvided(dependencyType, resolvingContext.getProvidedArgs());
    resolvingContext.setDependency(dependency);

    for (DependencyResolvingStrategy resolvingStrategy : resolvingStrategies) {
      resolvingStrategy.resolveDependency(injectionPoint, resolvingContext);
    }
  }

  @Nullable
  public static Object findProvided(Class<?> dependencyType, @Nullable Object[] providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      for (final Object providedArg : providedArgs) {
        if (dependencyType.isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

  public ArrayList<DependencyResolvingStrategy> getResolvingStrategies() {
    return resolvingStrategies;
  }

  public void setResolvingStrategies(DependencyResolvingStrategy... strategies) {
    resolvingStrategies.clear();
    addResolvingStrategies(strategies);
    resolvingStrategies.trimToSize();
  }

  public void setResolvingStrategies(List<DependencyResolvingStrategy> strategies) {
    resolvingStrategies.clear();
    addResolvingStrategies(strategies);
    resolvingStrategies.trimToSize();
  }

  public void addResolvingStrategies(DependencyResolvingStrategy... strategies) {
    if (ObjectUtils.isNotEmpty(strategies)) {
      CollectionUtils.addAll(resolvingStrategies, strategies);
      AnnotationAwareOrderComparator.sort(resolvingStrategies);
    }
  }

  public void addResolvingStrategies(List<DependencyResolvingStrategy> strategies) {
    if (CollectionUtils.isNotEmpty(strategies)) {
      CollectionUtils.addAll(resolvingStrategies, strategies);
      AnnotationAwareOrderComparator.sort(resolvingStrategies);
    }
  }

}
