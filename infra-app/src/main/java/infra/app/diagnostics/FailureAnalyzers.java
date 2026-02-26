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

package infra.app.diagnostics;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import infra.beans.factory.BeanFactory;
import infra.context.BootstrapContext;
import infra.context.ConfigurableApplicationContext;
import infra.lang.TodayStrategies;
import infra.lang.TodayStrategies.FailureHandler;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Utility to trigger {@link FailureAnalyzer} and {@link FailureAnalysisReporter}
 * instances loaded from {@code today.strategies}.
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

  private final BootstrapContext context;

  private final TodayStrategies strategies;

  private final List<FailureAnalyzer> analyzers;

  FailureAnalyzers(ConfigurableApplicationContext context) {
    this(context, TodayStrategies.forDefaultResourceLocation(context.getClassLoader()));
  }

  FailureAnalyzers(ConfigurableApplicationContext context, TodayStrategies strategies) {
    this.strategies = strategies;
    this.context = context.getBootstrapContext();
    var analyzers = strategies.load(FailureAnalyzer.class, context.getBootstrapContext(), FailureHandler.logging(logger));
    analyzers.add(FailureAnalyzedException::analyze);
    this.analyzers = Collections.unmodifiableList(analyzers);
  }

  @Override
  public boolean reportException(Throwable failure) {
    return report(analyze(failure, this.analyzers));
  }

  private @Nullable FailureAnalysis analyze(Throwable failure, List<FailureAnalyzer> analyzers) {
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

  private boolean report(@Nullable FailureAnalysis analysis) {
    if (analysis == null) {
      return false;
    }
    List<FailureAnalysisReporter> reporters = strategies.load(FailureAnalysisReporter.class, context, FailureHandler.logging(logger));
    if (reporters.isEmpty()) {
      return false;
    }
    for (FailureAnalysisReporter reporter : reporters) {
      reporter.report(analysis);
    }
    return true;
  }

}
