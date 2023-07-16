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

package cn.taketoday.infra.maven;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import cn.taketoday.app.loader.tools.Library;
import cn.taketoday.app.loader.tools.LibraryCoordinates;
import cn.taketoday.app.loader.tools.layer.CustomLayers;
import cn.taketoday.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CustomLayersProvider}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class CustomLayersProviderTests {

  private CustomLayersProvider customLayersProvider;

  @BeforeEach
  void setup() {
    this.customLayersProvider = new CustomLayersProvider();
  }

  @Test
  void getLayerResolverWhenDocumentValid() throws Exception {
    CustomLayers layers = this.customLayersProvider.getLayers(getDocument("layers.xml"));
    assertThat(layers).extracting("name")
            .containsExactly("my-deps", "my-dependencies-name", "snapshot-dependencies", "my-resources",
                    "configuration", "application");
    Library snapshot = mockLibrary("test-SNAPSHOT.jar", "org.foo", "1.0.0-SNAPSHOT");
    Library groupId = mockLibrary("my-library", "com.acme", null);
    Library otherDependency = mockLibrary("other-library", "org.foo", null);
    Library localSnapshotDependency = mockLibrary("local-library", "org.foo", "1.0-SNAPSHOT");
    given(localSnapshotDependency.isLocal()).willReturn(true);
    assertThat(layers.getLayer(snapshot)).hasToString("snapshot-dependencies");
    assertThat(layers.getLayer(groupId)).hasToString("my-deps");
    assertThat(layers.getLayer(otherDependency)).hasToString("my-dependencies-name");
    assertThat(layers.getLayer(localSnapshotDependency)).hasToString("application");
    assertThat(layers.getLayer("META-INF/resources/test.css")).hasToString("my-resources");
    assertThat(layers.getLayer("application.yml")).hasToString("configuration");
    assertThat(layers.getLayer("test")).hasToString("application");
  }

  private Library mockLibrary(String name, String groupId, String version) {
    Library library = mock(Library.class);
    given(library.getName()).willReturn(name);
    given(library.getCoordinates()).willReturn(LibraryCoordinates.of(groupId, null, version));
    return library;
  }

  @Test
  void getLayerResolverWhenDocumentContainsLibraryLayerWithNoFilters() throws Exception {
    CustomLayers layers = this.customLayersProvider.getLayers(getDocument("dependencies-layer-no-filter.xml"));
    Library library = mockLibrary("my-library", "com.acme", null);
    assertThat(layers.getLayer(library)).hasToString("my-deps");
    assertThatIllegalStateException().isThrownBy(() -> layers.getLayer("application.yml"))
            .withMessageContaining("match any layer");
  }

  @Test
  void getLayerResolverWhenDocumentContainsResourceLayerWithNoFilters() throws Exception {
    CustomLayers layers = this.customLayersProvider.getLayers(getDocument("application-layer-no-filter.xml"));
    Library library = mockLibrary("my-library", "com.acme", null);
    assertThat(layers.getLayer("application.yml")).hasToString("my-layer");
    assertThatIllegalStateException().isThrownBy(() -> layers.getLayer(library))
            .withMessageContaining("match any layer");
  }

  private Document getDocument(String resourceName) throws Exception {
    ClassPathResource resource = new ClassPathResource(resourceName);
    InputSource inputSource = new InputSource(resource.getInputStream());
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder documentBuilder = factory.newDocumentBuilder();
    return documentBuilder.parse(inputSource);
  }

}
