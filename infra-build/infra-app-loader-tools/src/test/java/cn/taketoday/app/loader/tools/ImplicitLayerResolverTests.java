/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ImplicitLayerResolver}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ImplicitLayerResolverTests {

	private final Layers layers = Layers.IMPLICIT;

	@Test
	void iteratorReturnsLayers() {
		assertThat(this.layers).containsExactly(StandardLayers.DEPENDENCIES, StandardLayers.INFRA_APP_LOADER,
				StandardLayers.SNAPSHOT_DEPENDENCIES, StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenNameInResourceLocationReturnsApplicationLayer() {
		assertThat(this.layers.getLayer("META-INF/resources/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("resources/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("static/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("public/logo.gif")).isEqualTo(StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenNameIsClassInResourceLocationReturnsApplicationLayer() {
		assertThat(this.layers.getLayer("META-INF/resources/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("resources/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("static/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("public/Logo.class")).isEqualTo(StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenNameNotInResourceLocationReturnsApplicationLayer() {
		assertThat(this.layers.getLayer("com/example/Application.class")).isEqualTo(StandardLayers.APPLICATION);
		assertThat(this.layers.getLayer("com/example/application.properties")).isEqualTo(StandardLayers.APPLICATION);
	}

	@Test
	void getLayerWhenLoaderClassReturnsLoaderLayer() {
		assertThat(this.layers.getLayer("cn/taketoday/app/loader/Launcher.class"))
			.isEqualTo(StandardLayers.INFRA_APP_LOADER);
		assertThat(this.layers.getLayer("cn/taketoday/app/loader/Utils.class"))
			.isEqualTo(StandardLayers.INFRA_APP_LOADER);
	}

	@Test
	void getLayerWhenLibraryIsSnapshotReturnsSnapshotLayer() {
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0.BUILD-SNAPSHOT.jar")))
			.isEqualTo(StandardLayers.SNAPSHOT_DEPENDENCIES);
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0-SNAPSHOT.jar")))
			.isEqualTo(StandardLayers.SNAPSHOT_DEPENDENCIES);
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0.SNAPSHOT.jar")))
			.isEqualTo(StandardLayers.SNAPSHOT_DEPENDENCIES);
	}

	@Test
	void getLayerWhenLibraryIsNotSnapshotReturnsDependenciesLayer() {
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0.jar"))).isEqualTo(StandardLayers.DEPENDENCIES);
		assertThat(this.layers.getLayer(mockLibrary("spring-boot.2.0.0-classified.jar")))
			.isEqualTo(StandardLayers.DEPENDENCIES);
	}

	private Library mockLibrary(String name) {
		Library library = mock(Library.class);
		given(library.getName()).willReturn(name);
		return library;
	}

}
