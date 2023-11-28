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

package cn.taketoday.framework.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/14 17:18
 */
@ExtendWith(MockitoExtension.class)
class TemplateAvailabilityProvidersTests {

  private TemplateAvailabilityProviders providers;

  @Mock
  private TemplateAvailabilityProvider provider;

  private final String view = "view";

  private final ClassLoader classLoader = getClass().getClassLoader();

  private final MockEnvironment environment = new MockEnvironment();

  @Mock
  private ResourceLoader resourceLoader;

  @BeforeEach
  void setup() {
    this.providers = new TemplateAvailabilityProviders(Collections.singleton(this.provider));
  }

  @Test
  void createWhenApplicationContextIsNull() {
    TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders((ApplicationContext) null);
    assertThat(providers.getProviders()).isNotEmpty();
  }

  @Test
  void createWhenUsingApplicationContextShouldLoadProviders() {
    ApplicationContext applicationContext = mock(ApplicationContext.class);
    given(applicationContext.getClassLoader()).willReturn(this.classLoader);
    TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(applicationContext);
    assertThat(providers.getProviders()).isNotEmpty();
    then(applicationContext).should().getClassLoader();
  }

  @Test
  void createWhenClassLoaderIsNull() {
    TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders((ClassLoader) null);
    assertThat(providers.getProviders()).isNotEmpty();
  }

  @Test
  void createWhenUsingClassLoaderShouldLoadProviders() {
    TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(this.classLoader);
    assertThat(providers.getProviders()).isNotEmpty();
  }

  @Test
  void createWhenProvidersIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new TemplateAvailabilityProviders((Collection<TemplateAvailabilityProvider>) null))
            .withMessageContaining("Providers is required");
  }

  @Test
  void createWhenUsingProvidersShouldUseProviders() {
    TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(
            Collections.singleton(this.provider));
    assertThat(providers.getProviders()).containsOnly(this.provider);
  }

  @Test
  void getProviderWhenApplicationContextIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.providers.getProvider(this.view, null))
            .withMessageContaining("ApplicationContext is required");
  }

  @Test
  void getProviderWhenViewIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(
                    () -> this.providers.getProvider(null, this.environment, this.classLoader, this.resourceLoader))
            .withMessageContaining("View is required");
  }

  @Test
  void getProviderWhenEnvironmentIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.providers.getProvider(this.view, null, this.classLoader, this.resourceLoader))
            .withMessageContaining("Environment is required");
  }

  @Test
  void getProviderWhenClassLoaderIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.providers.getProvider(this.view, this.environment, null, this.resourceLoader))
            .withMessageContaining("ClassLoader is required");
  }

  @Test
  void getProviderWhenResourceLoaderIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.providers.getProvider(this.view, this.environment, this.classLoader, null))
            .withMessageContaining("ResourceLoader is required");
  }

  @Test
  void getProviderWhenNoneMatchShouldReturnNull() {
    TemplateAvailabilityProvider found = this.providers.getProvider(this.view, this.environment, this.classLoader,
            this.resourceLoader);
    assertThat(found).isNull();
    then(this.provider).should().isTemplateAvailable(this.view, this.environment, this.classLoader,
            this.resourceLoader);
  }

  @Test
  void getProviderWhenMatchShouldReturnProvider() {
    given(this.provider.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader))
            .willReturn(true);
    TemplateAvailabilityProvider found = this.providers.getProvider(this.view, this.environment, this.classLoader,
            this.resourceLoader);
    assertThat(found).isSameAs(this.provider);

  }

  @Test
  void getProviderShouldCacheMatchResult() {
    given(this.provider.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader))
            .willReturn(true);
    this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
    this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
    then(this.provider).should().isTemplateAvailable(this.view, this.environment, this.classLoader,
            this.resourceLoader);
  }

  @Test
  void getProviderShouldCacheNoMatchResult() {
    this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
    this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
    then(this.provider).should().isTemplateAvailable(this.view, this.environment, this.classLoader,
            this.resourceLoader);
  }

  @Test
  void getProviderWhenCacheDisabledShouldNotUseCache() {
    given(this.provider.isTemplateAvailable(this.view, this.environment, this.classLoader, this.resourceLoader))
            .willReturn(true);
    this.environment.setProperty("infra.template.provider.cache", "false");
    this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
    this.providers.getProvider(this.view, this.environment, this.classLoader, this.resourceLoader);
    then(this.provider).should(times(2)).isTemplateAvailable(this.view, this.environment, this.classLoader,
            this.resourceLoader);
  }

}