/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionalOnJndi @ConditionalOnJndi}
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ConditionalOnJndiTests {

  private ClassLoader threadContextClassLoader;

  private String initialContextFactory;

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  private MockableOnJndi condition = new MockableOnJndi();

  @BeforeEach
  void setupThreadContextClassLoader() {
    this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(new JndiPropertiesHidingClassLoader(getClass().getClassLoader()));
  }

  @AfterEach
  void close() {
    TestableInitialContextFactory.clearAll();
    if (this.initialContextFactory != null) {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, this.initialContextFactory);
    }
    else {
      System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
    }
    Thread.currentThread().setContextClassLoader(this.threadContextClassLoader);
  }

  @Test
  void jndiNotAvailable() {
    this.contextRunner.withUserConfiguration(JndiAvailableConfiguration.class, JndiConditionConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(String.class));
  }

  @Test
  void jndiAvailable() {
    setupJndi();
    this.contextRunner.withUserConfiguration(JndiAvailableConfiguration.class, JndiConditionConfiguration.class)
            .run((context) -> assertThat(context).hasSingleBean(String.class));
  }

  @Test
  void jndiLocationNotBound() {
    setupJndi();
    this.contextRunner.withUserConfiguration(JndiConditionConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(String.class));
  }

  @Test
  void jndiLocationBound() {
    setupJndi();
    TestableInitialContextFactory.bind("java:/FooManager", new Object());
    this.contextRunner.withUserConfiguration(JndiConditionConfiguration.class)
            .run((context) -> assertThat(context).hasSingleBean(String.class));
  }

  @Test
  void jndiLocationNotFound() {
    ConditionOutcome outcome = this.condition.getMatchOutcome(null, mockMetaData("java:/a"));
    assertThat(outcome.isMatch()).isFalse();
  }

  @Test
  void jndiLocationFound() {
    this.condition.setFoundLocation("java:/b");
    ConditionOutcome outcome = this.condition.getMatchOutcome(null, mockMetaData("java:/a", "java:/b"));
    assertThat(outcome.isMatch()).isTrue();
  }

  private void setupJndi() {
    this.initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestableInitialContextFactory.class.getName());
  }

  private AnnotatedTypeMetadata mockMetaData(String... value) {
    AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("value", value);
    given(metadata.getAnnotationAttributes(ConditionalOnJndi.class.getName())).willReturn(attributes);
    return metadata;
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnJndi
  static class JndiAvailableConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnJndi("java:/FooManager")
  static class JndiConditionConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  static class MockableOnJndi extends OnJndiCondition {

    private boolean jndiAvailable = true;

    private String foundLocation;

    @Override
    protected boolean isJndiAvailable() {
      return this.jndiAvailable;
    }

    @Override
    protected JndiLocator getJndiLocator(String[] locations) {
      return new JndiLocator(locations) {
        @Override
        public String lookupFirstLocation() {
          return MockableOnJndi.this.foundLocation;
        }
      };
    }

    void setFoundLocation(String foundLocation) {
      this.foundLocation = foundLocation;
    }

  }

}
