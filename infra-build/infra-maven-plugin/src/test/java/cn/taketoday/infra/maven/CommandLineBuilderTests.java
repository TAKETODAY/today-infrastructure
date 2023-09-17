/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.infra.maven;

import org.junit.jupiter.api.Test;

import java.util.Map;

import cn.taketoday.infra.maven.sample.ClassWithMainMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommandLineBuilder}.
 *
 * @author Stephane Nicoll
 */
class CommandLineBuilderTests {

  public static final String CLASS_NAME = ClassWithMainMethod.class.getName();

  @Test
  void buildWithNullJvmArgumentsIsIgnored() {
    assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withJvmArguments((String[]) null).build())
            .containsExactly(CLASS_NAME);
  }

  @Test
  void buildWithNullIntermediateJvmArgumentIsIgnored() {
    assertThat(CommandLineBuilder.forMainClass(CLASS_NAME)
            .withJvmArguments("-verbose:class", null, "-verbose:gc")
            .build()).containsExactly("-verbose:class", "-verbose:gc", CLASS_NAME);
  }

  @Test
  void buildWithJvmArgument() {
    assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withJvmArguments("-verbose:class").build())
            .containsExactly("-verbose:class", CLASS_NAME);
  }

  @Test
  void buildWithNullSystemPropertyIsIgnored() {
    assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withSystemProperties(null).build())
            .containsExactly(CLASS_NAME);
  }

  @Test
  void buildWithSystemProperty() {
    assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withSystemProperties(Map.of("flag", "enabled")).build())
            .containsExactly("-Dflag=\"enabled\"", CLASS_NAME);
  }

  @Test
  void buildWithNullArgumentsIsIgnored() {
    assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withArguments((String[]) null).build())
            .containsExactly(CLASS_NAME);
  }

  @Test
  void buildWithNullIntermediateArgumentIsIgnored() {
    assertThat(CommandLineBuilder.forMainClass(CLASS_NAME).withArguments("--test", null, "--another").build())
            .containsExactly(CLASS_NAME, "--test", "--another");
  }

}
