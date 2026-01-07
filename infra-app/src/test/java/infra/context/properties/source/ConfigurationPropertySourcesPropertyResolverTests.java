/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import infra.core.TypeDescriptor;
import infra.core.conversion.ConversionFailedException;
import infra.core.env.ConfigurablePropertyResolver;
import infra.core.env.PropertySources;
import infra.core.env.StandardEnvironment;
import infra.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigurationPropertySourcesPropertyResolver}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertySourcesPropertyResolverTests {

  @Test
  void standardPropertyResolverResolvesMultipleTimes() {
    StandardEnvironment environment = new StandardEnvironment();
    CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
    environment.getProperty("missing");
    assertThat(propertySource.getCount("missing")).isEqualTo(2);
  }

  @Test
  void configurationPropertySourcesPropertyResolverResolvesSingleTime() {
    ResolverEnvironment environment = new ResolverEnvironment();
    CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
    environment.getProperty("missing");
    assertThat(propertySource.getCount("missing")).isOne();
  }

  @Test
  void containsPropertyWhenValidConfigurationPropertyName() {
    ResolverEnvironment environment = new ResolverEnvironment();
    CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
    assertThat(environment.containsProperty("spring")).isTrue();
    assertThat(environment.containsProperty("sprong")).isFalse();
    assertThat(propertySource.getCount("spring")).isOne();
    assertThat(propertySource.getCount("sprong")).isOne();
  }

  @Test
  void containsPropertyWhenNotValidConfigurationPropertyName() {
    ResolverEnvironment environment = new ResolverEnvironment();
    CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
    assertThat(environment.containsProperty("spr!ng")).isTrue();
    assertThat(environment.containsProperty("spr*ng")).isFalse();
    assertThat(propertySource.getCount("spr!ng")).isOne();
    assertThat(propertySource.getCount("spr*ng")).isOne();
  }

  @Test
  void getPropertyWhenValidConfigurationPropertyName() {
    ResolverEnvironment environment = new ResolverEnvironment();
    CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
    assertThat(environment.getProperty("spring")).isEqualTo("boot");
    assertThat(environment.getProperty("sprong")).isNull();
    assertThat(propertySource.getCount("spring")).isOne();
    assertThat(propertySource.getCount("sprong")).isOne();
  }

  @Test
  void getPropertyWhenNotValidConfigurationPropertyName() {
    ResolverEnvironment environment = new ResolverEnvironment();
    CountingMockPropertySource propertySource = createMockPropertySource(environment, true);
    assertThat(environment.getProperty("spr!ng")).isEqualTo("boot");
    assertThat(environment.getProperty("spr*ng")).isNull();
    assertThat(propertySource.getCount("spr!ng")).isOne();
    assertThat(propertySource.getCount("spr*ng")).isOne();
  }

  @Test
  void getPropertyWhenNotAttached() {
    ResolverEnvironment environment = new ResolverEnvironment();
    CountingMockPropertySource propertySource = createMockPropertySource(environment, false);
    assertThat(environment.getProperty("spring")).isEqualTo("boot");
    assertThat(environment.getProperty("sprong")).isNull();
    assertThat(propertySource.getCount("spring")).isOne();
    assertThat(propertySource.getCount("sprong")).isOne();
  }

  @Test
  void getPropertyAsTypeWhenHasPlaceholder() {
    ResolverEnvironment environment = new ResolverEnvironment();
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.withProperty("v1", "1");
    propertySource.withProperty("v2", "${v1}");
    environment.getPropertySources().addFirst(propertySource);
    assertThat(environment.getProperty("v2")).isEqualTo("1");
    assertThat(environment.getProperty("v2", Integer.class)).isOne();
  }

  @Test
  void throwsInvalidConfigurationPropertyValueExceptionWhenGetPropertyAsTypeFailsToConvert() {
    ResolverEnvironment environment = new ResolverEnvironment();
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.withProperty("v1", "one");
    propertySource.withProperty("v2", "${v1}");
    environment.getPropertySources().addFirst(propertySource);
    assertThat(environment.getProperty("v2")).isEqualTo("one");
    assertThatExceptionOfType(ConversionFailedException.class)
            .isThrownBy(() -> environment.getProperty("v2", Integer.class))
            .satisfies((ex) -> {
              assertThat(ex.getValue()).isEqualTo("one");
              assertThat(ex.getSourceType()).isEqualTo(TypeDescriptor.valueOf(String.class));
              assertThat(ex.getTargetType()).isEqualTo(TypeDescriptor.valueOf(Integer.class));
            })
            .havingCause()
            .satisfies((ex) -> {
              InvalidConfigurationPropertyValueException invalidValueEx = (InvalidConfigurationPropertyValueException) ex;
              assertThat(invalidValueEx.getName()).isEqualTo("v2");
              assertThat(invalidValueEx.getValue()).isEqualTo("one");
              assertThat(ex).cause().isInstanceOf(NumberFormatException.class);
            });
  }

  private CountingMockPropertySource createMockPropertySource(StandardEnvironment environment, boolean attach) {
    CountingMockPropertySource propertySource = new CountingMockPropertySource();
    propertySource.withProperty("spring", "boot");
    propertySource.withProperty("spr!ng", "boot");
    environment.getPropertySources().addFirst(propertySource);
    if (attach) {
      ConfigurationPropertySources.attach(environment);
    }
    return propertySource;
  }

  static class ResolverEnvironment extends StandardEnvironment {

    @Override
    protected ConfigurablePropertyResolver createPropertyResolver(PropertySources propertySources) {
      return new ConfigurationPropertySourcesPropertyResolver(propertySources);
    }

  }

  static class CountingMockPropertySource extends MockPropertySource {

    private final Map<String, AtomicInteger> counts = new HashMap<>();

    @Override
    public Object getProperty(String name) {
      incrementCount(name);
      return super.getProperty(name);
    }

    @Override
    public boolean containsProperty(String name) {
      incrementCount(name);
      return super.containsProperty(name);
    }

    private void incrementCount(String name) {
      this.counts.computeIfAbsent(name, (k) -> new AtomicInteger()).incrementAndGet();
    }

    int getCount(String name) {
      AtomicInteger count = this.counts.get(name);
      return (count != null) ? count.get() : 0;
    }

  }

}
