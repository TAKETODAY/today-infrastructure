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

package cn.taketoday.beans.factory.dependency;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.StrategiesDetector;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
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

  private final ArrayList<DependencyResolvingStrategy> resolvingStrategies = new ArrayList<>();

  @Override
  public boolean supports(Field field) {
    for (DependencyResolvingStrategy resolvingStrategy : resolvingStrategies) {
      if (resolvingStrategy.supports(field)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean supports(Executable executable) {
    for (DependencyResolvingStrategy resolvingStrategy : resolvingStrategies) {
      if (resolvingStrategy.supports(executable)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void resolveDependency(
          DependencyDescriptor descriptor, DependencyResolvingContext resolvingContext) {
    resolvingContext.setDependency(null);
    resolvingContext.setDependencyResolved(false);
    for (DependencyResolvingStrategy resolvingStrategy : resolvingStrategies) {
      resolvingStrategy.resolveDependency(descriptor, resolvingContext);
      if (resolvingContext.isDependencyResolved()) {
        return;
      }
    }
    // TODO maybe check required status?
  }

  public void initStrategies(
          @Nullable StrategiesDetector strategiesDetector, @Nullable BeanFactory beanFactory) {
    log.debug("Initialize dependency-resolving-strategies");

    if (strategiesDetector == null) {
      strategiesDetector = TodayStrategies.getDetector();
    }
    List<DependencyResolvingStrategy> strategies =
            strategiesDetector.getStrategies(DependencyResolvingStrategy.class, beanFactory);

    // un-ordered
    resolvingStrategies.addAll(strategies); // @since 4.0
    AnnotationAwareOrderComparator.sort(resolvingStrategies);

    resolvingStrategies.add(new BeanFactoryDependencyResolvingStrategy());
    resolvingStrategies.trimToSize();
  }

  public ArrayList<DependencyResolvingStrategy> getStrategies() {
    return resolvingStrategies;
  }

  public void setStrategies(DependencyResolvingStrategy... strategies) {
    clear();
    addStrategies(strategies);
    resolvingStrategies.trimToSize();
  }

  public void setStrategies(List<DependencyResolvingStrategy> strategies) {
    clear();
    addStrategies(strategies);
    resolvingStrategies.trimToSize();
  }

  public void addStrategies(DependencyResolvingStrategy... strategies) {
    if (ObjectUtils.isNotEmpty(strategies)) {
      CollectionUtils.addAll(resolvingStrategies, strategies);
      AnnotationAwareOrderComparator.sort(resolvingStrategies);
    }
  }

  public void addStrategies(List<DependencyResolvingStrategy> strategies) {
    if (CollectionUtils.isNotEmpty(strategies)) {
      CollectionUtils.addAll(resolvingStrategies, strategies);
      AnnotationAwareOrderComparator.sort(resolvingStrategies);
    }
  }

  public void clear() {
    resolvingStrategies.clear();
  }

  public boolean isNotEmpty() {
    return !resolvingStrategies.isEmpty();
  }

}
