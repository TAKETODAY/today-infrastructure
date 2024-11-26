/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.diagnostics.analyzer;

import java.util.ArrayList;
import java.util.List;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.InjectionPoint;
import infra.beans.factory.UnsatisfiedDependencyException;
import infra.beans.factory.support.AbstractAutowireCapableBeanFactory;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link BeanCurrentlyInCreationException}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BeanCurrentlyInCreationFailureAnalyzer
        extends AbstractFailureAnalyzer<BeanCurrentlyInCreationException> {

  @Nullable
  private final AbstractAutowireCapableBeanFactory beanFactory;

  public BeanCurrentlyInCreationFailureAnalyzer(@Nullable AbstractAutowireCapableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
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
            + "app.main.allow-circular-references to true.";
  }

  @Nullable
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
    List<BeanInCycle> beansInCycle = dependencyCycle.beansInCycle();
    boolean singleBean = beansInCycle.size() == 1;
    int cycleStart = dependencyCycle.cycleStart();
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

  private record DependencyCycle(List<BeanInCycle> beansInCycle, int cycleStart) {

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

    @Nullable
    private InjectionPoint findFailedInjectionPoint(BeanCreationException ex) {
      if (ex instanceof UnsatisfiedDependencyException e) {
        return e.getInjectionPoint();
      }
      return null;
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

    @Nullable
    static BeanInCycle get(Throwable ex) {
      if (ex instanceof BeanCreationException) {
        return get((BeanCreationException) ex);
      }
      return null;
    }

    @Nullable
    private static BeanInCycle get(BeanCreationException ex) {
      if (StringUtils.hasText(ex.getBeanName())) {
        return new BeanInCycle(ex);
      }
      return null;
    }

  }

}
