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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.beans.factory.BeanFactory;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link FailureAnalyzers}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
@ExtendWith(OutputCaptureExtension.class)
class FailureAnalyzersTests {

  private static AwareFailureAnalyzer failureAnalyzer;

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @BeforeEach
  void configureMock() {
    failureAnalyzer = mock(AwareFailureAnalyzer.class);
  }

  @Test
  void analyzersAreLoadedAndCalled() {
    RuntimeException failure = new RuntimeException();
    analyzeAndReport(failure, BasicFailureAnalyzer.class.getName(), BasicFailureAnalyzer.class.getName());
    then(failureAnalyzer).should(times(2)).analyze(failure);
  }

  @Test
  void analyzerIsConstructedWithBeanFactory(CapturedOutput output) {
    RuntimeException failure = new RuntimeException();
    analyzeAndReport(failure, BasicFailureAnalyzer.class.getName(),
            BeanFactoryConstructorFailureAnalyzer.class.getName());
    then(failureAnalyzer).should(times(2)).analyze(failure);
    assertThat(output).doesNotContain("implement BeanFactoryAware or EnvironmentAware");
  }

  @Test
  void analyzerIsConstructedWithEnvironment(CapturedOutput output) {
    RuntimeException failure = new RuntimeException();
    analyzeAndReport(failure, BasicFailureAnalyzer.class.getName(),
            EnvironmentConstructorFailureAnalyzer.class.getName());
    then(failureAnalyzer).should(times(2)).analyze(failure);
    assertThat(output).doesNotContain("implement BeanFactoryAware or EnvironmentAware");
  }

  @Test
  void environmentIsInjectedIntoEnvironmentAwareFailureAnalyzers() {
    RuntimeException failure = new RuntimeException();
    analyzeAndReport(failure, BasicFailureAnalyzer.class.getName(), StandardAwareFailureAnalyzer.class.getName());
  }

  @Test
  void analyzerThatFailsDuringInitializationDoesNotPreventOtherAnalyzersFromBeingCalled() {
    RuntimeException failure = new RuntimeException();
    analyzeAndReport(failure, BrokenInitializationFailureAnalyzer.class.getName(),
            BasicFailureAnalyzer.class.getName());
    then(failureAnalyzer).should().analyze(failure);
  }

  @Test
  void analyzerThatFailsDuringAnalysisDoesNotPreventOtherAnalyzersFromBeingCalled() {
    RuntimeException failure = new RuntimeException();
    analyzeAndReport(failure, BrokenAnalysisFailureAnalyzer.class.getName(), BasicFailureAnalyzer.class.getName());
    then(failureAnalyzer).should().analyze(failure);
  }

  private void analyzeAndReport(Throwable failure, String... factoryNames) {
    analyzeAndReport(failure, this.context, factoryNames);
  }

  private void analyzeAndReport(Throwable failure, AnnotationConfigApplicationContext context,
          String... factoryNames) {
    new FailureAnalyzers(context, Arrays.asList(factoryNames)).reportException(failure);
  }

  static class BasicFailureAnalyzer implements FailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
      return failureAnalyzer.analyze(failure);
    }

  }

  static class BrokenInitializationFailureAnalyzer implements FailureAnalyzer {

    static {
      Object foo = null;
      foo.toString();
    }

    @Override
    public FailureAnalysis analyze(Throwable failure) {
      return null;
    }

  }

  static class BrokenAnalysisFailureAnalyzer implements FailureAnalyzer {

    @Override
    public FailureAnalysis analyze(Throwable failure) {
      throw new NoClassDefFoundError();
    }

  }

  static class BeanFactoryConstructorFailureAnalyzer extends BasicFailureAnalyzer {

    BeanFactoryConstructorFailureAnalyzer(BeanFactory beanFactory) {
      assertThat(beanFactory).isNotNull();
    }

  }

  static class EnvironmentConstructorFailureAnalyzer extends BasicFailureAnalyzer {

    EnvironmentConstructorFailureAnalyzer(Environment environment) {
      assertThat(environment).isNotNull();
    }

  }

  interface AwareFailureAnalyzer extends FailureAnalyzer {

  }

  static class StandardAwareFailureAnalyzer extends BasicFailureAnalyzer implements AwareFailureAnalyzer {

  }

}
