/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.condition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.annotation.MergedAnnotation;
import infra.core.type.AnnotatedTypeMetadata;

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
    MergedAnnotation<ConditionalOnJndi> annotation = MergedAnnotation.valueOf(ConditionalOnJndi.class, attributes);
    given(metadata.getAnnotation(ConditionalOnJndi.class)).willReturn(annotation);
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
