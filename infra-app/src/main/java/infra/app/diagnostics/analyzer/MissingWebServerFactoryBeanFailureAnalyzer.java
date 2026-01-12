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

import infra.app.diagnostics.AbstractFailureAnalyzer;
import infra.app.diagnostics.FailureAnalysis;
import infra.app.diagnostics.FailureAnalyzer;
import infra.core.Ordered;
import infra.web.server.context.MissingWebServerFactoryBeanException;
import infra.web.server.reactive.ReactiveWebServerFactory;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by an
 * {@link MissingWebServerFactoryBeanException}.
 *
 * @author Guirong Hu
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 21:23
 */
class MissingWebServerFactoryBeanFailureAnalyzer
        extends AbstractFailureAnalyzer<MissingWebServerFactoryBeanException> implements Ordered {

  @Override
  @SuppressWarnings("NullAway")
  protected FailureAnalysis analyze(Throwable rootFailure, MissingWebServerFactoryBeanException cause) {
    return new FailureAnalysis(
            "Web application could not be started as there was no " + cause.getBeanType().getName()
                    + " bean defined in the context.",
            "Check your application's dependencies for a supported "
                    + getApplicationType(cause) + " server.\n"
                    + "Check the configured web application type.",
            cause);
  }

  private String getApplicationType(MissingWebServerFactoryBeanException cause) {
    if (cause.getWebServerFactoryClass() == ReactiveWebServerFactory.class) {
      return "reactive web";
    }
    return "web";
  }

  @Override
  public int getOrder() {
    return 0;
  }

}

