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

package infra.app.diagnostics.analyzer;

import infra.app.AotInitializerNotFoundException;
import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link AotInitializerNotFoundException}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
class AotInitializerNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<AotInitializerNotFoundException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, AotInitializerNotFoundException cause) {
    return new FailureAnalysis(cause.getMessage(), "Consider the following:\n"
            + "\tDid you build the application with enabled AOT processing?\n"
            + "\tIs the main class %s correct?\n".formatted(cause.getMainClass().getName())
            + "\tIf you want to run the application in regular mode, remove the system property 'infra.aot.enabled'",
            cause);
  }

}
