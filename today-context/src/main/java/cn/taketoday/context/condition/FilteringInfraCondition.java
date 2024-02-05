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

package cn.taketoday.context.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.context.annotation.config.AutoConfigurationImportFilter;
import cn.taketoday.context.annotation.config.AutoConfigurationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * Abstract base class for a {@link InfraCondition} that also implements
 * {@link AutoConfigurationImportFilter}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 16:07
 */
public abstract class FilteringInfraCondition extends InfraCondition
        implements AutoConfigurationImportFilter, BeanFactoryAware, BeanClassLoaderAware {

  private BeanFactory beanFactory;

  private ClassLoader beanClassLoader;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  protected final BeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  protected final ClassLoader getBeanClassLoader() {
    return this.beanClassLoader;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public boolean[] match(String[] configClasses, AutoConfigurationMetadata configMetadata) {
    ConditionEvaluationReport report = ConditionEvaluationReport.find(this.beanFactory);
    ConditionOutcome[] outcomes = getOutcomes(configClasses, configMetadata);
    boolean[] match = new boolean[outcomes.length];
    for (int i = 0; i < outcomes.length; i++) {
      match[i] = (outcomes[i] == null || outcomes[i].isMatch());
      if (!match[i] && outcomes[i] != null) {
        logOutcome(configClasses[i], outcomes[i]);
        if (report != null) {
          report.recordConditionEvaluation(configClasses[i], this, outcomes[i]);
        }
      }
    }
    return match;
  }

  protected abstract ConditionOutcome[] getOutcomes(String[] configClasses, AutoConfigurationMetadata configMetadata);

  protected final List<String> filter(Collection<String> classNames,
          ClassNameFilter classNameFilter, ClassLoader classLoader) {
    if (CollectionUtils.isEmpty(classNames)) {
      return Collections.emptyList();
    }
    ArrayList<String> matches = new ArrayList<>(classNames.size());
    for (String candidate : classNames) {
      if (classNameFilter.matches(candidate, classLoader)) {
        matches.add(candidate);
      }
    }
    return matches;
  }

  /**
   * Slightly faster variant of {@link ClassUtils#forName(String, ClassLoader)} that
   * doesn't deal with primitives, arrays or inner types.
   *
   * @param className the class name to resolve
   * @param classLoader the class loader to use
   * @return a resolved class
   * @throws ClassNotFoundException if the class cannot be found
   */
  protected static Class<?> resolve(String className, @Nullable ClassLoader classLoader) throws ClassNotFoundException {
    if (classLoader != null) {
      return Class.forName(className, false, classLoader);
    }
    return Class.forName(className);
  }

  protected enum ClassNameFilter {

    PRESENT {
      @Override
      public boolean matches(String className, @Nullable ClassLoader classLoader) {
        return isPresent(className, classLoader);
      }

    },

    MISSING {
      @Override
      public boolean matches(String className, @Nullable ClassLoader classLoader) {
        return !isPresent(className, classLoader);
      }

    };

    public abstract boolean matches(String className, @Nullable ClassLoader classLoader);

    public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
      if (classLoader == null) {
        classLoader = ClassUtils.getDefaultClassLoader();
      }
      try {
        resolve(className, classLoader);
        return true;
      }
      catch (Throwable ex) {
        return false;
      }
    }

  }
}
