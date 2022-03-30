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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.framework.env.PropertiesPropertySourceLoader;
import cn.taketoday.framework.logging.DeferredLog;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.mock.env.MockEnvironment;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StandardConfigDataLocationResolver}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class StandardConfigDataLocationResolverTests {

	private StandardConfigDataLocationResolver resolver;

	private ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

	private MockEnvironment environment;

	private Binder environmentBinder;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.environmentBinder = Binder.get(this.environment);
		this.resolver = new StandardConfigDataLocationResolver(new DeferredLog(), this.environmentBinder,
				this.resourceLoader);
	}

	@Test
	void isResolvableAlwaysReturnsTrue() {
		assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.of("test"))).isTrue();
	}

	@Test
	void resolveWhenLocationIsDirectoryResolvesAllMatchingFilesInDirectory() {
		ConfigDataLocation location = ConfigDataLocation.of("classpath:/configdata/properties/");
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		Assertions.assertThat(locations).extracting(Object::toString)
				.containsExactly("class path resource [configdata/properties/application.properties]");
	}

	@Test
	void resolveWhenLocationIsFileResolvesFile() {
		ConfigDataLocation location = ConfigDataLocation
				.of("file:src/test/resources/configdata/properties/application.properties");
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		Assertions.assertThat(locations).extracting(Object::toString).containsExactly(
				filePath("src", "test", "resources", "configdata", "properties", "application.properties"));
	}

	@Test
	void resolveWhenLocationIsFileAndNoMatchingLoaderThrowsException() {
		ConfigDataLocation location = ConfigDataLocation
				.of("file:src/test/resources/configdata/properties/application.unknown");
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Unable to load config data from")
				.satisfies((ex) -> assertThat(ex.getCause()).hasMessageStartingWith("File extension is not known"));
	}

	@Test
	void resolveWhenLocationWildcardIsSpecifiedForClasspathLocationThrowsException() {
		ConfigDataLocation location = ConfigDataLocation.of("classpath*:application.properties");
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageContaining("Location 'classpath*:application.properties' cannot use classpath wildcards");
	}

	@Test
	void resolveWhenLocationWildcardIsNotBeforeLastSlashThrowsException() {
		ConfigDataLocation location = ConfigDataLocation.of("file:src/test/resources/*/config/");
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Location '").withMessageEndingWith("' must end with '*/'");
	}

	@Test
	void createWhenConfigNameHasWildcardThrowsException() {
		this.environment.setProperty("context.config.name", "*/application");
		assertThatIllegalStateException()
				.isThrownBy(
						() -> new StandardConfigDataLocationResolver(null, this.environmentBinder, this.resourceLoader))
				.withMessageStartingWith("Config name '").withMessageEndingWith("' cannot contain '*'");
	}

	@Test
	void resolveWhenLocationHasMultipleWildcardsThrowsException() {
		ConfigDataLocation location = ConfigDataLocation.of("file:src/test/resources/config/**/");
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Location '").withMessageEndingWith("' cannot contain multiple wildcards");
	}

	@Test
	void resolveWhenLocationIsWildcardDirectoriesRestrictsToOneLevelDeep() {
		ConfigDataLocation location = ConfigDataLocation.of("file:src/test/resources/config/*/");
		this.environment.setProperty("context.config.name", "testproperties");
		this.resolver = new StandardConfigDataLocationResolver(null, this.environmentBinder, this.resourceLoader);
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(3);
		Assertions.assertThat(locations).extracting(Object::toString)
				.contains(filePath("src", "test", "resources", "config", "1-first", "testproperties.properties"))
				.contains(filePath("src", "test", "resources", "config", "2-second", "testproperties.properties"))
				.doesNotContain(filePath("src", "test", "resources", "config", "3-third", "testproperties.properties"));
	}

	@Test
	void resolveWhenLocationIsWildcardDirectoriesSortsAlphabeticallyBasedOnAbsolutePath() {
		ConfigDataLocation location = ConfigDataLocation.of("file:src/test/resources/config/*/");
		this.environment.setProperty("context.config.name", "testproperties");
		this.resolver = new StandardConfigDataLocationResolver(null, this.environmentBinder, this.resourceLoader);
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		Assertions.assertThat(locations).extracting(Object::toString).containsExactly(
				filePath("src", "test", "resources", "config", "0-empty", "testproperties.properties"),
				filePath("src", "test", "resources", "config", "1-first", "testproperties.properties"),
				filePath("src", "test", "resources", "config", "2-second", "testproperties.properties"));
	}

	@Test
	void resolveWhenLocationIsWildcardAndMatchingFilePresentShouldNotFail() {
		ConfigDataLocation location = ConfigDataLocation.of("optional:file:src/test/resources/a-file/*/");
		assertThatNoException().isThrownBy(() -> this.resolver.resolve(this.context, location));
	}

	@Test
	void resolveWhenLocationIsWildcardFilesLoadsAllFilesThatMatch() {
		ConfigDataLocation location = ConfigDataLocation
				.of("file:src/test/resources/config/*/testproperties.properties");
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(3);
		Assertions.assertThat(locations).extracting(Object::toString)
				.contains(filePath("src", "test", "resources", "config", "1-first", "testproperties.properties"))
				.contains(filePath("src", "test", "resources", "config", "2-second", "testproperties.properties"))
				.doesNotContain(filePath("src", "test", "resources", "config", "nested", "3-third",
						"testproperties.properties"));
	}

	@Test
	void resolveWhenLocationIsRelativeAndFileResolves() {
		this.environment.setProperty("context.config.name", "other");
		ConfigDataLocation location = ConfigDataLocation.of("other.properties");
		this.resolver = new StandardConfigDataLocationResolver(new DeferredLog(), this.environmentBinder,
				this.resourceLoader);
		StandardConfigDataReference parentReference = new StandardConfigDataReference(
				ConfigDataLocation.of("classpath:configdata/properties/application.properties"), null,
				"classpath:configdata/properties/application", null, "properties",
				new PropertiesPropertySourceLoader());
		ClassPathResource parentResource = new ClassPathResource("configdata/properties/application.properties");
		StandardConfigDataResource parent = new StandardConfigDataResource(parentReference, parentResource);
		given(this.context.getParent()).willReturn(parent);
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		Assertions.assertThat(locations).extracting(Object::toString)
				.contains("class path resource [configdata/properties/other.properties]");
	}

	@Test
	void resolveWhenLocationIsRelativeAndDirectoryResolves() {
		this.environment.setProperty("context.config.name", "testproperties");
		ConfigDataLocation location = ConfigDataLocation.of("nested/3-third/");
		this.resolver = new StandardConfigDataLocationResolver(new DeferredLog(), this.environmentBinder,
				this.resourceLoader);
		StandardConfigDataReference parentReference = new StandardConfigDataReference(
				ConfigDataLocation.of("optional:classpath:configdata/"), null, "classpath:config/specific", null,
				"properties", new PropertiesPropertySourceLoader());
		ClassPathResource parentResource = new ClassPathResource("config/specific.properties");
		StandardConfigDataResource parent = new StandardConfigDataResource(parentReference, parentResource);
		given(this.context.getParent()).willReturn(parent);
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		Assertions.assertThat(locations).extracting(Object::toString)
				.contains("class path resource [config/nested/3-third/testproperties.properties]");
	}

	@Test
	void resolveWhenLocationIsRelativeAndNoMatchingLoaderThrowsException() {
		ConfigDataLocation location = ConfigDataLocation.of("application.other");
		StandardConfigDataReference parentReference = new StandardConfigDataReference(
				ConfigDataLocation.of("classpath:configdata/properties/application.properties"), null,
				"configdata/properties/application", null, "properties", new PropertiesPropertySourceLoader());
		ClassPathResource parentResource = new ClassPathResource("configdata/properties/application.properties");
		StandardConfigDataResource parent = new StandardConfigDataResource(parentReference, parentResource);
		given(this.context.getParent()).willReturn(parent);
		assertThatIllegalStateException().isThrownBy(() -> this.resolver.resolve(this.context, location))
				.withMessageStartingWith("Unable to load config data from 'application.other'")
				.satisfies((ex) -> assertThat(ex.getCause()).hasMessageStartingWith("File extension is not known"));
	}

	@Test
	void resolveWhenLocationUsesOptionalExtensionSyntaxResolves() throws Exception {
		ConfigDataLocation location = ConfigDataLocation.of("classpath:/application-props-no-extension[.properties]");
		List<StandardConfigDataResource> locations = this.resolver.resolve(this.context, location);
		assertThat(locations.size()).isEqualTo(1);
		StandardConfigDataResource resolved = locations.get(0);
		assertThat(resolved.getResource().getFilename()).endsWith("application-props-no-extension");
		ConfigData loaded = new StandardConfigDataLoader().load(null, resolved);
		PropertySource<?> propertySource = loaded.getPropertySources().get(0);
		assertThat(propertySource.getProperty("withnotext")).isEqualTo("test");
	}

	@Test
	void resolveProfileSpecificReturnsProfileSpecificFiles() {
		ConfigDataLocation location = ConfigDataLocation.of("classpath:/configdata/properties/");
		Profiles profiles = mock(Profiles.class);
		given(profiles.iterator()).willReturn(Collections.singletonList("dev").iterator());
		List<StandardConfigDataResource> locations = this.resolver.resolveProfileSpecific(this.context, location,
				profiles);
		assertThat(locations.size()).isEqualTo(1);
		Assertions.assertThat(locations).extracting(Object::toString)
				.containsExactly("class path resource [configdata/properties/application-dev.properties]");
	}

	@Test
	void resolveProfileSpecificWhenLocationIsFileReturnsEmptyList() {
		ConfigDataLocation location = ConfigDataLocation.of("classpath:/configdata/properties/application.properties");
		Profiles profiles = mock(Profiles.class);
		given(profiles.iterator()).willReturn(Collections.emptyIterator());
		given(profiles.getActive()).willReturn(Collections.singletonList("dev"));
		List<StandardConfigDataResource> locations = this.resolver.resolveProfileSpecific(this.context, location,
				profiles);
		Assertions.assertThat(locations).isEmpty();
	}

	private String filePath(String... components) {
		return "file [" + String.join(File.separator, components) + "]";
	}

}
