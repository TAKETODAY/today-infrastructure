/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import org.apache.logging.log4j.util.Strings;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.properties.bind.BindContext;
import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.BindHandler;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.Profiles;
import cn.taketoday.core.env.SimpleCommandLinePropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.origin.Origin;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigDataEnvironmentPostProcessorIntegrationTests {

  private Application application;

  @TempDir
  public File temp;

  @BeforeEach
  void setup() {
    this.application = new Application(Config.class);
    this.application.setApplicationType(ApplicationType.NONE_WEB);
  }

  @AfterEach
  void clearProperties() {
    System.clearProperty("the.property");
  }

  @Test
  void runWhenUsingCustomResourceLoader() {
    this.application.setResourceLoader(new ResourceLoader() {

      @Override
      public Resource getResource(String location) {
        if (location.equals("classpath:/custom.properties")) {
          return new ByteArrayResource("the.property: fromcustom".getBytes(), location);
        }
        return new ClassPathResource("doesnotexist");
      }

      @Override
      public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
      }

    });
    ConfigurableApplicationContext context = this.application.run("--app.config.name=custom");
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("fromcustom");
  }

  @Test
  void runLoadsApplicationPropertiesOnClasspath() {
    ConfigurableApplicationContext context = this.application.run();
    String property = context.getEnvironment().getProperty("foo");
    assertThat(property).isEqualTo("bucket");
  }

  @Test
  void runLoadsApplicationYamlOnClasspath() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=customapplication");
    String property = context.getEnvironment().getProperty("yamlkey");
    assertThat(property).isEqualTo("yamlvalue");
  }

  @Test
  void runLoadsFileWithCustomName() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testproperties");
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("frompropertiesfile");
  }

  @Test
  void runWhenPropertiesAndYamlShouldPreferProperties() {
    ConfigurableApplicationContext context = this.application.run();
    String property = context.getEnvironment().getProperty("duplicate");
    assertThat(property).isEqualTo("properties");
  }

  @Test
  void runWhenMultipleCustomNamesLoadsEachName() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=moreproperties,testproperties");
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("frompropertiesfile");
  }

  @Test
  void runWhenNoActiveProfilesLoadsDefaultProfileFile() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testprofiles");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromdefaultpropertiesfile");
  }

  @Test
  void runWhenActiveProfilesDoesNotLoadDefault() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testprofilesdocument",
            "--app.config.location=classpath:configdata/profiles/", "--infra.profiles.default=thedefault",
            "--infra.profiles.active=other");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromotherprofile");
  }

  @Test
  void runWhenHasCustomDefaultProfileLoadsDefaultProfileFile() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testprofiles",
            "--infra.profiles.default=thedefault");
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("fromdefaultpropertiesfile");
  }

  @Test
  void runWhenHasCustomSpringConfigLocationLoadsAllFromSpecifiedLocation() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:application.properties,classpath:testproperties.properties");
    String property1 = context.getEnvironment().getProperty("the.property");
    String property2 = context.getEnvironment().getProperty("my.property");
    String property3 = context.getEnvironment().getProperty("foo");
    assertThat(property1).isEqualTo("frompropertiesfile");
    assertThat(property2).isEqualTo("frompropertiesfile");
    assertThat(property3).isEqualTo("bucket");
  }

  @Test
  void runWhenOneCustomLocationDoesNotExistLoadsOthers() {
    ConfigurableApplicationContext context = this.application.run(
            "--app.config.location=classpath:application.properties,classpath:testproperties.properties,optional:classpath:nonexistent.properties");
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("frompropertiesfile");
  }

  @Test
  void runWhenProfileSpecificMandatoryLocationDoesNotExistShouldNotFail() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testprofiles",
            "--app.config.location=classpath:configdata/profiles/");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromyamlfile");
  }

  @Test
  void runWhenProfileSpecificMandatoryLocationDoesNotExistShouldFailWhenProfileActive() {
    this.application.setAdditionalProfiles("prod");
    assertThatExceptionOfType(ConfigDataResourceNotFoundException.class).isThrownBy(() -> this.application
            .run("--app.config.name=testprofiles", "--app.config.location=classpath:configdata/profiles/"));
  }

  @Test
  void runWhenHasActiveProfilesFromMultipleLocationsActivatesProfileFromOneLocation() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:enableprofile.properties,classpath:enableother.properties");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.getActiveProfiles()).containsExactly("other");
    String property = environment.getProperty("other.property");
    assertThat(property).isEqualTo("fromotherpropertiesfile");
  }

  @Test
  void runWhenHasActiveProfilesFromMultipleAdditionaLocationsWithOneSwitchedOffLoadsExpectedProperties() {
    ConfigurableApplicationContext context = this.application.run(
            "--app.config.additional-location=classpath:enabletwoprofiles.properties,classpath:enableprofile.properties");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.getActiveProfiles()).containsExactly("myprofile");
    String property = environment.getProperty("my.property");
    assertThat(property).isEqualTo("fromprofilepropertiesfile");
  }

  @Test
  void runWhenHaslocalFileLoadsWithLocalFileTakingPrecedenceOverClasspath() throws Exception {
    File localFile = new File(new File("."), "application.properties");
    assertThat(localFile.exists()).isFalse();
    try {
      Properties properties = new Properties();
      properties.put("my.property", "fromlocalfile");
      try (OutputStream outputStream = new FileOutputStream(localFile)) {
        properties.store(outputStream, "");
      }
      ConfigurableApplicationContext context = this.application.run();
      String property = context.getEnvironment().getProperty("my.property");
      assertThat(property).isEqualTo("fromlocalfile");
    }
    finally {
      localFile.delete();
    }
  }

  @Test
  void runWhenHasCommandLinePropertiesLoadsWithCommandLineTakingPrecedence() {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
            .addFirst(new SimpleCommandLinePropertySource("--the.property=fromcommandline"));
    this.application.setEnvironment(environment);
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testproperties");
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("fromcommandline");
  }

  @Test
  void runWhenHasSystemPropertyLoadsWithSystemPropertyTakingPrecedence() {
    System.setProperty("the.property", "fromsystem");
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testproperties");
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("fromsystem");
  }

  @Test
  void runWhenHasDefaultPropertiesIncludesDefaultPropertiesLast() {
    this.application.setDefaultProperties(Collections.singletonMap("my.fallback", "foo"));
    ConfigurableApplicationContext context = this.application.run();
    String property = context.getEnvironment().getProperty("my.fallback");
    assertThat(property).isEqualTo("foo");
  }

  @Test
  void runWhenHasDefaultPropertiesWithConfigLocationConfigurationLoadsExpectedProperties() {
    this.application.setDefaultProperties(Collections.singletonMap("app.config.name", "testproperties"));
    ConfigurableApplicationContext context = this.application.run();
    String property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("frompropertiesfile");
  }

  @Test
  void runWhenHasActiveProfilesFromDefaultPropertiesAndFileLoadsWithFileTakingPrecedence() {
    this.application.setDefaultProperties(Collections.singletonMap("infra.profiles.active", "dev"));
    ConfigurableApplicationContext context = this.application.run("--app.config.name=enableprofile");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("myprofile");
  }

  @Test
  void runWhenProgrammaticallySetProfilesLoadsWithSetProfilesTakePrecedenceOverDefaultProfile() {
    this.application.setAdditionalProfiles("other");
    ConfigurableApplicationContext context = this.application.run();
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromotherpropertiesfile");
  }

  @Test
  void runWhenTwoProfilesSetProgrammaticallyLoadsWithPreservedProfileOrder() {
    this.application.setAdditionalProfiles("other", "dev");
    ConfigurableApplicationContext context = this.application.run();
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromdevpropertiesfile");
  }

  @Test
  void runWhenProfilesPresentBeforeConfigFileProcessingAugmentsProfileActivatedByConfigFile() {
    this.application.setAdditionalProfiles("other");
    ConfigurableApplicationContext context = this.application.run("--app.config.name=enableprofile");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("other", "myprofile");
    String property = context.getEnvironment().getProperty("other.property");
    assertThat(property).isEqualTo("fromotherpropertiesfile");
    property = context.getEnvironment().getProperty("the.property");
    assertThat(property).isEqualTo("fromprofilepropertiesfile");
  }

  @Test
  void runWhenProfilePropertiesUsedInPlaceholdersLoadsWithResolvedPlaceholders() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=enableprofile");
    String property = context.getEnvironment().getProperty("one.more");
    assertThat(property).isEqualTo("fromprofilepropertiesfile");
  }

  @Test
  void runWhenDuplicateProfileSetProgrammaticallyAndViaPropertyLoadsWithProfiles() {
    this.application.setAdditionalProfiles("dev");
    ConfigurableApplicationContext context = this.application.run("--infra.profiles.active=dev,other");
    assertThat(context.getEnvironment().getActiveProfiles()).contains("dev", "other");
    assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
  }

  @Test
  void runWhenProfilesActivatedViaBracketNotationSetsProfiles() {
    ConfigurableApplicationContext context = this.application.run("--infra.profiles.active[0]=dev",
            "--infra.profiles.active[1]=other");
    assertThat(context.getEnvironment().getActiveProfiles()).contains("dev", "other");
    assertThat(context.getEnvironment().getProperty("my.property")).isEqualTo("fromotherpropertiesfile");
  }

  @Test
  void loadWhenProfileInMultiDocumentFilesLoadsExpectedProperties() {
    this.application.setAdditionalProfiles("dev");
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testprofiles",
            "--app.config.location=classpath:configdata/profiles/");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromdevprofile");
    property = context.getEnvironment().getProperty("my.other");
    assertThat(property).isEqualTo("notempty");
  }

  @Test
  void runWhenMultipleActiveProfilesWithMultiDocumentFilesLoadsInOrderOfDocument() {
    this.application.setAdditionalProfiles("other", "dev");
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testprofiles",
            "--app.config.location=classpath:configdata/profiles/");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromotherprofile");
    property = context.getEnvironment().getProperty("my.other");
    assertThat(property).isEqualTo("notempty");
    property = context.getEnvironment().getProperty("dev.property");
    assertThat(property).isEqualTo("devproperty");
  }

  @Test
  void runWhenHasAndProfileExpressionLoadsExpectedProperties() {
    assertProfileExpression("devandother", "dev", "other");
  }

  @Test
  void runWhenHasComplexProfileExpressionsLoadsExpectedProperties() {
    assertProfileExpression("devorotherandanother", "dev", "another");
  }

  @Test
  void runWhenProfileExpressionsDoNotMatchLoadsExpectedProperties() {
    assertProfileExpression("fromyamlfile", "dev");
  }

  @Test
  void runWhenHasNegatedProfilesLoadsExpectedProperties() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testnegatedprofiles",
            "--app.config.location=classpath:configdata/profiles/");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromnototherprofile");
    property = context.getEnvironment().getProperty("my.notother");
    assertThat(property).isEqualTo("foo");
  }

  @Test
  void runWhenHasNegatedProfilesWithProfileActiveLoadsExpectedProperties() {
    this.application.setAdditionalProfiles("other");
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testnegatedprofiles",
            "--app.config.location=classpath:configdata/profiles/");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromotherprofile");
    property = context.getEnvironment().getProperty("my.notother");
    assertThat(property).isNull();
  }

  @Test
  void runWhenHasActiveProfileConfigurationInMultiDocumentFileLoadsInExpectedOrder() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testsetprofiles",
            "--app.config.location=classpath:configdata/profiles/");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(context.getEnvironment().getActiveProfiles()).contains("dev");
    assertThat(property).isEqualTo("fromdevprofile");
    assertThat(context.getEnvironment().getPropertySources()).extracting("name").contains(
            "Config resource 'class path resource [configdata/profiles/testsetprofiles.yml]' via location 'classpath:configdata/profiles/' (document #0)",
            "Config resource 'class path resource [configdata/profiles/testsetprofiles.yml]' via location 'classpath:configdata/profiles/' (document #1)");
  }

  @Test
  void runWhenHasYamlWithCommaSeparatedMultipleProfilesLoadsExpectedProperties() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testsetmultiprofiles");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
  }

  @Test
  void runWhenHasYamlWithListProfilesLoadsExpectedProperties() {
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testsetmultiprofileslist");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
  }

  @Test
  void loadWhenHasWhitespaceTrims() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=testsetmultiprofileswhitespace");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "healthcheck");
  }

  @Test
  void loadWhenHasConfigLocationAsFile() {
    String location = "file:src/test/resources/specificlocation.properties";
    ConfigurableApplicationContext context = this.application.run("--app.config.location=" + location);
    assertThat(context.getEnvironment())
            .has(matchingPropertySource("Config resource 'file [" + Strings
                    .join(Arrays.asList("src", "test", "resources", "specificlocation.properties"), File.separatorChar)
                    + "]' via location '" + location + "'"));
  }

  @Test
  void loadWhenHasRelativeConfigLocationUsesFileLocation() {
    String location = "src/test/resources/specificlocation.properties";
    ConfigurableApplicationContext context = this.application.run("--app.config.location=" + location);
    assertThat(context.getEnvironment()).has(matchingPropertySource("Config resource 'file [" + Strings
            .join(Arrays.asList("src", "test", "resources", "specificlocation.properties"), File.separatorChar)
            + "]' via location '" + location + "'"));
  }

  @Test
  void loadWhenCustomDefaultProfileAndActiveFromPreviousSourceDoesNotActivateDefault() {
    ConfigurableApplicationContext context = this.application.run("--infra.profiles.default=customdefault",
            "--infra.profiles.active=dev");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo("fromdevpropertiesfile");
    assertThat(context.getEnvironment().containsProperty("customdefault")).isFalse();
  }

  @Test
  void runWhenCustomDefaultProfileSameAsActiveFromFileActivatesProfile() {
    ConfigurableApplicationContext context = this.application.run(
            "--app.config.location=classpath:configdata/profiles/", "--infra.profiles.default=customdefault",
            "--app.config.name=customprofile");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.containsProperty("customprofile")).isTrue();
    assertThat(environment.containsProperty("customprofile-customdefault")).isTrue();
    assertThat(environment.acceptsProfiles(Profiles.of("customdefault"))).isTrue();
  }

  @Test
  void runWhenActiveProfilesCanBeConfiguredUsingPlaceholdersResolvedAgainstTheEnvironmentLoadsExpectedProperties() {
    ConfigurableApplicationContext context = this.application.run("--activeProfile=testPropertySource",
            "--app.config.name=testactiveprofiles");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("testPropertySource");
  }

  @Test
  void runWhenHasAdditionalLocationLoadsWithAdditionalTakingPrecedenceOverDefaultLocation() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.additional-location=classpath:override.properties");
    assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
    assertThat(context.getEnvironment().getProperty("value")).isEqualTo("1234");
  }

  @Test
  void runWhenMultipleAdditionalLocationsLoadsWithLastWinning() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.additional-location=classpath:override.properties,classpath:some.properties");
    assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("spam");
    assertThat(context.getEnvironment().getProperty("value")).isEqualTo("1234");
  }

  @Test
  void runWhenAdditionalLocationAndLocationLoadsWithAdditionalTakingPrecedenceOverConfigured() {
    ConfigurableApplicationContext context = this.application.run(
            "--app.config.location=classpath:some.properties",
            "--app.config.additional-location=classpath:override.properties");
    assertThat(context.getEnvironment().getProperty("foo")).isEqualTo("bar");
    assertThat(context.getEnvironment().getProperty("value")).isNull();
  }

  @Test
  void runWhenPropertiesFromCustomPropertySourceLoaderShouldLoadFromCustomSource() {
    ConfigurableApplicationContext context = this.application.run();
    assertThat(context.getEnvironment().getProperty("customloader1")).isEqualTo("true");
  }

  @Test
  void runWhenCustomDefaultPropertySourceLoadsWithoutReplacingCustomSource() {
    // gh-17011
    Map<String, Object> source = new HashMap<>();
    source.put("mapkey", "mapvalue");
    MapPropertySource propertySource = new MapPropertySource("defaultProperties", source) {

      @Override
      public Object getProperty(String name) {
        if ("app.config.name".equals(name)) {
          return "gh17001";
        }
        return super.getProperty(name);
      }

    };
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(propertySource);
    this.application.setEnvironment(environment);
    ConfigurableApplicationContext context = this.application.run();
    assertThat(context.getEnvironment().getProperty("mapkey")).isEqualTo("mapvalue");
    assertThat(context.getEnvironment().getProperty("gh17001loaded")).isEqualTo("true");
  }

  @Test
  void runWhenConfigLocationHasUnknownFileExtensionFailsFast() {
    String location = "classpath:application.unknown";
    assertThatIllegalStateException().isThrownBy(() -> this.application.run("--app.config.location=" + location))
            .withMessageContaining("Unable to load config data").withMessageContaining(location)
            .satisfies((ex) -> assertThat(ex.getCause()).hasMessageContaining("File extension is not known")
                    .hasMessageContaining("it must end in '/'"));
  }

  @Test
  void runWhenConfigLocationHasOptionalMissingDirectoryContinuesToLoad() {
    String location = "optional:classpath:application.unknown/";
    this.application.run("--app.config.location=" + location);
  }

  @Test
  void runWhenConfigLocationHasNonOptionalMissingFileDirectoryThrowsResourceNotFoundException() {
    File location = new File(this.temp, "application.unknown");
    assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(() -> this.application
            .run("--app.config.location=" + StringUtils.cleanPath(location.getAbsolutePath()) + "/"));
  }

  @Test
  void runWhenConfigLocationHasNonOptionalMissingClasspathDirectoryThrowsLocationNotFoundException() {
    String location = "classpath:application.unknown/";
    assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
            .isThrownBy(() -> this.application.run("--app.config.location=" + location));
  }

  @Test
  void runWhenConfigLocationHasNonOptionalEmptyFileDirectoryDoesNotThrowException() {
    File location = new File(this.temp, "application.empty");
    location.mkdirs();
    assertThatNoException().isThrownBy(() -> this.application
            .run("--app.config.location=" + StringUtils.cleanPath(location.getAbsolutePath()) + "/"));
  }

  @Test
  void runWhenConfigLocationHasMandatoryDirectoryThatDoesntExistThrowsException() {
    assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(
            () -> this.application.run("--app.config.location=" + StringUtils.cleanPath("invalid/")));
  }

  @Test
  void runWhenConfigLocationHasNonOptionalEmptyFileDoesNotThrowException() throws IOException {
    File location = new File(this.temp, "application.properties");
    FileCopyUtils.copy(new byte[0], location);
    assertThatNoException()
            .isThrownBy(() -> this.application.run("--app.config.location=classpath:/application.properties,"
                    + StringUtils.cleanPath(location.getAbsolutePath())));
  }

  @Test
  void runWhenResolvedIsOptionalDoesNotThrowException() {
    ApplicationContext context = this.application.run("--app.config.location=test:optionalresult");
    assertThat(context.getEnvironment().containsProperty("spring")).isFalse();
  }

  @Test
  @Disabled("Disabled until infra.profiles suppport is dropped")
  void runWhenUsingInvalidPropertyThrowsException() {
    assertThatExceptionOfType(InvalidConfigDataPropertyException.class).isThrownBy(
            () -> this.application.run("--app.config.location=classpath:invalidproperty.properties"));
  }

  @Test
  void runWhenImportUsesPlaceholder() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:application-import-with-placeholder.properties");
    assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("iwasimported");
  }

  @Test
  void runWhenImportFromEarlierDocumentUsesPlaceholder() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:application-import-with-placeholder-in-document.properties");
    assertThat(context.getEnvironment().getProperty("my.value")).isEqualTo("iwasimported");
  }

  @Test
    // gh-26858
  void runWhenImportWithProfileVariantOrdersPropertySourcesCorrectly() {
    this.application.setAdditionalProfiles("dev");
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:application-import-with-profile-variant.properties");
    assertThat(context.getEnvironment().getProperty("my.value"))
            .isEqualTo("application-import-with-profile-variant-imported-dev");
  }

  @Test
  void runWhenImportWithProfileVariantAndDirectProfileImportOrdersPropertySourcesCorrectly() {
    this.application.setAdditionalProfiles("dev");
    ConfigurableApplicationContext context = this.application.run(
            "--app.config.location=classpath:application-import-with-profile-variant-and-direct-profile-import.properties");
    assertThat(context.getEnvironment().getProperty("my.value"))
            .isEqualTo("application-import-with-profile-variant-imported-dev");
  }

  @Test
  void runWhenHasPropertyInProfileDocumentThrowsException() {
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.application.run(
                    "--app.config.location=classpath:application-import-with-placeholder-in-profile-document.properties"))
            .withCauseInstanceOf(InactiveConfigDataAccessException.class);
  }

  @Test
    // gh-29386
  void runWhenHasPropertyInEarlierProfileDocumentThrowsException() {
    assertThatExceptionOfType(BindException.class).isThrownBy(() -> this.application.run(
                    "--app.config.location=classpath:application-import-with-placeholder-in-earlier-profile-document.properties"))
            .withCauseInstanceOf(InactiveConfigDataAccessException.class);
  }

  @Test
    // gh-29386
  void runWhenHasPropertyInEarlierDocumentLoads() {
    ConfigurableApplicationContext context = this.application.run(
            "--app.config.location=classpath:application-import-with-placeholder-in-earlier-document.properties");
    assertThat(context.getEnvironment().getProperty("my.value"))
            .isEqualTo("application-import-with-placeholder-in-earlier-document-imported");
  }

  @Test
  void runWhenHasNonOptionalImportThrowsException() {
    assertThatExceptionOfType(ConfigDataResourceNotFoundException.class).isThrownBy(
            () -> this.application.run("--app.config.location=classpath:missing-appplication.properties"));
  }

  @Test
  void runWhenHasNonOptionalImportAndIgnoreNotFoundPropertyDoesNotThrowException() {
    this.application.run("--app.config.on-not-found=ignore",
            "--app.config.location=classpath:missing-appplication.properties");
  }

  @Test
  void runWhenHasIncludedProfilesActivatesProfiles() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:application-include-profiles.properties");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
            "p5");
  }

  @Test
  void runWhenHasIncludedProfilesWithPlaceholderActivatesProfiles() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:application-include-profiles-with-placeholder.properties");
    assertThat(context.getEnvironment().getActiveProfiles()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4",
            "p5");
  }

  @Test
  void runWhenHasIncludedProfilesWithProfileSpecificDocumentThrowsException() {
    assertThatExceptionOfType(InactiveConfigDataAccessException.class).isThrownBy(() -> this.application.run(
            "--app.config.location=classpath:application-include-profiles-in-profile-specific-document.properties"));
  }

  @Test
  void runWhenHasIncludedProfilesWithListSyntaxWithProfileSpecificDocumentThrowsException() {
    assertThatExceptionOfType(InvalidConfigDataPropertyException.class).isThrownBy(() -> this.application.run(
            "--app.config.name=application-include-profiles-list-in-profile-specific-file",
            "--infra.profiles.active=test"));
  }

  @Test
  void runWhenImportingIncludesParentOrigin() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.location=classpath:application-import-with-placeholder.properties");
    Binder binder = Binder.get(context.getEnvironment());
    List<ConfigurationProperty> properties = new ArrayList<>();
    BindHandler bindHandler = new BindHandler() {

      @Override
      public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context,
              Object result) {
        properties.add(context.getConfigurationProperty());
        return result;
      }

    };
    binder.bind("my.value", Bindable.of(String.class), bindHandler);
    assertThat(properties).hasSize(1);
    Origin origin = properties.get(0).getOrigin();
    assertThat(origin.toString()).contains("application-import-with-placeholder-imported");
    assertThat(origin.getParent().toString()).contains("application-import-with-placeholder");
  }

  @Test
  void runWhenHasWildcardLocationLoadsFromAllMatchingLocations() {
    ConfigurableApplicationContext context = this.application.run(
            "--app.config.location=file:src/test/resources/config/*/", "--app.config.name=testproperties");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.getProperty("first.property")).isEqualTo("apple");
    assertThat(environment.getProperty("second.property")).isEqualTo("ball");
  }

  @Test
  void runWhenOptionalWildcardLocationDoesNotExistDoesNotThrowException() {
    assertThatNoException().isThrownBy(() -> this.application.run(
            "--app.config.location=optional:file:src/test/resources/nonexistent/*/testproperties.properties"));
  }

  @Test
  void runWhenMandatoryWildcardLocationDoesNotExistThrowsException() {
    assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(() -> this.application
            .run("--app.config.location=file:src/test/resources/nonexistent/*/testproperties.properties"));
  }

  @Test
  void runWhenMandatoryWildcardLocationHasEmptyFileDirectory() {
    assertThatNoException()
            .isThrownBy(() -> this.application.run("--app.config.location=file:src/test/resources/config/*/"));
  }

  @Test
  void runWhenMandatoryWildcardLocationHasNoSubdirectories() {
    assertThatExceptionOfType(ConfigDataLocationNotFoundException.class).isThrownBy(
                    () -> this.application.run("--app.config.location=file:src/test/resources/config/0-empty/*/"))
            .withMessage(
                    "Config data location 'file:src/test/resources/config/0-empty/*/' contains no subdirectories");
  }

  @Test
  void runWhenHasMandatoryWildcardLocationThatDoesNotExist() {
    assertThatExceptionOfType(ConfigDataLocationNotFoundException.class)
            .isThrownBy(() -> this.application.run("--app.config.location=file:invalid/*/"));
  }

  @Test
  void runWhenHasOptionalWildcardLocationThatDoesNotExistDoesNotThrow() {
    assertThatNoException()
            .isThrownBy(() -> this.application.run("--app.config.location=optional:file:invalid/*/"));
  }

  @Test
  void runWhenOptionalWildcardLocationHasNoSubdirectoriesDoesNotThrow() {
    assertThatNoException().isThrownBy(() -> this.application
            .run("--app.config.location=optional:file:src/test/resources/config/0-empty/*/"));
  }

  @Test
    // gh-24990
  void runWhenHasProfileSpecificFileWithActiveOnProfileProperty() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=application-activate-on-profile-in-profile-specific-file");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.getProperty("test1")).isEqualTo("test1");
    assertThat(environment.getProperty("test2")).isEqualTo("test2");
  }

  @Test
    // gh-26960
  void runWhenHasProfileSpecificImportWithImportImportsSecondProfileSpecificFile() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=application-profile-specific-import-with-import");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.containsProperty("application-profile-specific-import-with-import")).isTrue();
    assertThat(environment.containsProperty("application-profile-specific-import-with-import-p1")).isTrue();
    assertThat(environment.containsProperty("application-profile-specific-import-with-import-p2")).isFalse();
    assertThat(environment.containsProperty("application-profile-specific-import-with-import-import")).isTrue();
    assertThat(environment.containsProperty("application-profile-specific-import-with-import-import-p1")).isTrue();
    assertThat(environment.containsProperty("application-profile-specific-import-with-import-import-p2")).isTrue();
  }

  @Test
    // gh-26960
  void runWhenHasProfileSpecificImportWithCustomImportResolvesProfileSpecific() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=application-profile-specific-import-with-custom-import");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.containsProperty("test:boot")).isTrue();
    assertThat(environment.containsProperty("test:boot:ps")).isTrue();
  }

  @Test
    // gh-26593
  void runWhenHasFilesInRootAndConfigWithProfiles() {
    ConfigurableApplicationContext context = this.application
            .run("--app.config.name=file-in-root-and-config-with-profile", "--infra.profiles.active=p1,p2");
    ConfigurableEnvironment environment = context.getEnvironment();
    assertThat(environment.containsProperty("file-in-root-and-config-with-profile")).isTrue();
    assertThat(environment.containsProperty("file-in-root-and-config-with-profile-p1")).isTrue();
    assertThat(environment.containsProperty("file-in-root-and-config-with-profile-p2")).isTrue();
    assertThat(environment.containsProperty("config-file-in-root-and-config-with-profile")).isTrue();
    assertThat(environment.containsProperty("config-file-in-root-and-config-with-profile-p1")).isTrue();
    assertThat(environment.containsProperty("config-file-in-root-and-config-with-profile-p2")).isTrue();
    assertThat(environment.getProperty("v1")).isEqualTo("config-file-in-root-and-config-with-profile-p2");
    assertThat(environment.getProperty("v2")).isEqualTo("file-in-root-and-config-with-profile-p2");
  }

  private Condition<ConfigurableEnvironment> matchingPropertySource(final String sourceName) {
    return new Condition<ConfigurableEnvironment>("environment containing property source " + sourceName) {

      @Override
      public boolean matches(ConfigurableEnvironment value) {
        value.getPropertySources().forEach((ps) -> System.out.println(ps.getName()));
        return value.getPropertySources().contains(sourceName);
      }

    };
  }

  private void assertProfileExpression(String value, String... activeProfiles) {
    this.application.setAdditionalProfiles(activeProfiles);
    ConfigurableApplicationContext context = this.application.run("--app.config.name=testprofileexpression",
            "--app.config.location=classpath:configdata/profiles/");
    String property = context.getEnvironment().getProperty("my.property");
    assertThat(property).isEqualTo(value);
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

  }

  static class LocationResolver implements ConfigDataLocationResolver<TestConfigDataResource> {

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
      return location.hasPrefix("test:");

    }

    @Override
    public List<TestConfigDataResource> resolve(ConfigDataLocationResolverContext context,
            ConfigDataLocation location)
            throws ConfigDataLocationNotFoundException, ConfigDataResourceNotFoundException {
      return Collections.singletonList(new TestConfigDataResource(location, false));
    }

    @Override
    public List<TestConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context,
            ConfigDataLocation location, cn.taketoday.framework.context.config.Profiles profiles)
            throws ConfigDataLocationNotFoundException {
      return Collections.singletonList(new TestConfigDataResource(location, true));
    }

  }

  static class Loader implements ConfigDataLoader<TestConfigDataResource> {

    @Override
    public ConfigData load(ConfigDataLoaderContext context, TestConfigDataResource resource)
            throws IOException, ConfigDataResourceNotFoundException {
      if (resource.isOptional()) {
        return null;
      }
      Map<String, Object> map = new LinkedHashMap<>();
      if (!resource.isProfileSpecific()) {
        map.put("spring", "boot");
      }
      String suffix = (!resource.isProfileSpecific()) ? "" : ":ps";
      map.put(resource.toString() + suffix, "true");
      MapPropertySource propertySource = new MapPropertySource("loaded" + suffix, map);
      return new ConfigData(Collections.singleton(propertySource));
    }

  }

  static class TestConfigDataResource extends ConfigDataResource {

    private final ConfigDataLocation location;

    private boolean profileSpecific;

    TestConfigDataResource(ConfigDataLocation location, boolean profileSpecific) {
      super(location.toString().contains("optionalresult"));
      this.location = location;
      this.profileSpecific = profileSpecific;
    }

    boolean isProfileSpecific() {
      return this.profileSpecific;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      TestConfigDataResource other = (TestConfigDataResource) obj;
      return ObjectUtils.nullSafeEquals(this.location, other.location)
              && this.profileSpecific == other.profileSpecific;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public String toString() {
      return this.location.toString();
    }

  }

}
