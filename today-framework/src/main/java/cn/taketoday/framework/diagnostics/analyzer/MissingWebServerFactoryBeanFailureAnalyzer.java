/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.diagnostics.analyzer;

import cn.taketoday.core.Ordered;
import cn.taketoday.framework.diagnostics.AbstractFailureAnalyzer;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.framework.diagnostics.FailureAnalyzer;
import cn.taketoday.web.server.context.MissingWebServerFactoryBeanException;
import cn.taketoday.web.server.reactive.server.ReactiveWebServerFactory;

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
  protected FailureAnalysis analyze(Throwable rootFailure, MissingWebServerFactoryBeanException cause) {
    return new FailureAnalysis(
            "Web application could not be started as there was no " + cause.getBeanType().getName()
                    + " bean defined in the context.",
            "Check your application's dependencies for a supported "
                    + getApplicationType(cause) + " web server.\n"
                    + "Check the configured web application type.",
            cause);
  }

  private String getApplicationType(MissingWebServerFactoryBeanException cause) {
    if (cause.getWebServerFactoryClass() == ReactiveWebServerFactory.class) {
      return "reactive_web";
    }
    return "netty_web";
  }

  @Override
  public int getOrder() {
    return 0;
  }

}

