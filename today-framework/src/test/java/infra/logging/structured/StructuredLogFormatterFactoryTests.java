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

package infra.logging.structured;

import org.junit.jupiter.api.Test;

import infra.core.env.Environment;
import infra.logging.structured.StructuredLogFormatterFactory.CommonFormatters;
import infra.mock.env.MockEnvironment;
import infra.util.Instantiator.AvailableParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StructuredLogFormatterFactory}.
 *
 * @author Phillip Webb
 */
class StructuredLogFormatterFactoryTests {

  private final StructuredLogFormatterFactory<LogEvent> factory;

  private final MockEnvironment environment = new MockEnvironment();

  StructuredLogFormatterFactoryTests() {
    this.environment.setProperty("logging.structured.ecs.service.version", "1.2.3");
    this.factory = new StructuredLogFormatterFactory<>(LogEvent.class, this.environment,
            this::addAvailableParameters, this::addCommonFormatters);
  }

  private void addAvailableParameters(AvailableParameters availableParameters) {
    availableParameters.add(StringBuilder.class, new StringBuilder("Hello"));
  }

  private void addCommonFormatters(CommonFormatters<LogEvent> commonFormatters) {
    commonFormatters.add(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA,
            (instantiator) -> new TestEcsFormatter(instantiator.getArg(Environment.class),
                    instantiator.getArg(StringBuilder.class)));
  }

  @Test
  void getUsingCommonFormat() {
    assertThat(this.factory.get("ecs")).isInstanceOf(TestEcsFormatter.class);
  }

  @Test
  void getUsingClassName() {
    assertThat(this.factory.get(ExtendedTestEcsFormatter.class.getName()))
            .isInstanceOf(ExtendedTestEcsFormatter.class);
  }

  @Test
  void getUsingClassNameWhenNoSuchClass() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> assertThat(this.factory.get("com.example.WeMadeItUp")).isNull())
            .withMessage("Unknown format 'com.example.WeMadeItUp'. "
                    + "Values can be a valid fully-qualified class name or one of the common formats: [ecs]");
  }

  @Test
  void getUsingClassNameWhenHasGenericMismatch() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.factory.get(DifferentFormatter.class.getName()))
            .withMessage("Type argument of infra.logging.structured."
                    + "StructuredLogFormatterFactoryTests$DifferentFormatter "
                    + "must be infra.logging.structured."
                    + "StructuredLogFormatterFactoryTests$LogEvent "
                    + "but was infra.logging.structured."
                    + "StructuredLogFormatterFactoryTests$DifferentLogEvent");
  }

  @Test
  void getUsingClassNameInjectsApplicationMetadata() {
    TestEcsFormatter formatter = (TestEcsFormatter) this.factory.get(TestEcsFormatter.class.getName());
    assertThat(formatter.getEnvironment()).isSameAs(this.environment);
  }

  @Test
  void getUsingClassNameInjectsCustomParameter() {
    TestEcsFormatter formatter = (TestEcsFormatter) this.factory.get(TestEcsFormatter.class.getName());
    assertThat(formatter.getCustom()).hasToString("Hello");
  }

  static class LogEvent {

  }

  static class DifferentLogEvent {

  }

  static class TestEcsFormatter implements StructuredLogFormatter<LogEvent> {

    private Environment environment;

    private StringBuilder custom;

    TestEcsFormatter(Environment environment, StringBuilder custom) {
      this.environment = environment;
      this.custom = custom;
    }

    @Override
    public String format(LogEvent event) {
      return "formatted " + this.environment.getProperty("logging.structured.ecs.service.version");
    }

    Environment getEnvironment() {
      return this.environment;
    }

    StringBuilder getCustom() {
      return this.custom;
    }

  }

  static class ExtendedTestEcsFormatter extends TestEcsFormatter {

    ExtendedTestEcsFormatter(Environment environment, StringBuilder custom) {
      super(environment, custom);
    }

  }

  static class DifferentFormatter implements StructuredLogFormatter<DifferentLogEvent> {

    @Override
    public String format(DifferentLogEvent event) {
      return "";
    }

  }

}
