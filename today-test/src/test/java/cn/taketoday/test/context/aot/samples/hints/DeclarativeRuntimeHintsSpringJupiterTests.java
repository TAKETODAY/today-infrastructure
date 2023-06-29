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

package cn.taketoday.test.context.aot.samples.hints;

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.aot.hint.annotation.RegisterReflectionForBinding;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportRuntimeHints;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@Reflective
@RegisterReflectionForBinding(DeclarativeRuntimeHintsSpringJupiterTests.SampleClassWithGetter.class)
@ImportRuntimeHints(DeclarativeRuntimeHintsSpringJupiterTests.DemoHints.class)
public class DeclarativeRuntimeHintsSpringJupiterTests {

  @Test
  void test(@Autowired String foo) {
    assertThat(foo).isEqualTo("bar");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    String foo() {
      return "bar";
    }
  }

  static class DemoHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources().registerPattern("org/example/config/*.txt");
    }

  }

  public static class SampleClassWithGetter {

    public String getName() {
      return null;
    }
  }

}
