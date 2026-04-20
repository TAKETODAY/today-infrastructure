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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import infra.app.Application;
import infra.app.DefaultBootstrapContext;
import infra.app.TestApplicationEnvironment;
import infra.app.context.config.ConfigData.Options;
import infra.core.env.PropertySource;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.test.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Nguyen Bao Sach
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataEnvironmentPostProcessorTests {

  private final TestApplicationEnvironment environment = new TestApplicationEnvironment();

  private final Application application = new Application();

  private @Nullable ConfigDataEnvironment configDataEnvironment;

  private @Nullable ConfigDataEnvironmentPostProcessor postProcessor;

  @Test
  void postProcessEnvironmentWhenNoLoaderCreatesDefaultLoaderInstance() {
    setupMocksAndSpies();
    assertThat(this.configDataEnvironment).isNotNull();
    assertThat(this.postProcessor).isNotNull();
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should()
            .getConfigDataEnvironment(any(),
                    assertArg((resourceLoader) -> assertThat(resourceLoader).isInstanceOf(DefaultResourceLoader.class)),
                    any());
    then(this.configDataEnvironment).should().processAndApply();
  }

  @Test
  void postProcessEnvironmentWhenCustomLoaderUsesSpecifiedLoaderInstance() {
    setupMocksAndSpies();
    assertThat(this.configDataEnvironment).isNotNull();
    assertThat(this.postProcessor).isNotNull();
    ResourceLoader resourceLoader = mock(ResourceLoader.class);
    this.application.setResourceLoader(resourceLoader);
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should()
            .getConfigDataEnvironment(any(),
                    assertArg((resourceLoaderB) -> assertThat(resourceLoaderB).isSameAs(resourceLoader)), any());
    then(this.configDataEnvironment).should().processAndApply();
  }

  @Test
  void postProcessEnvironmentWhenHasAdditionalProfilesOnApplicationUsesAdditionalProfiles() {
    setupMocksAndSpies();
    assertThat(this.configDataEnvironment).isNotNull();
    assertThat(this.postProcessor).isNotNull();
    this.application.setAdditionalProfiles("dev");
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should()
            .getConfigDataEnvironment(any(), any(),
                    assertArg((additionalProperties) -> assertThat(additionalProperties).containsExactly("dev")));
    then(this.configDataEnvironment).should().processAndApply();
  }

  @Test
  void postProcessEnvironmentWhenNoActiveProfiles() {
    setupMocksAndSpies();
    assertThat(this.configDataEnvironment).isNotNull();
    assertThat(this.postProcessor).isNotNull();
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should().getConfigDataEnvironment(any(), any(ResourceLoader.class), any());
    then(this.configDataEnvironment).should().processAndApply();
    assertThat(this.environment.getActiveProfiles()).isEmpty();
  }

  @Test
  @WithResource(name = "application.properties", content = "property=value")
  @WithResource(name = "application-dev.properties", content = "property=dev-value")
  void applyToAppliesPostProcessing() {
    int before = this.environment.getPropertySources().size();
    TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
    ConfigDataEnvironmentPostProcessor.applyTo(this.environment, null, null, Collections.singleton("dev"),
            listener);
    assertThat(this.environment.getPropertySources()).hasSizeGreaterThan(before);
    assertThat(this.environment.getActiveProfiles()).containsExactly("dev");
    assertThat(listener.getAddedPropertySources()).isNotEmpty();
    Profiles profiles = listener.getProfiles();
    assertThat(profiles).isNotNull();
    assertThat(profiles.getActive()).containsExactly("dev");
    assertThat(listener.getAddedPropertySources().stream().anyMatch((added) -> hasDevProfile(added.getResource())))
            .isTrue();
  }

  @Test
  @WithResource(name = "application.properties", content = """
          infra.profiles.active=dev
          property=value
          #---
          app.config.activate.on-profile=dev
          property=dev-value1
          """)
  @WithResource(name = "application-dev.properties", content = "property=dev-value2")
  void applyToCanOverrideConfigDataOptions() {
    ConfigDataEnvironmentUpdateListener listener = new ConfigDataEnvironmentUpdateListener() {

      @Override
      public Options onConfigDataOptions(ConfigData configData, PropertySource<?> propertySource,
              Options options) {
        return options.with(ConfigData.Option.IGNORE_PROFILES);
      }

    };
    ConfigDataEnvironmentPostProcessor.applyTo(this.environment, null, null, Collections.emptyList(), listener);
    assertThat(this.environment.getProperty("property")).isEqualTo("value");
    assertThat(this.environment.getActiveProfiles()).isEmpty();
  }

  private void setupMocksAndSpies() {
    this.configDataEnvironment = mock(ConfigDataEnvironment.class);
    this.postProcessor = spy(new ConfigDataEnvironmentPostProcessor(new DefaultBootstrapContext()));
  }

  private boolean hasDevProfile(@Nullable ConfigDataResource resource) {
    return (resource instanceof StandardConfigDataResource standardResource)
            && "dev".equals(standardResource.getProfile());
  }

}
