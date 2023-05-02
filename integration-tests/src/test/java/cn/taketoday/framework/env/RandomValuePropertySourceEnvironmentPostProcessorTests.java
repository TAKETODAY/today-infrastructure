/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.env;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import cn.taketoday.framework.Application;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentPostProcessor;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RandomValuePropertySourceEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 */
class RandomValuePropertySourceEnvironmentPostProcessorTests {

  private final RandomValuePropertySourceEnvironmentPostProcessor postProcessor =
          new RandomValuePropertySourceEnvironmentPostProcessor();

  @Test
  void getOrderIsBeforeConfigData() {
    assertThat(this.postProcessor.getOrder()).isLessThan(ConfigDataEnvironmentPostProcessor.ORDER);
  }

  @Test
  void postProcessEnvironmentAddsPropertySource() {
    MockEnvironment environment = new MockEnvironment();
    this.postProcessor.postProcessEnvironment(environment, mock(Application.class));
    Assertions.assertThat(environment.getProperty("random.string")).isNotNull();
  }

}
