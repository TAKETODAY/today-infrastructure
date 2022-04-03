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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.BootstrapRegistry.InstanceSupplier;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, Collections.singletonList(TestLogResolver.class.getName()));
    assertThat(resolvers.getResolvers()).hasSize(1);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestLogResolver.class);
    TestLogResolver resolver = (TestLogResolver) resolvers.getResolvers().get(0);
    assertThat(resolver.getLog()).isNotNull();
  }

  @Test
  void createWhenInjectingBinderCreatesResolver() {
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, Collections.singletonList(TestBoundResolver.class.getName()));
    assertThat(resolvers.getResolvers()).hasSize(1);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestBoundResolver.class);
    assertThat(((TestBoundResolver) resolvers.getResolvers().get(0)).getBinder()).isSameAs(this.binder);
  }

  @Test
  void createWhenNotInjectingBinderCreatesResolver() {
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, Collections.singletonList(TestResolver.class.getName()));
    assertThat(resolvers.getResolvers()).hasSize(1);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(TestResolver.class);
  }

  @Test
  void createWhenResolverHasBootstrapParametersInjectsBootstrapContext() {
    new ConfigDataLocationResolvers(this.bootstrapContext, this.binder, this.resourceLoader,
            Collections.singletonList(TestBootstrappingResolver.class.getName()));
    assertThat(this.bootstrapContext.get(String.class)).isEqualTo("boot");
  }

  @Test
  void createWhenNameIsNotConfigDataLocationResolverThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ConfigDataLocationResolvers(this.bootstrapContext, this.binder,
                    this.resourceLoader, Collections.singletonList(InputStream.class.getName())))
            .withMessageContaining("Unable to instantiate").havingCause().withMessageContaining("not assignable");
  }

  @Test
  void createOrdersResolvers() {
    List<String> names = new ArrayList<>();
    names.add(TestResolver.class.getName());
    names.add(LowestTestResolver.class.getName());
    names.add(HighestTestResolver.class.getName());
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, names);
    assertThat(resolvers.getResolvers().get(0)).isExactlyInstanceOf(HighestTestResolver.class);
    assertThat(resolvers.getResolvers().get(1)).isExactlyInstanceOf(TestResolver.class);
    assertThat(resolvers.getResolvers().get(2)).isExactlyInstanceOf(LowestTestResolver.class);
  }

  @Test
  void resolveResolvesUsingFirstSupportedResolver() {
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader,
            Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
    ConfigDataLocation location = ConfigDataLocation.of("LowestTestResolver:test");
    List<ConfigDataResolutionResult> resolved = resolvers.resolve(this.context, location, null);
    Assertions.assertThat(resolved).hasSize(1);
    TestConfigDataResource resource = (TestConfigDataResource) resolved.get(0).getResource();
    assertThat(resource.getResolver()).isInstanceOf(LowestTestResolver.class);
    assertThat(resource.getLocation()).isEqualTo(location);
    assertThat(resource.isProfileSpecific()).isFalse();
  }

  @Test
  void resolveWhenProfileMergesResolvedLocations() {
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader,
            Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
    ConfigDataLocation location = ConfigDataLocation.of("LowestTestResolver:test");
    List<ConfigDataResolutionResult> resolved = resolvers.resolve(this.context, location, this.profiles);
    Assertions.assertThat(resolved).hasSize(2);
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
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader,
            Arrays.asList(LowestTestResolver.class.getName(), HighestTestResolver.class.getName()));
    ConfigDataLocation location = ConfigDataLocation.of("Missing:test");
    assertThatExceptionOfType(UnsupportedConfigDataLocationException.class)
            .isThrownBy(() -> resolvers.resolve(this.context, location, null))
            .satisfies((ex) -> assertThat(ex.getLocation()).isEqualTo(location));
  }

  @Test
  void resolveWhenOptional() {
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, this.resourceLoader, Arrays.asList(OptionalResourceTestResolver.class.getName()));
    ConfigDataLocation location = ConfigDataLocation.of("OptionalResourceTestResolver:test");
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
