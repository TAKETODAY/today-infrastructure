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

import infra.core.io.DescriptiveResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ProblemTests {

  @Test
  void exposesProblemDetailsAndFormatsParseState() {
    var location = new Location(new DescriptiveResource("configuration.xml"), "bean");
    var state = new ParseState();
    state.push(new BeanEntry("sample"));
    var cause = new IllegalStateException("cause");
    var problem = new Problem("Invalid definition", location, state, cause);
    assertThat(problem.getMessage()).isEqualTo("Invalid definition");
    assertThat(problem.getLocation()).isSameAs(location);
    assertThat(problem.getResourceDescription()).contains("configuration.xml");
    assertThat(problem.getParseState()).isSameAs(state);
    assertThat(problem.getRootCause()).isSameAs(cause);
    assertThat(problem.toString()).contains("Invalid definition", "configuration.xml", "Bean 'sample'");
  }

  @Test
  void optionalDetailsMayBeAbsent() {
    var location = new Location(new DescriptiveResource("resource"));
    var problem = new Problem("message", location);
    assertThat(problem.getParseState()).isNull();
    assertThat(problem.getRootCause()).isNull();
    assertThat(problem.toString()).isEqualTo("Configuration problem: message\nOffending resource: resource");

    var state = new ParseState();
    var problemWithState = new Problem("message", location, state);
    assertThat(problemWithState.getParseState()).isSameAs(state);
    assertThat(problemWithState.getRootCause()).isNull();
  }

  @Test
  void messageAndLocationAreRequired() {
    var location = new Location(new DescriptiveResource("resource"));
    assertThatIllegalArgumentException().isThrownBy(() -> new Problem(null, location));
    assertThatIllegalArgumentException().isThrownBy(() -> new Problem("message", null));
  }

}
