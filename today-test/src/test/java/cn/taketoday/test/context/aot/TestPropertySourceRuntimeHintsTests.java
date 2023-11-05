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

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.stream.Stream;

import cn.taketoday.aot.generate.InMemoryGeneratedFiles;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.env.YamlTestProperties;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for registering run-time hints for {@code @TestPropertySource}, tested
 * via the {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TestPropertySourceRuntimeHintsTests extends AbstractAotTests {

  private final RuntimeHints runtimeHints = new RuntimeHints();

  private final TestContextAotGenerator generator =
          new TestContextAotGenerator(new InMemoryGeneratedFiles(), this.runtimeHints);

  @Test
  void testPropertySourceWithClassPathStarLocationPattern() {
    Class<?> testClass = ClassPathStarLocationPatternTestCase.class;

    // We can effectively only assert that an exception is not thrown; however,
    // a WARN-level log message similar to the following should be logged.
    //
    // Runtime hint registration is not supported for the 'classpath*:' prefix or
    // wildcards in @TestPropertySource locations. Please manually register a resource
    // hint for each location represented by 'classpath*:**/aot/samples/basic/test?.yaml'.
    assertThatNoException().isThrownBy(() -> this.generator.processAheadOfTime(Stream.of(testClass)));

    // But we can also ensure that a resource hint was not registered.
    assertThat(resource("cn/taketoday/test/context/aot/samples/basic/test1.yaml")).rejects(runtimeHints);
  }

  @Test
  void testPropertySourceWithWildcardLocationPattern() {
    Class<?> testClass = WildcardLocationPatternTestCase.class;

    // We can effectively only assert that an exception is not thrown; however,
    // a WARN-level log message similar to the following should be logged.
    //
    // Runtime hint registration is not supported for the 'classpath*:' prefix or
    // wildcards in @TestPropertySource locations. Please manually register a resource
    // hint for each location represented by 'classpath:cn/taketoday/test/context/aot/samples/basic/test?.yaml'.
    assertThatNoException().isThrownBy(() -> this.generator.processAheadOfTime(Stream.of(testClass)));

    // But we can also ensure that a resource hint was not registered.
    assertThat(resource("cn/taketoday/test/context/aot/samples/basic/test1.yaml")).rejects(runtimeHints);
  }

  private static Predicate<RuntimeHints> resource(String location) {
    return RuntimeHintsPredicates.resource().forResource(location);
  }

  @JUnitConfig(Config.class)
  @YamlTestProperties("classpath*:**/aot/samples/basic/test?.yaml")
  static class ClassPathStarLocationPatternTestCase {
  }

  @JUnitConfig(Config.class)
  @YamlTestProperties("classpath:cn/taketoday/test/context/aot/samples/basic/test?.yaml")
  static class WildcardLocationPatternTestCase {
  }

  @Configuration
  static class Config {
  }

}
