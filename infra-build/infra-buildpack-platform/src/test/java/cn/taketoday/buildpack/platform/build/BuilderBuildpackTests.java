/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.build;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BuilderBuildpack}.
 *
 * @author Scott Frederick
 */
class BuilderBuildpackTests extends AbstractJsonTests {

  private BuildpackResolverContext resolverContext;

  @BeforeEach
  void setUp() throws Exception {
    BuilderMetadata metadata = BuilderMetadata.fromJson(getContentAsString("builder-metadata.json"));
    this.resolverContext = mock(BuildpackResolverContext.class);
    given(this.resolverContext.getBuildpackMetadata()).willReturn(metadata.getBuildpacks());
  }

  @Test
  void resolveWhenFullyQualifiedBuildpackWithVersionResolves() throws Exception {
    BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:paketo-buildpacks/spring-boot@3.5.0");
    Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack.getCoordinates())
            .isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
    assertThatNoLayersAreAdded(buildpack);
  }

  @Test
  void resolveWhenFullyQualifiedBuildpackWithoutVersionResolves() throws Exception {
    BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:paketo-buildpacks/spring-boot");
    Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack.getCoordinates())
            .isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
    assertThatNoLayersAreAdded(buildpack);
  }

  @Test
  void resolveWhenUnqualifiedBuildpackWithVersionResolves() throws Exception {
    BuildpackReference reference = BuildpackReference.of("paketo-buildpacks/spring-boot@3.5.0");
    Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack.getCoordinates())
            .isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
    assertThatNoLayersAreAdded(buildpack);
  }

  @Test
  void resolveWhenUnqualifiedBuildpackWithoutVersionResolves() throws Exception {
    BuildpackReference reference = BuildpackReference.of("paketo-buildpacks/spring-boot");
    Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack.getCoordinates())
            .isEqualTo(BuildpackCoordinates.of("paketo-buildpacks/spring-boot", "3.5.0"));
    assertThatNoLayersAreAdded(buildpack);
  }

  @Test
  void resolveWhenFullyQualifiedBuildpackWithVersionNotInBuilderThrowsException() {
    BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:example/buildpack1@1.2.3");
    assertThatIllegalArgumentException().isThrownBy(() -> BuilderBuildpack.resolve(this.resolverContext, reference))
            .withMessageContaining("'urn:cnb:builder:example/buildpack1@1.2.3'")
            .withMessageContaining("not found in builder");
  }

  @Test
  void resolveWhenFullyQualifiedBuildpackWithoutVersionNotInBuilderThrowsException() {
    BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:example/buildpack1");
    assertThatIllegalArgumentException().isThrownBy(() -> BuilderBuildpack.resolve(this.resolverContext, reference))
            .withMessageContaining("'urn:cnb:builder:example/buildpack1'")
            .withMessageContaining("not found in builder");
  }

  @Test
  void resolveWhenUnqualifiedBuildpackNotInBuilderReturnsNull() {
    BuildpackReference reference = BuildpackReference.of("example/buildpack1@1.2.3");
    Buildpack buildpack = BuilderBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNull();
  }

  private void assertThatNoLayersAreAdded(Buildpack buildpack) throws IOException {
    List<Layer> layers = new ArrayList<>();
    buildpack.apply(layers::add);
    assertThat(layers).isEmpty();
  }

}
