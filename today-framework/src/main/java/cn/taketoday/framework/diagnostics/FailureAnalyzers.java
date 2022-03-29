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

package cn.taketoday.framework.diagnostics;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Utility to trigger {@link FailureAnalyzer} and {@link FailureAnalysisReporter}
 * instances loaded from {@code today-strategies.properties}.
 * <p>
 * A {@code FailureAnalyzer} that requires access to the {@link BeanFactory} in order to
 * perform its analysis can implement {@code BeanFactoryAware} to have the
 * {@code BeanFactory} injected prior to {@link FailureAnalyzer#analyze(Throwable)} being
 * called.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class FailureAnalyzers implements ApplicationExceptionReporter {

  private static final Logger logger = LoggerFactory.getLogger(FailureAnalyzers.class);

  @Nullable
  private final ClassLoader classLoader;

  private final List<FailureAnalyzer> analyzers;

  FailureAnalyzers(ConfigurableApplicationContext context) {
    this(context, null);
  }

  FailureAnalyzers(ConfigurableApplicationContext context, @Nullable ClassLoader classLoader) {
    this.classLoader = (classLoader != null) ? classLoader : getClassLoader(context);
    this.analyzers = loadFailureAnalyzers(context, this.classLoader);
  }

  @Nullable
  private ClassLoader getClassLoader(@Nullable ConfigurableApplicationContext context) {
    return (context != null) ? context.getClassLoader() : null;
  }

  private List<FailureAnalyzer> loadFailureAnalyzers(
          ConfigurableApplicationContext context, @Nullable ClassLoader classLoader) {
    List<String> classNames = TodayStrategies.getStrategiesNames(FailureAnalyzer.class, classLoader);
    List<FailureAnalyzer> analyzers = new ArrayList<>();
    for (String className : classNames) {
      try {
        FailureAnalyzer analyzer = createAnalyzer(context, className);
        if (analyzer != null) {
          analyzers.add(analyzer);
        }
      }
      catch (Throwable ex) {
        logger.trace(LogMessage.format("Failed to load %s", className), ex);
      }
    }
    AnnotationAwareOrderComparator.sort(analyzers);
    return analyzers;
  }

  @Nullable
  private FailureAnalyzer createAnalyzer(@Nullable ConfigurableApplicationContext context, String className) throws Exception {
    Constructor<?> constructor = ClassUtils.forName(className, this.classLoader).getDeclaredConstructor();
    ReflectionUtils.makeAccessible(constructor);
    FailureAnalyzer analyzer = (FailureAnalyzer) constructor.newInstance();
    if (analyzer instanceof BeanFactoryAware || analyzer instanceof EnvironmentAware) {
      if (context == null) {
        logger.trace(LogMessage.format("Skipping %s due to missing context", className));
        return null;
      }
      if (analyzer instanceof BeanFactoryAware) {
        ((BeanFactoryAware) analyzer).setBeanFactory(context.getBeanFactory());
      }
      if (analyzer instanceof EnvironmentAware) {
        ((EnvironmentAware) analyzer).setEnvironment(context.getEnvironment());
      }
    }
    return analyzer;
  }

  @Override
  public boolean reportException(Throwable failure) {
    FailureAnalysis analysis = analyze(failure, this.analyzers);
    return report(analysis, this.classLoader);
  }

  @Nullable
  private FailureAnalysis analyze(Throwable failure, List<FailureAnalyzer> analyzers) {
    for (FailureAnalyzer analyzer : analyzers) {
      try {
        FailureAnalysis analysis = analyzer.analyze(failure);
        if (analysis != null) {
          return analysis;
        }
      }
      catch (Throwable ex) {
        logger.trace(LogMessage.format("FailureAnalyzer %s failed", analyzer), ex);
      }
    }
    return null;
  }

  private boolean report(@Nullable FailureAnalysis analysis, ClassLoader classLoader) {
    List<FailureAnalysisReporter> reporters = TodayStrategies.get(
            FailureAnalysisReporter.class, classLoader);
    if (analysis == null || reporters.isEmpty()) {
      return false;
    }
    for (FailureAnalysisReporter reporter : reporters) {
      reporter.report(analysis);
    }
    return true;
  }

}
