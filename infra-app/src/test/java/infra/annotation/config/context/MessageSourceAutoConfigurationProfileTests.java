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

package infra.annotation.config.context;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.annotation.config.web.RandomPortWebServerConfig;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.test.annotation.DirtiesContext;
import infra.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 */
@DirtiesContext
@InfraTest
@ActiveProfiles("switch-messages")
@ImportAutoConfiguration({
        RandomPortWebServerConfig.class,
        MessageSourceAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class
})
class MessageSourceAutoConfigurationProfileTests {

  @Autowired
  private ApplicationContext context;

  @Test
  @Disabled
  void testMessageSourceFromPropertySourceAnnotation() {
    assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

}
