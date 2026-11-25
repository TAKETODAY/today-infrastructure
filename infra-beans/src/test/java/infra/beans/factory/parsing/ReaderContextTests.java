/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.parsing;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import infra.core.io.DescriptiveResource;
import infra.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class ReaderContextTests {

  private final Resource resource = new DescriptiveResource("configuration.xml");

  private final Object source = new Object();

  private final ReaderEventListener listener = mock(ReaderEventListener.class);

  @Test
  void forwardsWarningWithResourceSourceAndCause() {
    var reporter = mock(ProblemReporter.class);
    var context = new ReaderContext(this.resource, reporter, this.listener, new PassThroughSourceExtractor());
    var cause = new IllegalStateException("invalid value");

    context.warning("Unknown setting", this.source, cause);

    var captor = ArgumentCaptor.forClass(Problem.class);
    then(reporter).should().warning(captor.capture());
    Problem problem = captor.getValue();
    assertThat(problem.getMessage()).isEqualTo("Unknown setting");
    assertThat(problem.getLocation().getResource()).isSameAs(this.resource);
    assertThat(problem.getLocation().getSource()).isSameAs(this.source);
    assertThat(problem.getRootCause()).isSameAs(cause);
    assertThat(problem.getParseState()).isNull();
  }

  @Test
  void forwardsFatalOverloadsWithTheSuppliedDetails() {
    var reporter = mock(ProblemReporter.class);
    var context = new ReaderContext(this.resource, reporter, this.listener, new NullSourceExtractor());
    var state = new ParseState();
    var cause = new IllegalStateException("cause");

    context.fatal("fatal", null);
    context.fatal("fatal with cause", this.source, cause);
    context.fatal("fatal with state", this.source, state);

    var captor = ArgumentCaptor.forClass(Problem.class);
    then(reporter).should(times(3)).fatal(captor.capture());
    assertThat(captor.getAllValues()).satisfiesExactly(
            problem -> assertProblem(problem, "fatal", null, null, null),
            problem -> assertProblem(problem, "fatal with cause", this.source, null, cause),
            problem -> assertProblem(problem, "fatal with state", this.source, state, null));
    then(reporter).shouldHaveNoMoreInteractions();
  }

  @Test
  void forwardsErrorOverloadsWithTheSuppliedDetails() {
    var reporter = mock(ProblemReporter.class);
    var context = new ReaderContext(this.resource, reporter, this.listener, new NullSourceExtractor());
    var state = new ParseState();
    var cause = new IllegalStateException("cause");

    context.error("error", this.source);
    context.error("error with cause", this.source, cause);
    context.error("error with state", this.source, state);
    context.error("error with both", this.source, state, cause);

    var captor = ArgumentCaptor.forClass(Problem.class);
    then(reporter).should(times(4)).error(captor.capture());
    assertThat(captor.getAllValues()).satisfiesExactly(
            problem -> assertProblem(problem, "error", this.source, null, null),
            problem -> assertProblem(problem, "error with cause", this.source, null, cause),
            problem -> assertProblem(problem, "error with state", this.source, state, null),
            problem -> assertProblem(problem, "error with both", this.source, state, cause));
    then(reporter).shouldHaveNoMoreInteractions();
  }

  @Test
  void forwardsWarningsWithoutCauseAndWithOptionalState() {
    var reporter = mock(ProblemReporter.class);
    var context = new ReaderContext(this.resource, reporter, this.listener, new NullSourceExtractor());
    var state = new ParseState();
    context.warning("warning", this.source);
    context.warning("warning with state", this.source, state);

    var captor = ArgumentCaptor.forClass(Problem.class);
    then(reporter).should(times(2)).warning(captor.capture());
    assertThat(captor.getAllValues()).satisfiesExactly(
            problem -> assertProblem(problem, "warning", this.source, null, null),
            problem -> assertProblem(problem, "warning with state", this.source, state, null));
    then(reporter).shouldHaveNoMoreInteractions();
  }

  @Test
  void reportsDefaultsComponentsAndAliases() {
    var context = new ReaderContext(this.resource, mock(ProblemReporter.class), this.listener, new NullSourceExtractor());
    var defaults = mock(DefaultsDefinition.class);
    var component = mock(ComponentDefinition.class);

    context.fireDefaultsRegistered(defaults);
    context.fireComponentRegistered(component);
    context.fireAliasRegistered("bean", "alias", this.source);

    then(this.listener).should().defaultsRegistered(defaults);
    then(this.listener).should().componentRegistered(component);
    var captor = ArgumentCaptor.forClass(AliasDefinition.class);
    then(this.listener).should().aliasRegistered(captor.capture());
    var alias = captor.getValue();
    assertThat(alias.getBeanName()).isEqualTo("bean");
    assertThat(alias.getAlias()).isEqualTo("alias");
    assertThat(alias.getSource()).isSameAs(this.source);
    then(this.listener).shouldHaveNoMoreInteractions();
  }

  @Test
  void fatalProblemIncludesParseStateAndPreservesCause() {
    var context = new ReaderContext(this.resource, new FailFastProblemReporter(), this.listener, new NullSourceExtractor());
    var state = new ParseState();
    state.push(new BeanEntry("sample"));
    var cause = new IllegalArgumentException("missing name");

    assertThatExceptionOfType(BeanDefinitionParsingException.class)
            .isThrownBy(() -> context.fatal("Invalid bean", this.source, state, cause))
            .withMessageContainingAll("Invalid bean", "configuration.xml", "sample")
            .withCause(cause);
  }

  @Test
  void reportsImportWithAndWithoutResolvedResources() {
    var context = new ReaderContext(this.resource, mock(ProblemReporter.class), this.listener, new NullSourceExtractor());
    var imported = new DescriptiveResource("imported.xml");
    context.fireImportProcessed("optional.xml", this.source);
    context.fireImportProcessed("imported.xml", new Resource[] { imported }, this.source);

    var captor = ArgumentCaptor.forClass(ImportDefinition.class);
    then(this.listener).should(times(2)).importProcessed(captor.capture());
    assertThat(captor.getAllValues()).satisfiesExactly(unresolved -> {
      assertThat(unresolved.getImportedResource()).isEqualTo("optional.xml");
      assertThat(unresolved.getSource()).isSameAs(this.source);
      assertThat(unresolved.getActualResources()).isNull();
    }, resolved -> {
      assertThat(resolved.getImportedResource()).isEqualTo("imported.xml");
      assertThat(resolved.getSource()).isSameAs(this.source);
      assertThat(resolved.getActualResources()).containsExactly(imported);
    });
  }

  @Test
  void passesContainingResourceToConfiguredSourceExtractor() {
    var extracted = new Object();
    SourceExtractor extractor = (candidate, resource) -> {
      assertThat(candidate).isSameAs(this.source);
      assertThat(resource).isSameAs(this.resource);
      return extracted;
    };
    var context = new ReaderContext(this.resource, mock(ProblemReporter.class), this.listener, extractor);

    assertThat(context.getSourceExtractor()).isSameAs(extractor);
    assertThat(context.extractSource(this.source)).isSameAs(extracted);
  }

  private void assertProblem(Problem problem, String message, @Nullable Object source,
          @Nullable ParseState state, @Nullable Throwable cause) {
    assertThat(problem.getMessage()).isEqualTo(message);
    assertThat(problem.getLocation().getResource()).isSameAs(this.resource);
    assertThat(problem.getLocation().getSource()).isSameAs(source);
    assertThat(problem.getParseState()).isSameAs(state);
    assertThat(problem.getRootCause()).isSameAs(cause);
  }

}
