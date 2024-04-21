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

package cn.taketoday.annotation.config.context;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import cn.taketoday.annotation.config.web.RandomPortWebServerConfig;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.ImportAutoConfiguration;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.ActiveProfiles;

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
