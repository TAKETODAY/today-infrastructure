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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributor.Kind;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributors.BinderOption;
import cn.taketoday.mock.env.MockEnvironment;
import cn.taketoday.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironmentContributors}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataEnvironmentContributorsTests {

  private static final ConfigDataLocation LOCATION_1 = ConfigDataLocation.valueOf("location1");

  private static final ConfigDataLocation LOCATION_2 = ConfigDataLocation.valueOf("location2");

  private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

  private MockEnvironment environment;

  private Binder binder;

  private ConfigDataImporter importer;

  private ConfigDataActivationContext activationContext;

  @Captor
  private ArgumentCaptor<ConfigDataLocationResolverContext> locationResolverContext;

  @Captor
  private ArgumentCaptor<ConfigDataLoaderContext> loaderContext;

  @BeforeEach
  void setup() {
    this.environment = new MockEnvironment();
    this.binder = Binder.get(this.environment);
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            this.binder, new DefaultResourceLoader(getClass().getClassLoader()));
    ConfigDataLoaders loaders = new ConfigDataLoaders(this.bootstrapContext,
            getClass().getClassLoader());
    this.importer = new ConfigDataImporter(ConfigDataNotFoundAction.FAIL, resolvers, loaders);
    this.activationContext = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, null);
  }

  @Test
  void createCreatesWithInitialContributors() {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(contributor));
    Iterator<ConfigDataEnvironmentContributor> iterator = contributors.iterator();
    assertThat(iterator.next()).isSameAs(contributor);
    assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
  }

  @Test
  void withProcessedImportsWhenHasNoUnprocessedImportsReturnsSameInstance() {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
            .ofExisting(new MockPropertySource());
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(contributor));
    ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
            this.activationContext);
    assertThat(withProcessedImports).isSameAs(contributors);
  }

  @Test
  void withProcessedImportsResolvesAndLoads() {
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = Arrays.asList(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
            new ConfigData(Arrays.asList(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(contributor));
    ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
            this.activationContext);
    Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
    assertThat(iterator.next().getPropertySource()).isSameAs(propertySource);
    assertThat(iterator.next().getKind()).isEqualTo(Kind.INITIAL_IMPORT);
    assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void withProcessedImportsResolvesAndLoadsChainedImports() {
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> initialLocations = List.of(LOCATION_1);
    MockPropertySource initialPropertySource = new MockPropertySource();
    initialPropertySource.setProperty("context.config.import", "location2");
    Map<ConfigDataResolutionResult, ConfigData> initialImported = new LinkedHashMap<>();
    initialImported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
            new ConfigData(List.of(initialPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(initialLocations)))
            .willReturn(initialImported);
    List<ConfigDataLocation> secondLocations = List.of(LOCATION_2);
    MockPropertySource secondPropertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> secondImported = new LinkedHashMap<>();
    secondImported.put(new ConfigDataResolutionResult(LOCATION_2, new TestConfigDataResource("b"), false),
            new ConfigData(List.of(secondPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(secondLocations)))
            .willReturn(secondImported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor));
    ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
            this.activationContext);
    Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
    assertThat(iterator.next().getPropertySource()).isSameAs(secondPropertySource);
    assertThat(iterator.next().getPropertySource()).isSameAs(initialPropertySource);
    assertThat(iterator.next().getKind()).isEqualTo(Kind.INITIAL_IMPORT);
    assertThat(iterator.next().getKind()).isEqualTo(Kind.ROOT);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void withProcessedImportsProvidesLocationResolverContextWithAccessToBinder() {
    MockPropertySource existingPropertySource = new MockPropertySource();
    existingPropertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
            .ofExisting(existingPropertySource);
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = Arrays.asList(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
            new ConfigData(Arrays.asList(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(existingContributor, contributor));
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should().resolveAndLoad(any(), this.locationResolverContext.capture(), any(), any());
    ConfigDataLocationResolverContext context = this.locationResolverContext.getValue();
    assertThat(context.getBinder().bind("test", String.class).get()).isEqualTo("springboot");
  }

  @Test
  void withProcessedImportsProvidesLocationResolverContextWithAccessToParent() {
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> initialLocations = List.of(LOCATION_1);
    MockPropertySource initialPropertySource = new MockPropertySource();
    initialPropertySource.setProperty("context.config.import", "location2");
    Map<ConfigDataResolutionResult, ConfigData> initialImported = new LinkedHashMap<>();
    initialImported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
            new ConfigData(List.of(initialPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(initialLocations)))
            .willReturn(initialImported);
    List<ConfigDataLocation> secondLocations = List.of(LOCATION_2);
    MockPropertySource secondPropertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> secondImported = new LinkedHashMap<>();
    secondImported.put(new ConfigDataResolutionResult(LOCATION_2, new TestConfigDataResource("b"), false),
            new ConfigData(List.of(secondPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(secondLocations)))
            .willReturn(secondImported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor));
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should().resolveAndLoad(any(), this.locationResolverContext.capture(), any(),
            eq(secondLocations));
    ConfigDataLocationResolverContext context = this.locationResolverContext.getValue();
    assertThat(context.getParent()).hasToString("a");
  }

  @Test
  void withProcessedImportsProvidesLocationResolverContextWithAccessToBootstrapRegistry() {
    MockPropertySource existingPropertySource = new MockPropertySource();
    existingPropertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
            .ofExisting(existingPropertySource);
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = List.of(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
            new ConfigData(List.of(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(existingContributor, contributor));
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should().resolveAndLoad(any(), this.locationResolverContext.capture(), any(), any());
    ConfigDataLocationResolverContext context = this.locationResolverContext.getValue();
    assertThat(context.getBootstrapContext()).isSameAs(this.bootstrapContext);
  }

  @Test
  void withProcessedImportsProvidesLoaderContextWithAccessToBootstrapRegistry() {
    MockPropertySource existingPropertySource = new MockPropertySource();
    existingPropertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
            .ofExisting(existingPropertySource);
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = List.of(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
            new ConfigData(List.of(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(existingContributor, contributor));
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should().resolveAndLoad(any(), any(), this.loaderContext.capture(), any());
    ConfigDataLoaderContext context = this.loaderContext.getValue();
    assertThat(context.getBootstrapContext()).isSameAs(this.bootstrapContext);
  }

  @Test
  void getBinderProvidesBinder() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor));
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("springboot");
  }

  @Test
  void getBinderWhenHasMultipleSourcesPicksFirst() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("test", "one");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("test", "two");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor));
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("one");
  }

  @Test
  void getBinderWhenHasInactiveIgnoresInactive() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("test", "one");
    firstPropertySource.setProperty("context.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("test", "two");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor));
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
  }

  @Test
  void getBinderWhenHasPlaceholderResolvesPlaceholder() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("test", "${other}");
    propertySource.setProperty("other", "springboot");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor));
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("springboot");
  }

  @Test
  void getBinderWhenHasPlaceholderAndInactiveResolvesPlaceholderOnlyFromActive() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("other", "one");
    firstPropertySource.setProperty("context.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("other", "two");
    secondPropertySource.setProperty("test", "${other}");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor));
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
  }

  @Test
  void getBinderWhenFailOnBindToInactiveSourceWithFirstInactiveThrowsException() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("test", "one");
    firstPropertySource.setProperty("context.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("test", "two");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor));
    Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
            .satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
  }

  @Test
  void getBinderWhenFailOnBindToInactiveSourceWithLastInactiveThrowsException() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("test", "one");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("context.config.activate.on-profile", "production");
    secondPropertySource.setProperty("test", "two");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor));
    Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
            .satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
  }

  @Test
  void getBinderWhenFailOnBindToInactiveSourceWithResolveToInactiveThrowsException() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("other", "one");
    firstPropertySource.setProperty("context.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("test", "${other}");
    secondPropertySource.setProperty("other", "one");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor));
    Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
            .satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
  }

  private ConfigDataEnvironmentContributor createBoundImportContributor(ConfigData configData,
          int propertySourceIndex) {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(null, null,
            false, configData, propertySourceIndex);
    return contributor.withBoundProperties(Collections.singleton(contributor), null);
  }

  private static class TestConfigDataResource extends ConfigDataResource {

    private final String value;

    TestConfigDataResource(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return this.value;
    }

  }

}
