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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
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
import static org.mockito.ArgumentMatchers.assertArg;
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

  private final DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

  private final ConversionService conversionService = DefaultConversionService.getSharedInstance();

  private ConfigDataImporter importer;

  private ConfigDataActivationContext activationContext;

  @BeforeEach
  void setup() {
    MockEnvironment environment = new MockEnvironment();
    Binder binder = Binder.get(environment);
    ConfigDataLocationResolvers resolvers = new ConfigDataLocationResolvers(this.bootstrapContext,
            binder, new DefaultResourceLoader(getClass().getClassLoader()));
    ConfigDataLoaders loaders = new ConfigDataLoaders(this.bootstrapContext, getClass().getClassLoader());
    this.importer = new ConfigDataImporter(ConfigDataNotFoundAction.FAIL, resolvers, loaders);
    this.activationContext = new ConfigDataActivationContext(CloudPlatform.KUBERNETES, null);
  }

  @Test
  void createCreatesWithInitialContributors() {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
            this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor), this.conversionService);
    Iterator<ConfigDataEnvironmentContributor> iterator = contributors.iterator();
    assertThat(iterator.next()).isSameAs(contributor);
    assertThat(iterator.next().kind).isEqualTo(Kind.ROOT);
  }

  @Test
  void withProcessedImportsWhenHasNoUnprocessedImportsReturnsSameInstance() {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor
            .ofExisting(new MockPropertySource(), this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor), this.conversionService);
    ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer,
            this.activationContext);
    assertThat(withProcessedImports).isSameAs(contributors);
  }

  @Test
  void withProcessedImportsResolvesAndLoads() {
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
            new ConfigData(List.of(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1, this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.bootstrapContext, List.of(contributor), this.conversionService);
    ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer, this.activationContext);
    Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
    assertThat(iterator.next().propertySource).isSameAs(propertySource);
    assertThat(iterator.next().kind).isEqualTo(Kind.INITIAL_IMPORT);
    assertThat(iterator.next().kind).isEqualTo(Kind.ROOT);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void withProcessedImportsResolvesAndLoadsChainedImports() {
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> initialLocations = Collections.singletonList(LOCATION_1);
    MockPropertySource initialPropertySource = new MockPropertySource();
    initialPropertySource.setProperty("app.config.import", "location2");
    Map<ConfigDataResolutionResult, ConfigData> initialImported = new LinkedHashMap<>();
    initialImported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
            new ConfigData(List.of(initialPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(initialLocations)))
            .willReturn(initialImported);
    List<ConfigDataLocation> secondLocations = Collections.singletonList(LOCATION_2);
    MockPropertySource secondPropertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> secondImported = new LinkedHashMap<>();
    secondImported.put(new ConfigDataResolutionResult(LOCATION_2, new TestConfigDataResource("b"), false),
            new ConfigData(List.of(secondPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(secondLocations)))
            .willReturn(secondImported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1, this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(this.bootstrapContext, List.of(contributor), this.conversionService);
    ConfigDataEnvironmentContributors withProcessedImports = contributors.withProcessedImports(this.importer, this.activationContext);
    Iterator<ConfigDataEnvironmentContributor> iterator = withProcessedImports.iterator();
    assertThat(iterator.next().propertySource).isSameAs(secondPropertySource);
    assertThat(iterator.next().propertySource).isSameAs(initialPropertySource);
    assertThat(iterator.next().kind).isEqualTo(Kind.INITIAL_IMPORT);
    assertThat(iterator.next().kind).isEqualTo(Kind.ROOT);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void withProcessedImportsProvidesLocationResolverContextWithAccessToBinder() {
    MockPropertySource existingPropertySource = new MockPropertySource();
    existingPropertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
            .ofExisting(existingPropertySource, this.conversionService);
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
            new ConfigData(List.of(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
            this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(existingContributor, contributor), this.conversionService);
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should()
            .resolveAndLoad(any(),
                    assertArg((context) -> assertThat(context.getBinder().bind("test", String.class).get())
                            .isEqualTo("springboot")),
                    any(), any());
  }

  @Test
  void withProcessedImportsProvidesLocationResolverContextWithAccessToParent() {
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> initialLocations = Collections.singletonList(LOCATION_1);
    MockPropertySource initialPropertySource = new MockPropertySource();
    initialPropertySource.setProperty("app.config.import", "location2");
    Map<ConfigDataResolutionResult, ConfigData> initialImported = new LinkedHashMap<>();
    initialImported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a"), false),
            new ConfigData(List.of(initialPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(initialLocations)))
            .willReturn(initialImported);
    List<ConfigDataLocation> secondLocations = Collections.singletonList(LOCATION_2);
    MockPropertySource secondPropertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> secondImported = new LinkedHashMap<>();
    secondImported.put(new ConfigDataResolutionResult(LOCATION_2, new TestConfigDataResource("b"), false),
            new ConfigData(List.of(secondPropertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(secondLocations)))
            .willReturn(secondImported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
            this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor), this.conversionService);
    ArgumentCaptor<ConfigDataLocationResolverContext> locationResolverContext = ArgumentCaptor
            .forClass(ConfigDataLocationResolverContext.class);
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should()
            .resolveAndLoad(any(), locationResolverContext.capture(), any(), eq(secondLocations));
    ConfigDataLocationResolverContext context = locationResolverContext.getValue();
    assertThat(context.getParent()).hasToString("a");
  }

  @Test
  void withProcessedImportsProvidesLocationResolverContextWithAccessToBootstrapRegistry() {
    MockPropertySource existingPropertySource = new MockPropertySource();
    existingPropertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
            .ofExisting(existingPropertySource, this.conversionService);
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
            new ConfigData(List.of(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
            this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(existingContributor, contributor), this.conversionService);
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should()
            .resolveAndLoad(any(),
                    assertArg((context) -> assertThat(context.getBootstrapContext()).isSameAs(this.bootstrapContext)),
                    any(), any());
  }

  @Test
  void withProcessedImportsProvidesLoaderContextWithAccessToBootstrapRegistry() {
    MockPropertySource existingPropertySource = new MockPropertySource();
    existingPropertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor existingContributor = ConfigDataEnvironmentContributor
            .ofExisting(existingPropertySource, this.conversionService);
    this.importer = mock(ConfigDataImporter.class);
    List<ConfigDataLocation> locations = Collections.singletonList(LOCATION_1);
    MockPropertySource propertySource = new MockPropertySource();
    Map<ConfigDataResolutionResult, ConfigData> imported = new LinkedHashMap<>();
    imported.put(new ConfigDataResolutionResult(LOCATION_1, new TestConfigDataResource("a'"), false),
            new ConfigData(List.of(propertySource)));
    given(this.importer.resolveAndLoad(eq(this.activationContext), any(), any(), eq(locations)))
            .willReturn(imported);
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofInitialImport(LOCATION_1,
            this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(existingContributor, contributor), this.conversionService);
    contributors.withProcessedImports(this.importer, this.activationContext);
    then(this.importer).should()
            .resolveAndLoad(any(), any(),
                    assertArg((context) -> assertThat(context.getBootstrapContext()).isSameAs(this.bootstrapContext)),
                    any());
  }

  @Test
  void getBinderProvidesBinder() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("test", "springboot");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource,
            this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor), this.conversionService);
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
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService);
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("one");
  }

  @Test
  void getBinderWhenHasInactiveIgnoresInactive() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("test", "one");
    firstPropertySource.setProperty("app.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("test", "two");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService);
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
  }

  @Test
  void getBinderWhenHasPlaceholderResolvesPlaceholder() {
    MockPropertySource propertySource = new MockPropertySource();
    propertySource.setProperty("test", "${other}");
    propertySource.setProperty("other", "springboot");
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofExisting(propertySource,
            this.conversionService);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, List.of(contributor), this.conversionService);
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("springboot");
  }

  @Test
  void getBinderWhenHasPlaceholderAndInactiveResolvesPlaceholderOnlyFromActive() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("other", "one");
    firstPropertySource.setProperty("app.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("other", "two");
    secondPropertySource.setProperty("test", "${other}");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService);
    Binder binder = contributors.getBinder(this.activationContext);
    assertThat(binder.bind("test", String.class).get()).isEqualTo("two");
  }

  @Test
  void getBinderWhenFailOnBindToInactiveSourceWithFirstInactiveThrowsException() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("test", "one");
    firstPropertySource.setProperty("app.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("test", "two");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService);
    Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
            .satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
  }

  @Test
  void getBinderWhenFailOnBindToInactiveSourceWithLastInactiveThrowsException() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("test", "one");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("app.config.activate.on-profile", "production");
    secondPropertySource.setProperty("test", "two");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService);
    Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
            .satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
  }

  @Test
  void getBinderWhenFailOnBindToInactiveSourceWithResolveToInactiveThrowsException() {
    MockPropertySource firstPropertySource = new MockPropertySource();
    firstPropertySource.setProperty("other", "one");
    firstPropertySource.setProperty("app.config.activate.on-profile", "production");
    MockPropertySource secondPropertySource = new MockPropertySource();
    secondPropertySource.setProperty("test", "${other}");
    secondPropertySource.setProperty("other", "one");
    ConfigData configData = new ConfigData(Arrays.asList(firstPropertySource, secondPropertySource));
    ConfigDataEnvironmentContributor firstContributor = createBoundImportContributor(configData, 0);
    ConfigDataEnvironmentContributor secondContributor = createBoundImportContributor(configData, 1);
    ConfigDataEnvironmentContributors contributors = new ConfigDataEnvironmentContributors(
            this.bootstrapContext, Arrays.asList(firstContributor, secondContributor), this.conversionService);
    Binder binder = contributors.getBinder(this.activationContext, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> binder.bind("test", String.class))
            .satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(InactiveConfigDataAccessException.class));
  }

  private ConfigDataEnvironmentContributor createBoundImportContributor(ConfigData configData,
          int propertySourceIndex) {
    ConfigDataEnvironmentContributor contributor = ConfigDataEnvironmentContributor.ofUnboundImport(null, null,
            false, configData, propertySourceIndex, this.conversionService);
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
