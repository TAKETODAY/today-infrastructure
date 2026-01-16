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

package infra.web.server.diagnostics;

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.web.server.PortInUseException;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code PortInUseException}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PortInUseFailureAnalyzer extends AbstractFailureAnalyzer<PortInUseException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, PortInUseException cause) {
    return new FailureAnalysis("Web server failed to start. Port %d was already in use.".formatted(cause.getPort()),
            "Identify and stop the process that's listening on port %d or configure this application to listen on another port.".formatted(cause.getPort()), cause);
  }

}
