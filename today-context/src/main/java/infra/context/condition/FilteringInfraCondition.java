/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.condition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import infra.beans.BeansException;
import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.context.annotation.config.AutoConfigurationImportFilter;
import infra.context.annotation.config.AutoConfigurationMetadata;
import infra.util.ClassUtils;
import infra.util.CollectionUtils;

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

  @SuppressWarnings("NullAway.Init")
  private BeanFactory beanFactory;

  @SuppressWarnings("NullAway.Init")
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
  public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata configMetadata) {
    ConditionEvaluationReport report = ConditionEvaluationReport.find(this.beanFactory);
    @Nullable ConditionOutcome[] outcomes = getOutcomes(autoConfigurationClasses, configMetadata);
    boolean[] match = new boolean[outcomes.length];
    for (int i = 0; i < outcomes.length; i++) {
      ConditionOutcome outcome = outcomes[i];
      match[i] = (outcome == null || outcome.isMatch());
      if (!match[i] && outcome != null) {
        String autoConfigurationClass = autoConfigurationClasses[i];
        logOutcome(autoConfigurationClass, outcome);
        if (report != null) {
          report.recordConditionEvaluation(autoConfigurationClass, this, outcome);
        }
      }
    }
    return match;
  }

  protected abstract @Nullable ConditionOutcome[] getOutcomes(String[] configClasses, AutoConfigurationMetadata configMetadata);

  protected final List<String> filter(@Nullable Collection<String> classNames, ClassNameFilter classNameFilter, @Nullable ClassLoader classLoader) {
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

    private static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
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
