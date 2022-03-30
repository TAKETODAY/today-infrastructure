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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.DefaultBootstrapContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Nguyen Bao Sach
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataEnvironmentPostProcessorTests {

  private StandardEnvironment environment = new StandardEnvironment();

  private Application application = new Application();

  @Mock
  private ConfigDataEnvironment configDataEnvironment;

  @Spy
  private ConfigDataEnvironmentPostProcessor postProcessor = new ConfigDataEnvironmentPostProcessor(
          new DefaultBootstrapContext());

  @Captor
  private ArgumentCaptor<Set<String>> additionalProfilesCaptor;

  @Captor
  private ArgumentCaptor<ResourceLoader> resourceLoaderCaptor;

  @Test
  void postProcessEnvironmentWhenNoLoaderCreatesDefaultLoaderInstance() {
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should().getConfigDataEnvironment(any(), this.resourceLoaderCaptor.capture(), any());
    then(this.configDataEnvironment).should().processAndApply();
    assertThat(this.resourceLoaderCaptor.getValue()).isInstanceOf(DefaultResourceLoader.class);
  }

  @Test
  void postProcessEnvironmentWhenCustomLoaderUsesSpecifiedLoaderInstance() {
    ResourceLoader resourceLoader = mock(ResourceLoader.class);
    this.application.setResourceLoader(resourceLoader);
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should().getConfigDataEnvironment(any(), this.resourceLoaderCaptor.capture(), any());
    then(this.configDataEnvironment).should().processAndApply();
    assertThat(this.resourceLoaderCaptor.getValue()).isSameAs(resourceLoader);
  }

  @Test
  void postProcessEnvironmentWhenHasAdditionalProfilesOnSpringApplicationUsesAdditionalProfiles() {
    this.application.setAdditionalProfiles("dev");
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should().getConfigDataEnvironment(any(), any(),
            this.additionalProfilesCaptor.capture());
    then(this.configDataEnvironment).should().processAndApply();
    assertThat(this.additionalProfilesCaptor.getValue()).containsExactly("dev");
  }

  @Test
  void postProcessEnvironmentWhenNoActiveProfiles() {
    willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
    this.postProcessor.postProcessEnvironment(this.environment, this.application);
    then(this.postProcessor).should().getConfigDataEnvironment(any(), this.resourceLoaderCaptor.capture(), any());
    then(this.configDataEnvironment).should().processAndApply();
    assertThat(this.environment.getActiveProfiles()).isEmpty();
  }

  @Test
  void applyToAppliesPostProcessing() {
    int before = this.environment.getPropertySources().size();
    TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
    ConfigDataEnvironmentPostProcessor.applyTo(this.environment, null, null, Collections.singleton("dev"),
            listener);
    assertThat(this.environment.getPropertySources().size()).isGreaterThan(before);
    assertThat(this.environment.getActiveProfiles()).containsExactly("dev");
    assertThat(listener.getAddedPropertySources()).hasSizeGreaterThan(0);
    assertThat(listener.getProfiles().getActive()).containsExactly("dev");
    assertThat(listener.getAddedPropertySources().stream().anyMatch((added) -> hasDevProfile(added.getResource())))
            .isTrue();
  }

  private boolean hasDevProfile(ConfigDataResource resource) {
    return (resource instanceof StandardConfigDataResource)
            && "dev".equals(((StandardConfigDataResource) resource).getProfile());
  }

}
