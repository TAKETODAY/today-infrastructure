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

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.BootstrapRegistry.InstanceSupplier;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.DefaultBootstrapContext;
import cn.taketoday.framework.logging.DeferredLogFactory;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.mock.env.MockPropertySource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLoaders}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigDataLoadersTests {

	private DeferredLogFactory logFactory = Supplier::get;

	private DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();

	private ConfigDataLoaderContext context = mock(ConfigDataLoaderContext.class);

	@Test
	void createWhenLoaderHasLogParameterInjectsLog() {
		new ConfigDataLoaders(this.logFactory, this.bootstrapContext, null,
				Arrays.asList(LoggingConfigDataLoader.class.getName()));
	}

	@Test
	void createWhenLoaderHasDeferredLogFactoryParameterInjectsDeferredLogFactory() {
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext, null,
				Arrays.asList(DeferredLogFactoryConfigDataLoader.class.getName()));
		assertThat(loaders).extracting("loaders").asList()
				.satisfies(this::containsValidDeferredLogFactoryConfigDataLoader);
	}

	private void containsValidDeferredLogFactoryConfigDataLoader(List<?> list) {
		assertThat(list).hasSize(1);
		DeferredLogFactoryConfigDataLoader loader = (DeferredLogFactoryConfigDataLoader) list.get(0);
		assertThat(loader.getLogFactory()).isSameAs(this.logFactory);
	}

	@Test
	void createWhenLoaderHasBootstrapParametersInjectsBootstrapContext() {
		new ConfigDataLoaders(this.logFactory, this.bootstrapContext, null,
				Arrays.asList(BootstrappingConfigDataLoader.class.getName()));
		assertThat(this.bootstrapContext.get(String.class)).isEqualTo("boot");
	}

	@Test
	void loadWhenSingleLoaderSupportsLocationReturnsLoadedConfigData() throws Exception {
		TestConfigDataResource location = new TestConfigDataResource("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext, null,
				Arrays.asList(TestConfigDataLoader.class.getName()));
		ConfigData loaded = loaders.load(this.context, location);
		assertThat(getLoader(loaded)).isInstanceOf(TestConfigDataLoader.class);
	}

	@Test
	void loadWhenMultipleLoadersSupportLocationThrowsException() {
		TestConfigDataResource location = new TestConfigDataResource("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext, null,
				Arrays.asList(LoggingConfigDataLoader.class.getName(), TestConfigDataLoader.class.getName()));
		assertThatIllegalStateException().isThrownBy(() -> loaders.load(this.context, location))
				.withMessageContaining("Multiple loaders found for resource 'test'");
	}

	@Test
	void loadWhenNoLoaderSupportsLocationThrowsException() {
		TestConfigDataResource location = new TestConfigDataResource("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext, null,
				Arrays.asList(NonLoadableConfigDataLoader.class.getName()));
		assertThatIllegalStateException().isThrownBy(() -> loaders.load(this.context, location))
				.withMessage("No loader found for resource 'test'");
	}

	@Test
	void loadWhenGenericTypeDoesNotMatchSkipsLoader() throws Exception {
		TestConfigDataResource location = new TestConfigDataResource("test");
		ConfigDataLoaders loaders = new ConfigDataLoaders(this.logFactory, this.bootstrapContext, null,
				Arrays.asList(OtherConfigDataLoader.class.getName(), SpecificConfigDataLoader.class.getName()));
		ConfigData loaded = loaders.load(this.context, location);
		assertThat(getLoader(loaded)).isInstanceOf(SpecificConfigDataLoader.class);
	}

	private ConfigDataLoader<?> getLoader(ConfigData loaded) {
		return (ConfigDataLoader<?>) loaded.getPropertySources().get(0).getProperty("loader");
	}

	private static ConfigData createConfigData(ConfigDataLoader<?> loader, ConfigDataResource resource) {
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("loader", loader);
		propertySource.setProperty("resource", resource);
		List<PropertySource<?>> propertySources = Arrays.asList(propertySource);
		return new ConfigData(propertySources);
	}

	static class TestConfigDataResource extends ConfigDataResource {

		private final String value;

		TestConfigDataResource(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

	}

	static class OtherConfigDataResource extends ConfigDataResource {

	}

	static class LoggingConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		LoggingConfigDataLoader(Log log) {
			assertThat(log).isNotNull();
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			throw new AssertionError("Unexpected call");
		}

	}

	static class DeferredLogFactoryConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		private final DeferredLogFactory logFactory;

		DeferredLogFactoryConfigDataLoader(DeferredLogFactory logFactory) {
			assertThat(logFactory).isNotNull();
			this.logFactory = logFactory;
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			throw new AssertionError("Unexpected call");
		}

		DeferredLogFactory getLogFactory() {
			return this.logFactory;
		}

	}

	static class BootstrappingConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		BootstrappingConfigDataLoader(ConfigurableBootstrapContext configurableBootstrapContext,
				BootstrapRegistry bootstrapRegistry, BootstrapContext bootstrapContext) {
			assertThat(configurableBootstrapContext).isNotNull();
			assertThat(bootstrapRegistry).isNotNull();
			assertThat(bootstrapContext).isNotNull();
			assertThat(configurableBootstrapContext).isEqualTo(bootstrapRegistry).isEqualTo(bootstrapContext);
			bootstrapRegistry.register(String.class, InstanceSupplier.of("boot"));
		}

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			throw new AssertionError("Unexpected call");
		}

	}

	static class TestConfigDataLoader implements ConfigDataLoader<ConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, ConfigDataResource resource) throws IOException {
			return createConfigData(this, resource);
		}

	}

	static class NonLoadableConfigDataLoader extends TestConfigDataLoader {

		@Override
		public boolean isLoadable(ConfigDataLoaderContext context, ConfigDataResource resource) {
			return false;
		}

	}

	static class SpecificConfigDataLoader implements ConfigDataLoader<TestConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, TestConfigDataResource location) throws IOException {
			return createConfigData(this, location);
		}

	}

	static class OtherConfigDataLoader implements ConfigDataLoader<OtherConfigDataResource> {

		@Override
		public ConfigData load(ConfigDataLoaderContext context, OtherConfigDataResource location) throws IOException {
			return createConfigData(this, location);
		}

	}

}
