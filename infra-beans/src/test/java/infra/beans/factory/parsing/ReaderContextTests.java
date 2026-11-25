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

}
