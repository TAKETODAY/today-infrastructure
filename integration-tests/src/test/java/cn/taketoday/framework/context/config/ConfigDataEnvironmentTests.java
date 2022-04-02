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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.MockApplicationEnvironment;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributor.Kind;
import cn.taketoday.framework.context.config.TestConfigDataEnvironmentUpdateListener.AddedPropertySource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.env.MockPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataEnvironmentTests {

  private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

  private MockApplicationEnvironment environment = new MockApplicationEnvironment();

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  private Collection<String> additionalProfiles = Collections.emptyList();

  @Test
  void createExposesEnvironmentBinderToConfigDataLocationResolvers() {
    this.environment.setProperty("spring", "boot");
    TestConfigDataEnvironment configDataEnvironment = new TestConfigDataEnvironment(
            this.bootstrapContext, this.environment, this.resourceLoader, this.additionalProfiles, null);
    assertThat(configDataEnvironment.getConfigDataLocationResolversBinder().bind("spring", String.class).get())
            .isEqualTo("boot");
  }

  @Test
  void createCreatesContributorsBasedOnExistingSources() {
    MockPropertySource propertySource1 = new MockPropertySource("p1");
    MockPropertySource propertySource2 = new MockPropertySource("p2");
    MockPropertySource propertySource3 = new MockPropertySource("p3");
    this.environment.getPropertySources().addLast(propertySource1);
    this.environment.getPropertySources().addLast(propertySource2);
    this.environment.getPropertySources().addLast(propertySource3);
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors().getRoot()
            .getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
    Object[] wrapped = children.stream().filter((child) -> child.getKind() == Kind.EXISTING)
            .map(ConfigDataEnvironmentContributor::getPropertySource).toArray();
    assertThat(wrapped[1]).isEqualTo(propertySource1);
    assertThat(wrapped[2]).isEqualTo(propertySource2);
    assertThat(wrapped[3]).isEqualTo(propertySource3);
  }

  @Test
  void createWhenHasDefaultPropertySourceMovesItToLastContributor() {
    MockPropertySource defaultPropertySource = new MockPropertySource("defaultProperties");
    MockPropertySource propertySource1 = new MockPropertySource("p2");
    MockPropertySource propertySource2 = new MockPropertySource("p3");
    this.environment.getPropertySources().addLast(defaultPropertySource);
    this.environment.getPropertySources().addLast(propertySource1);
    this.environment.getPropertySources().addLast(propertySource2);
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors().getRoot()
            .getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
    Object[] wrapped = children.stream().filter((child) -> child.getKind() == Kind.EXISTING)
            .map(ConfigDataEnvironmentContributor::getPropertySource).toArray();
    assertThat(wrapped[1]).isEqualTo(propertySource1);
    assertThat(wrapped[2]).isEqualTo(propertySource2);
    assertThat(wrapped[3]).isEqualTo(defaultPropertySource);
  }

  @Test
  void createCreatesInitialImportContributorsInCorrectOrder() {
    this.environment.setProperty("context.config.location", "l1,l2");
    this.environment.setProperty("context.config.additional-location", "a1,a2");
    this.environment.setProperty("context.config.import", "i1,i2");
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    List<ConfigDataEnvironmentContributor> children = configDataEnvironment.getContributors().getRoot()
            .getChildren(ImportPhase.BEFORE_PROFILE_ACTIVATION);
    Object[] imports = children.stream().filter((child) -> child.getKind() == Kind.INITIAL_IMPORT)
            .map(ConfigDataEnvironmentContributor::getImports).map(Object::toString).toArray();
    assertThat(imports).containsExactly("[i2]", "[i1]", "[a2]", "[a1]", "[l2]", "[l1]");
  }

  @Test
  void processAndApplyAddsImportedSourceToEnvironment(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getProperty("spring")).isEqualTo("boot");
  }

  @Test
  void processAndApplyOnlyAddsActiveContributors(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getProperty("spring")).isEqualTo("boot");
    assertThat(this.environment.getProperty("other")).isNull();
  }

  @Test
  void processAndApplyMovesDefaultPropertySourceToLast(TestInfo info) {
    MockPropertySource defaultPropertySource = new MockPropertySource("defaultProperties");
    this.environment.getPropertySources().addFirst(defaultPropertySource);
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    configDataEnvironment.processAndApply();
    List<PropertySource<?>> sources = this.environment.getPropertySources().stream().collect(Collectors.toList());
    assertThat(sources.get(sources.size() - 1)).isSameAs(defaultPropertySource);
  }

  @Test
  void processAndApplySetsDefaultProfiles(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getDefaultProfiles()).containsExactly("one", "two", "three");
  }

  @Test
  void processAndApplySetsActiveProfiles(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getActiveProfiles()).containsExactly("one", "two", "three");
  }

  @Test
  void processAndApplySetsActiveProfilesAndProfileGroups(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getActiveProfiles()).containsExactly("one", "four", "five", "two", "three");
  }

  @Test
  void processAndApplyDoesNotSetProfilesFromIgnoreProfilesContributors(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null) {

      @Override
      protected ConfigDataEnvironmentContributors createContributors(
              List<ConfigDataEnvironmentContributor> contributors) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("context.profiles.active", "ignore1");
        source.put("context.profiles.include", "ignore2");
        ConfigData data = new ConfigData(Collections.singleton(new MapPropertySource("test", source)),
                ConfigData.Option.IGNORE_PROFILES);
        contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(ConfigDataLocation.of("test"),
                mock(ConfigDataResource.class), false, data, 0));
        return super.createContributors(contributors);
      }

    };
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getActiveProfiles()).containsExactly("test");
  }

  @ParameterizedTest
  @CsvSource({ "include", "include[0]" })
  void processAndApplyWhenHasProfileIncludeInProfileSpecificDocumentThrowsException(String property, TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null) {

      @Override
      protected ConfigDataEnvironmentContributors createContributors(
              List<ConfigDataEnvironmentContributor> contributors) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("context.config.activate.on-profile", "activate");
        source.put("context.profiles." + property, "include");
        ConfigData data = new ConfigData(Collections.singleton(new MapPropertySource("test", source)));
        contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(ConfigDataLocation.of("test"),
                mock(ConfigDataResource.class), false, data, 0));
        return super.createContributors(contributors);
      }

    };
    assertThatExceptionOfType(InactiveConfigDataAccessException.class)
            .isThrownBy(configDataEnvironment::processAndApply);
  }

  @ParameterizedTest
  @CsvSource({ "context.profiles.include", "context.profiles.include[0]" })
  void processAndApplyIncludesProfilesFromSpringProfilesInclude(String property, TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null) {

      @Override
      protected ConfigDataEnvironmentContributors createContributors(
              List<ConfigDataEnvironmentContributor> contributors) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(property, "included");
        ConfigData data = new ConfigData(Collections.singleton(new MapPropertySource("test", source)));
        contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(ConfigDataLocation.of("test"),
                mock(ConfigDataResource.class), false, data, 0));
        return super.createContributors(contributors);
      }

    };
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getActiveProfiles()).containsExactly("included");
  }

  @Test
  void processAndApplyDoesNotSetProfilesFromIgnoreProfilesContributorsWhenNoProfilesActive(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null) {

      @Override
      protected ConfigDataEnvironmentContributors createContributors(
              List<ConfigDataEnvironmentContributor> contributors) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("context.profiles.active", "ignore1");
        source.put("context.profiles.include", "ignore2");
        ConfigData data = new ConfigData(Collections.singleton(new MapPropertySource("test", source)),
                ConfigData.Option.IGNORE_PROFILES);
        contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(ConfigDataLocation.of("test"),
                mock(ConfigDataResource.class), false, data, 0));
        return super.createContributors(contributors);
      }

    };
    configDataEnvironment.processAndApply();
    assertThat(this.environment.getActiveProfiles()).isEmpty();
    assertThat(this.environment.getProperty("spring")).isEqualTo("boot");
  }

  @Test
  @Disabled("Disabled until context.profiles support is dropped")
  void processAndApplyWhenHasInvalidPropertyThrowsException() {
    this.environment.setProperty("context.profile", "a");
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, null);
    assertThatExceptionOfType(InvalidConfigDataPropertyException.class)
            .isThrownBy(configDataEnvironment::processAndApply);
  }

  @Test
  void processAndApplyWhenHasListenerCallsOnPropertySourceAdded(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, listener);
    configDataEnvironment.processAndApply();
    assertThat(listener.getAddedPropertySources()).hasSize(1);
    AddedPropertySource addedPropertySource = listener.getAddedPropertySources().get(0);
    assertThat(addedPropertySource.getPropertySource().getProperty("spring")).isEqualTo("boot");
    assertThat(addedPropertySource.getLocation().toString()).isEqualTo(getConfigLocation(info));
    assertThat(addedPropertySource.getResource().toString()).contains("class path resource")
            .contains(info.getTestMethod().get().getName());
  }

  @Test
  void processAndApplyWhenHasListenerCallsOnSetProfiles(TestInfo info) {
    this.environment.setProperty("context.config.location", getConfigLocation(info));
    TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
    ConfigDataEnvironment configDataEnvironment = new ConfigDataEnvironment(this.bootstrapContext,
            this.environment, this.resourceLoader, this.additionalProfiles, listener);
    configDataEnvironment.processAndApply();
    assertThat(listener.getProfiles().getActive()).containsExactly("one", "two", "three");
  }

  private String getConfigLocation(TestInfo info) {
    return "optional:classpath:" + info.getTestClass().get().getName().replace('.', '/') + "-"
            + info.getTestMethod().get().getName() + ".properties";
  }

  static class TestConfigDataEnvironment extends ConfigDataEnvironment {

    private Binder configDataLocationResolversBinder;

    TestConfigDataEnvironment(
            ConfigurableBootstrapContext bootstrapContext,
            ConfigurableEnvironment environment,
            ResourceLoader resourceLoader,
            Collection<String> additionalProfiles,
            @Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
      super(bootstrapContext, environment, resourceLoader, additionalProfiles,
              environmentUpdateListener);
    }

    @Override
    protected ConfigDataLocationResolvers createConfigDataLocationResolvers(
            ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
      this.configDataLocationResolversBinder = binder;
      return super.createConfigDataLocationResolvers(bootstrapContext, binder, resourceLoader);
    }

    Binder getConfigDataLocationResolversBinder() {
      return this.configDataLocationResolversBinder;
    }

  }

}
