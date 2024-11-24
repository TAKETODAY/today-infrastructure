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

package infra.app.context.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.app.context.config.ConfigDataLocation;
import infra.app.context.config.ConfigDataLocationResolver;
import infra.app.context.config.ConfigDataLocationResolverContext;
import infra.app.context.config.ConfigDataResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLocationResolver}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLocationResolverTests {

  private ConfigDataLocationResolver<?> resolver = new TestConfigDataLocationResolver();

  private ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

  @Test
  void resolveProfileSpecificReturnsEmptyList() {
    assertThat(this.resolver.resolveProfileSpecific(this.context, null, null)).isEmpty();
  }

  static class TestConfigDataLocationResolver implements ConfigDataLocationResolver<ConfigDataResource> {

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
      return true;
    }

    @Override
    public List<ConfigDataResource> resolve(ConfigDataLocationResolverContext context,
            ConfigDataLocation location) {
      return null;
    }

  }

}
