/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.context;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.framework.diagnostics.FailureAnalysis;
import cn.taketoday.framework.web.reactive.context.ReactiveWebServerApplicationContext;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.servlet.context.ServletWebServerApplicationContext;
import cn.taketoday.framework.web.servlet.server.ServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 21:27
 */
class MissingWebServerFactoryBeanFailureAnalyzerTests {

  @Test
  void missingServletWebServerFactoryBeanFailure() {
    ApplicationContextException failure = createFailure(new ServletWebServerApplicationContext());
    assertThat(failure).isNotNull();
    FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(failure);
    assertThat(analysis).isNotNull();
    assertThat(analysis.getDescription()).isEqualTo("Web application could not be started as there was no "
            + ServletWebServerFactory.class.getName() + " bean defined in the context.");
    assertThat(analysis.getAction()).isEqualTo(
            "Check your application's dependencies for a supported servlet_web web server.\nCheck the configured web "
                    + "application type.");
  }

  @Test
  void missingReactiveWebServerFactoryBeanFailure() {
    ApplicationContextException failure = createFailure(new ReactiveWebServerApplicationContext());
    FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(failure);
    assertThat(analysis).isNotNull();
    assertThat(analysis.getDescription())
            .isEqualTo("Web application could not be started as there was no " + ReactiveWebServerFactory.class.getName() + " bean defined in the context.");
    assertThat(analysis.getAction())
            .isEqualTo(
                    "Check your application's dependencies for a supported reactive_web web server.\nCheck the configured web "
                            + "application type.");
  }

  private ApplicationContextException createFailure(ConfigurableApplicationContext context) {
    try {
      context.refresh();
      context.close();
      return null;
    }
    catch (ApplicationContextException ex) {
      return ex;
    }
  }

}
