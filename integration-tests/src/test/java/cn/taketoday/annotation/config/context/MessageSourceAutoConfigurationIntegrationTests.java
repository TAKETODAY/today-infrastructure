/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.context;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.ImportAutoConfiguration;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.test.annotation.DirtiesContext;

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
  void testMessageSourceFromPropertySourceAnnotation() {
    assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

}
