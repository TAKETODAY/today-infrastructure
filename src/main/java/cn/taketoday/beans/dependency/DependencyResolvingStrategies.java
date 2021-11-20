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
  public void resolveDependency(
          DependencyInjectionPoint injectionPoint, DependencyResolvingContext resolvingContext) {
    for (DependencyResolvingStrategy resolvingStrategy : resolvingStrategies) {
      resolvingStrategy.resolveDependency(injectionPoint, resolvingContext);
      if (resolvingContext.isTerminate()) {
        return;
      }
    }
    if (!resolvingContext.hasDependency()) {
      resolvingContext.setDependency(DependencyInjectionPoint.DO_NOT_SET);
    }
  }

  public void initStrategies(
          @Nullable StrategiesDetector strategiesDetector, @Nullable BeanFactory beanFactory) {
    log.debug("Initialize dependency-resolving-strategies");
    resolvingStrategies.add(new OptionalDependencyResolver());
    resolvingStrategies.add(new ArrayBeanDependencyResolver());
    resolvingStrategies.add(new ObjectSupplierDependencyResolvingStrategy());
    resolvingStrategies.add(new CollectionDependencyResolvingStrategy());
    resolvingStrategies.add(new AutowiredDependencyResolvingStrategy());

    try { // @formatter:off
      resolvingStrategies.add(new JSR330InjectDependencyResolver());
      log.debug("Add JSR-330 '@Inject,@Named' annotation supports");
    }
    catch (Exception ignored) {}
    try {
      resolvingStrategies.add(new JSR250ResourceDependencyResolvingStrategy());
      log.debug("Add JSR-250 '@Resource' annotation supports");
    }
    catch (Exception ignored) {}
    // @formatter:on

    if (strategiesDetector == null) {
      strategiesDetector = TodayStrategies.getDetector();
    }
    List<DependencyResolvingStrategy> strategies =
            strategiesDetector.getStrategies(DependencyResolvingStrategy.class, beanFactory);

    // un-ordered
    resolvingStrategies.addAll(strategies); // @since 4.0
    resolvingStrategies.trimToSize();
    AnnotationAwareOrderComparator.sort(resolvingStrategies);
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

}
