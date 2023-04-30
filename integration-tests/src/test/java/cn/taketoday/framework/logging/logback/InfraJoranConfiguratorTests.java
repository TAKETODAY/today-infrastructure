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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import cn.taketoday.context.properties.source.ConfigurationPropertySources;
import cn.taketoday.framework.logging.LoggingStartupContext;
import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraJoranConfigurator}.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class InfraJoranConfiguratorTests {

  private MockEnvironment environment;

  private LoggingStartupContext initializationContext;

  private JoranConfigurator configurator;

  private LoggerContext context;

  private Logger logger;

  private CapturedOutput output;

  @BeforeEach
  void setup(CapturedOutput output) {
    this.output = output;
    this.environment = new MockEnvironment();
    this.initializationContext = new LoggingStartupContext(this.environment);
    this.configurator = new InfraJoranConfigurator(this.initializationContext);
    this.context = (LoggerContext) LoggerFactory.getILoggerFactory();
    this.logger = this.context.getLogger(getClass());
  }

  @AfterEach
  void reset() {
    this.context.stop();
    new BasicConfigurator().configure((LoggerContext) LoggerFactory.getILoggerFactory());
  }

  @Test
  void profileActive() throws Exception {
    this.environment.setActiveProfiles("production");
    initialize("production-profile.xml");
    this.logger.trace("Hello");
    assertThat(this.output).contains("Hello");
  }

  @Test
  void multipleNamesFirstProfileActive() throws Exception {
    this.environment.setActiveProfiles("production");
    initialize("multi-profile-names.xml");
    this.logger.trace("Hello");
    assertThat(this.output).contains("Hello");
  }

  @Test
  void multipleNamesSecondProfileActive() throws Exception {
    this.environment.setActiveProfiles("test");
    initialize("multi-profile-names.xml");
    this.logger.trace("Hello");
    assertThat(this.output).contains("Hello");
  }

  @Test
  void profileNotActive() throws Exception {
    initialize("production-profile.xml");
    this.logger.trace("Hello");
    assertThat(this.output).doesNotContain("Hello");
  }

  @Test
  void profileExpressionMatchFirst() throws Exception {
    this.environment.setActiveProfiles("production");
    initialize("profile-expression.xml");
    this.logger.trace("Hello");
    assertThat(this.output).contains("Hello");
  }

  @Test
  void profileExpressionMatchSecond() throws Exception {
    this.environment.setActiveProfiles("test");
    initialize("profile-expression.xml");
    this.logger.trace("Hello");
    assertThat(this.output).contains("Hello");
  }

  @Test
  void profileExpressionNoMatch() throws Exception {
    this.environment.setActiveProfiles("development");
    initialize("profile-expression.xml");
    this.logger.trace("Hello");
    assertThat(this.output).doesNotContain("Hello");
  }

  @Test
  void profileNestedActiveActive() throws Exception {
    doTestNestedProfile(true, "outer", "inner");
  }

  @Test
  void profileNestedActiveNotActive() throws Exception {
    doTestNestedProfile(false, "outer");
  }

  @Test
  void profileNestedNotActiveActive() throws Exception {
    doTestNestedProfile(false, "inner");
  }

  @Test
  void profileNestedNotActiveNotActive() throws Exception {
    doTestNestedProfile(false);
  }

  @Test
  void infraProperty() throws Exception {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "my.example-property=test");
    initialize("property.xml");
    assertThat(this.context.getProperty("MINE")).isEqualTo("test");
  }

  @Test
  void relaxedInfraProperty() throws Exception {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "my.EXAMPLE_PROPERTY=test");
    ConfigurationPropertySources.attach(this.environment);
    initialize("property.xml");
    assertThat(this.context.getProperty("MINE")).isEqualTo("test");
  }

  @Test
  void infraPropertyNoValue() throws Exception {
    initialize("property.xml");
    assertThat(this.context.getProperty("SIMPLE")).isNull();
  }

  @Test
  void relaxedInfraPropertyNoValue() throws Exception {
    initialize("property.xml");
    assertThat(this.context.getProperty("MINE")).isNull();
  }

  @Test
  void infraPropertyWithDefaultValue() throws Exception {
    initialize("property-default-value.xml");
    assertThat(this.context.getProperty("SIMPLE")).isEqualTo("foo");
  }

  @Test
  void relaxedInfraPropertyWithDefaultValue() throws Exception {
    initialize("property-default-value.xml");
    assertThat(this.context.getProperty("MINE")).isEqualTo("bar");
  }

  @Test
  void infraPropertyInIfWhenTrue() throws Exception {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "my.example-property=true");
    initialize("property-in-if.xml");
    assertThat(this.context.getProperty("MYCHECK")).isEqualTo("i-was-included");
  }

  @Test
  void infraPropertyInIfWhenFalse() throws Exception {
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "my.example-property=false");
    initialize("property-in-if.xml");
    assertThat(this.context.getProperty("MYCHECK")).isNull();
  }

  private void doTestNestedProfile(boolean expected, String... profiles) throws JoranException {
    this.environment.setActiveProfiles(profiles);
    initialize("nested.xml");
    this.logger.trace("Hello");
    if (expected) {
      assertThat(this.output).contains("Hello");
    }
    else {
      assertThat(this.output).doesNotContain("Hello");
    }

  }

  private void initialize(String config) throws JoranException {
    this.configurator.setContext(this.context);
    this.configurator.doConfigure(getClass().getResourceAsStream(config));
  }

}
