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

import java.util.List;
import java.util.stream.Collectors;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.core.env.Environment;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.Instantiator;
import cn.taketoday.util.Instantiator.FailureHandler;
import cn.taketoday.util.StringUtils;

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

  FailureAnalyzers(@Nullable ConfigurableApplicationContext context) {
    this(context, TodayStrategies.getStrategiesNames(FailureAnalyzer.class, getClassLoader(context)));
  }

  FailureAnalyzers(@Nullable ConfigurableApplicationContext context, List<String> classNames) {
    this.classLoader = getClassLoader(context);
    this.analyzers = loadFailureAnalyzers(classNames, context);
  }

  @Nullable
  private static ClassLoader getClassLoader(@Nullable ConfigurableApplicationContext context) {
    return context != null ? context.getClassLoader() : null;
  }

  private List<FailureAnalyzer> loadFailureAnalyzers(
          List<String> classNames, @Nullable ConfigurableApplicationContext context) {
    var instantiator = new Instantiator<FailureAnalyzer>(FailureAnalyzer.class,
            parameters -> {
              if (context != null) {
                parameters.add(BeanFactory.class, context.getBeanFactory());
                parameters.add(Environment.class, context.getEnvironment());
              }
            }, new LoggingInstantiationFailureHandler());
    List<FailureAnalyzer> analyzers = instantiator.instantiate(classLoader, classNames);
    return handleAwareAnalyzers(analyzers, context);
  }

  private List<FailureAnalyzer> handleAwareAnalyzers(
          List<FailureAnalyzer> analyzers, @Nullable ConfigurableApplicationContext context) {
    List<FailureAnalyzer> awareAnalyzers = analyzers.stream()
            .filter((analyzer) -> analyzer instanceof BeanFactoryAware || analyzer instanceof EnvironmentAware)
            .toList();
    if (!awareAnalyzers.isEmpty()) {
      String awareAnalyzerNames = StringUtils.collectionToCommaDelimitedString(awareAnalyzers.stream()
              .map((analyzer) -> analyzer.getClass().getName()).collect(Collectors.toList()));
      logger.warn("FailureAnalyzers [{}] implement BeanFactoryAware or EnvironmentAware."
                      + "Support for these interfaces on FailureAnalyzers is deprecated, "
                      + "and will be removed in a future release."
                      + "Instead provide a constructor that accepts BeanFactory or Environment parameters.",
              awareAnalyzerNames);

      if (context == null) {
        logger.trace("Skipping [{}] due to missing context", awareAnalyzerNames);
        return analyzers.stream().filter((analyzer) -> !awareAnalyzers.contains(analyzer))
                .collect(Collectors.toList());
      }
      for (FailureAnalyzer analyzer : awareAnalyzers) {
        if (analyzer instanceof BeanFactoryAware) {
          ((BeanFactoryAware) analyzer).setBeanFactory(context.getBeanFactory());
        }
        if (analyzer instanceof EnvironmentAware) {
          ((EnvironmentAware) analyzer).setEnvironment(context.getEnvironment());
        }
      }
    }
    return analyzers;
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
        logger.trace("FailureAnalyzer {} failed", analyzer, ex);
      }
    }
    return null;
  }

  private boolean report(@Nullable FailureAnalysis analysis, @Nullable ClassLoader classLoader) {
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

  static class LoggingInstantiationFailureHandler implements FailureHandler {

    @Override
    public void handleFailure(Class<?> type, String implementationName, Throwable failure) {
      logger.trace("Skipping {}: {}", implementationName, failure.getMessage());
    }

  }

}
