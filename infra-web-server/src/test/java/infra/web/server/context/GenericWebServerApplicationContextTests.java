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

package infra.web.server.context;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContextException;
import infra.web.server.GenericWebServerFactory;
import infra.web.server.MissingWebServerFactoryBeanException;
import infra.web.server.MockWebServerFactory;
import infra.web.server.WebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class GenericWebServerApplicationContextTests {

  @Test
  void refreshCreatesAndStartsWebServer() {
    WebServer webServer = mock(WebServer.class, CALLS_REAL_METHODS);
    try (GenericWebServerApplicationContext context = new GenericWebServerApplicationContext()) {
      context.getBeanFactory().registerSingleton("webServerFactory", (GenericWebServerFactory) () -> webServer);
      context.setServerNamespace("main");
      context.refresh();

      assertThat(context.getWebServer()).isSameAs(webServer);
      assertThat(context.getServerNamespace()).isEqualTo("main");
      then(webServer).should().start();
    }
    then(webServer).should().destroy();
  }

  @Test
  void refreshFailsWhenWebServerFactoryIsMissing() {
    GenericWebServerApplicationContext context = new GenericWebServerApplicationContext();

    assertThatThrownBy(context::refresh)
            .isInstanceOf(ApplicationContextException.class)
            .hasRootCauseInstanceOf(MissingWebServerFactoryBeanException.class);
  }

  @Test
  void refreshFailsWhenMultipleWebServerFactoriesAreDefined() {
    GenericWebServerApplicationContext context = new GenericWebServerApplicationContext();
    context.registerBean("first", MockWebServerFactory.class);
    context.registerBean("second", MockWebServerFactory.class);

    assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(context::refresh)
            .havingCause().isInstanceOf(ApplicationContextException.class)
            .withMessageContaining("multiple WebServerFactory beans")
            .withMessageContaining("first,second");
  }

  @Test
  void refreshStopsCreatedWebServerWhenLaterBeanFails() {
    WebServer webServer = mock(WebServer.class);
    try (GenericWebServerApplicationContext context = new GenericWebServerApplicationContext()) {
      context.getBeanFactory().registerSingleton("webServerFactory", (GenericWebServerFactory) () -> webServer);
      context.registerBean("failingBean", FailingBean.class);

      assertThatThrownBy(context::refresh).isInstanceOf(BeanCreationException.class)
              .hasRootCauseMessage("expected failure");
      then(webServer).should().stop();
      then(webServer).shouldHaveNoMoreInteractions();
    }
  }

  @Test
  void standardBeanFactoryConstructorIsSupported() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    try (GenericWebServerApplicationContext context = new GenericWebServerApplicationContext(beanFactory)) {
      assertThat(context.getBeanFactory()).isSameAs(beanFactory);
      assertThat(context.getWebServer()).isNull();
    }
  }

  private static final class FailingBean {

    private FailingBean() {
      throw new IllegalStateException("expected failure");
    }

  }

}
