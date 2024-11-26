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

package infra.annotation.config.context;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.app.test.context.InfraTest;
import infra.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 */
@DirtiesContext
@InfraTest("infra.messages.basename:test/messages")
@ImportAutoConfiguration({
        MessageSourceAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class
})
class MessageSourceAutoConfigurationIntegrationTests {

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
