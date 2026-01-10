/*
 * Copyright 2002-present the original author or authors.
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

package infra.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import infra.core.io.DescriptiveResource;
import infra.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class FailFastProblemReporterTests {

  @Test
  public void testError() throws Exception {
    FailFastProblemReporter reporter = new FailFastProblemReporter();
    assertThatExceptionOfType(BeanDefinitionParsingException.class).isThrownBy(() ->
            reporter.error(new Problem("VGER", new Location(new DescriptiveResource("here")),
                    null, new IllegalArgumentException())));
  }

  @Test
  public void testWarn() throws Exception {
    Problem problem = new Problem("VGER", new Location(new DescriptiveResource("here")),
            null, new IllegalArgumentException());

    Logger log = mock(Logger.class);

    FailFastProblemReporter reporter = new FailFastProblemReporter();
    reporter.setLogger(log);
    reporter.warning(problem);

    verify(log).warn(eq(problem), isA(IllegalArgumentException.class));
  }

}
