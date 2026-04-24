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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import infra.app.BootstrapContext;
import infra.app.BootstrapRegistry;
import infra.app.BootstrapRegistry.InstanceSupplier;
import infra.app.ConfigurableBootstrapContext;
import infra.app.DefaultBootstrapContext;
import infra.context.properties.bind.Binder;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.core.test.io.support.MockTodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConfigDataLocationResolvers}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataLocationResolversTests {

  private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

  @Mock
  private Binder binder;

  @Mock
  private ConfigDataLocationResolverContext context;

  @Mock
  private Profiles profiles;

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Test
  void createWhenInjectingLogAndDeferredLogFactoryCreatesResolver() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, TestLogResolver.class);
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    assertThat(resolvers.getResolvers()).hasSize(1);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestLogResolver.class);
    TestLogResolver resolver = (TestLogResolver) resolvers.getResolvers().get(0);
    assertThat(resolver.getLog()).isNotNull();
  }

  @Test
  void createWhenInjectingBinderCreatesResolver() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, TestBoundResolver.class);
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    assertThat(resolvers.getResolvers()).hasSize(1);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestBoundResolver.class);
    assertThat(((TestBoundResolver) resolvers.getResolvers().get(0)).getBinder()).isSameAs(this.binder);
  }

  @Test
  void createWhenNotInjectingBinderCreatesResolver() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, TestResolver.class);
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    assertThat(resolvers.getResolvers()).hasSize(1);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestResolver.class);
  }

  @Test
  void createWhenResolverHasBootstrapParametersInjectsBootstrapContext() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, TestBootstrappingResolver.class);
    new ConfigDataLocationResolvers(this.bootstrapContext, this.binder, this.resourceLoader, strategies);
    assertThat(this.bootstrapContext.get(String.class)).isEqualTo("boot");
  }

  @Test
  void createOrdersResolvers() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, TestResolver.class, LowestTestResolver.class,
            HighestTestResolver.class);

    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(HighestTestResolver.class);
    assertThat(resolvers.getResolvers().get(1)).isExactlyInstanceOf(TestResolver.class);
    assertThat(resolvers.getResolvers().get(2)).isExactlyInstanceOf(LowestTestResolver.class);
  }

  @Test
  void resolveResolvesUsingFirstSupportedResolver() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, LowestTestResolver.class, HighestTestResolver.class);

    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    ConfigDataLocation location = ConfigDataLocation.valueOf("LowestTestResolver:test");
    List<ConfigDataResolutionResult> resolved = resolvers.resolve(this.context, location, null);
    assertThat(resolved).hasSize(1);
    TestConfigDataResource resource = (TestConfigDataResource) resolved.get(0).getResource();
    assertThat(resource.getResolver()).isInstanceOf(LowestTestResolver.class);
    assertThat(resource.getLocation()).isEqualTo(location);
    assertThat(resource.isProfileSpecific()).isFalse();
  }

  @Test
  void resolveWhenProfileMergesResolvedLocations() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, LowestTestResolver.class, HighestTestResolver.class);

    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    ConfigDataLocation location = ConfigDataLocation.valueOf("LowestTestResolver:test");
    List<ConfigDataResolutionResult> resolved = resolvers.resolve(this.context, location, this.profiles);
    assertThat(resolved).hasSize(2);
    TestConfigDataResource resource = (TestConfigDataResource) resolved.get(0).getResource();
    assertThat(resource.getResolver()).isInstanceOf(LowestTestResolver.class);
    assertThat(resource.getLocation()).isEqualTo(location);
    assertThat(resource.isProfileSpecific()).isFalse();
    TestConfigDataResource profileResource = (TestConfigDataResource) resolved.get(1).getResource();
    assertThat(profileResource.getResolver()).isInstanceOf(LowestTestResolver.class);
    assertThat(profileResource.getLocation()).isEqualTo(location);
    assertThat(profileResource.isProfileSpecific()).isTrue();
  }

  @Test
  void resolveWhenNoResolverThrowsException() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, LowestTestResolver.class, HighestTestResolver.class);

    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    ConfigDataLocation location = ConfigDataLocation.valueOf("Missing:test");
    assertThatExceptionOfType(UnsupportedConfigDataLocationException.class)
            .isThrownBy(() -> resolvers.resolve(this.context, location, null))
            .satisfies((ex) -> assertThat(ex.getLocation()).isEqualTo(location));
  }

  @Test
  void resolveWhenOptional() {
    MockTodayStrategies strategies = new MockTodayStrategies();
    strategies.add(ConfigDataLocationResolver.class, OptionalResourceTestResolver.class);

    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, strategies);
    ConfigDataLocation location = ConfigDataLocation.valueOf("OptionalResourceTestResolver:test");
    List<ConfigDataResolutionResult> resolved = resolvers.resolve(this.context, location, null);
    assertThat(resolved.get(0).getResource().isOptional()).isTrue();
  }

  static class TestResolver implements ConfigDataLocationResolver<TestConfigDataResource> {

    private final boolean optionalResource;

    TestResolver() {
      this(false);
    }

    TestResolver(boolean optionalResource) {
      this.optionalResource = optionalResource;
    }

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
      String name = getClass().getName();
      name = name.substring(name.lastIndexOf("$") + 1);
      return location.hasPrefix(name + ":");
    }

    @Override
    public List<TestConfigDataResource> resolve(ConfigDataLocationResolverContext context,
            ConfigDataLocation location) {
      return Collections.singletonList(new TestConfigDataResource(this.optionalResource, this, location, false));
    }

    @Override
    public List<TestConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
            ConfigDataLocation location, Profiles profiles) {
      return Collections.singletonList(new TestConfigDataResource(this.optionalResource, this, location, true));
    }

  }

  static class TestLogResolver extends TestResolver {
    private final Logger log = LoggerFactory.getLogger(TestLogResolver.class);

    Logger getLog() {
      return this.log;
    }

  }

  static class TestBoundResolver extends TestResolver {

    private final Binder binder;

    TestBoundResolver(Binder binder) {
      this.binder = binder;
    }

    Binder getBinder() {
      return this.binder;
    }

  }

  static class TestBootstrappingResolver extends TestResolver {

    TestBootstrappingResolver(ConfigurableBootstrapContext configurableBootstrapContext,
            BootstrapRegistry bootstrapRegistry, BootstrapContext bootstrapContext) {
      assertThat(configurableBootstrapContext).isNotNull();
      assertThat(bootstrapRegistry).isNotNull();
      assertThat(bootstrapContext).isNotNull();
      assertThat(configurableBootstrapContext).isEqualTo(bootstrapRegistry).isEqualTo(bootstrapContext);
      bootstrapRegistry.register(String.class, InstanceSupplier.of("boot"));
    }

  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class HighestTestResolver extends TestResolver {

  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  static class LowestTestResolver extends TestResolver {

  }

  static class OptionalResourceTestResolver extends TestResolver {

    OptionalResourceTestResolver() {
      super(true);
    }

  }

  static class TestConfigDataResource extends ConfigDataResource {

    private final TestResolver resolver;

    private final ConfigDataLocation location;

    private final boolean profileSpecific;

    TestConfigDataResource(boolean optional, TestResolver resolver, ConfigDataLocation location,
            boolean profileSpecific) {
      super(optional);
      this.resolver = resolver;
      this.location = location;
      this.profileSpecific = profileSpecific;
    }

    TestResolver getResolver() {
      return this.resolver;
    }

    ConfigDataLocation getLocation() {
      return this.location;
    }

    boolean isProfileSpecific() {
      return this.profileSpecific;
    }

  }

}
