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

package cn.taketoday.framework.diagnostics.analyzer;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.support.AbstractAutowireCapableBeanFactory;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanCurrentlyInCreationException}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanCurrentlyInCreationFailureAnalyzer extends AbstractFailureAnalyzer<BeanCurrentlyInCreationException>
        implements BeanFactoryAware {

  private AbstractAutowireCapableBeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (beanFactory instanceof AbstractAutowireCapableBeanFactory) {
      this.beanFactory = (AbstractAutowireCapableBeanFactory) beanFactory;
    }
  }

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, BeanCurrentlyInCreationException cause) {
    DependencyCycle dependencyCycle = findCycle(rootFailure);
    if (dependencyCycle == null) {
      return null;
    }
    return new FailureAnalysis(buildMessage(dependencyCycle), action(), cause);
  }

  private String action() {
    if (this.beanFactory != null && this.beanFactory.isAllowCircularReferences()) {
      return "Despite circular references being allowed, the dependency cycle between beans could not be "
              + "broken. Update your application to remove the dependency cycle.";
    }
    return "Relying upon circular references is discouraged and they are prohibited by default. "
            + "Update your application to remove the dependency cycle between beans. "
            + "As a last resort, it may be possible to break the cycle automatically by setting "
            + "context.main.allow-circular-references to true.";
  }

  private DependencyCycle findCycle(Throwable rootFailure) {
    List<BeanInCycle> beansInCycle = new ArrayList<>();
    Throwable candidate = rootFailure;
    int cycleStart = -1;
    while (candidate != null) {
      BeanInCycle beanInCycle = BeanInCycle.get(candidate);
      if (beanInCycle != null) {
        int index = beansInCycle.indexOf(beanInCycle);
        if (index == -1) {
          beansInCycle.add(beanInCycle);
        }
        cycleStart = (cycleStart != -1) ? cycleStart : index;
      }
      candidate = candidate.getCause();
    }
    if (cycleStart == -1) {
      return null;
    }
    return new DependencyCycle(beansInCycle, cycleStart);
  }

  private String buildMessage(DependencyCycle dependencyCycle) {
    StringBuilder message = new StringBuilder();
    message.append(
            String.format("The dependencies of some of the beans in the application context form a cycle:%n%n"));
    List<BeanInCycle> beansInCycle = dependencyCycle.getBeansInCycle();
    boolean singleBean = beansInCycle.size() == 1;
    int cycleStart = dependencyCycle.getCycleStart();
    for (int i = 0; i < beansInCycle.size(); i++) {
      BeanInCycle beanInCycle = beansInCycle.get(i);
      if (i == cycleStart) {
        message.append(String.format(singleBean ? "┌──->──┐%n" : "┌─────┐%n"));
      }
      else if (i > 0) {
        String leftSide = (i < cycleStart) ? " " : "↑";
        message.append(String.format("%s     ↓%n", leftSide));
      }
      String leftSide = (i < cycleStart) ? " " : "|";
      message.append(String.format("%s  %s%n", leftSide, beanInCycle));
    }
    message.append(String.format(singleBean ? "└──<-──┘%n" : "└─────┘%n"));
    return message.toString();
  }

  private static final class DependencyCycle {

    private final List<BeanInCycle> beansInCycle;

    private final int cycleStart;

    private DependencyCycle(List<BeanInCycle> beansInCycle, int cycleStart) {
      this.beansInCycle = beansInCycle;
      this.cycleStart = cycleStart;
    }

    List<BeanInCycle> getBeansInCycle() {
      return this.beansInCycle;
    }

    int getCycleStart() {
      return this.cycleStart;
    }

  }

  private static final class BeanInCycle {

    private final String name;

    private final String description;

    private BeanInCycle(BeanCreationException ex) {
      this.name = ex.getBeanName();
      this.description = determineDescription(ex);
    }

    private String determineDescription(BeanCreationException ex) {
      if (StringUtils.hasText(ex.getResourceDescription())) {
        return String.format(" defined in %s", ex.getResourceDescription());
      }
      InjectionPoint failedInjectionPoint = findFailedInjectionPoint(ex);
      if (failedInjectionPoint != null && failedInjectionPoint.getField() != null) {
        return String.format(" (field %s)", failedInjectionPoint.getField());
      }
      return "";
    }

    private InjectionPoint findFailedInjectionPoint(BeanCreationException ex) {
      if (!(ex instanceof UnsatisfiedDependencyException)) {
        return null;
      }
      return ((UnsatisfiedDependencyException) ex).getInjectionPoint();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return this.name.equals(((BeanInCycle) obj).name);
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }

    @Override
    public String toString() {
      return this.name + this.description;
    }

    static BeanInCycle get(Throwable ex) {
      if (ex instanceof BeanCreationException) {
        return get((BeanCreationException) ex);
      }
      return null;
    }

    private static BeanInCycle get(BeanCreationException ex) {
      if (StringUtils.hasText(ex.getBeanName())) {
        return new BeanInCycle(ex);
      }
      return null;
    }

  }

}
