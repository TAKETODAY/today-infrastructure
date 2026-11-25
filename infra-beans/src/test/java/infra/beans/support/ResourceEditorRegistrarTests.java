/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.beans.support;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import infra.beans.PropertyEditorRegistry;
import infra.beans.SimpleTypeConverter;
import infra.beans.propertyeditors.ClassArrayEditor;
import infra.beans.propertyeditors.ClassEditor;
import infra.beans.propertyeditors.FileEditor;
import infra.beans.propertyeditors.InputSourceEditor;
import infra.beans.propertyeditors.InputStreamEditor;
import infra.beans.propertyeditors.PathEditor;
import infra.beans.propertyeditors.ReaderEditor;
import infra.beans.propertyeditors.URIEditor;
import infra.beans.propertyeditors.URLEditor;
import infra.core.env.MapPropertySource;
import infra.core.env.StandardEnvironment;
import infra.core.io.ContextResource;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.PathMatchingPatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceArrayPropertyEditor;
import infra.core.io.ResourceEditor;
import infra.core.io.WritableResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class ResourceEditorRegistrarTests {

  private static final Map<Class<?>, Class<? extends PropertyEditor>> EDITOR_TYPES = Map.ofEntries(
          Map.entry(Resource.class, ResourceEditor.class),
          Map.entry(ContextResource.class, ResourceEditor.class),
          Map.entry(WritableResource.class, ResourceEditor.class),
          Map.entry(InputStream.class, InputStreamEditor.class),
          Map.entry(InputSource.class, InputSourceEditor.class),
          Map.entry(File.class, FileEditor.class),
          Map.entry(Path.class, PathEditor.class),
          Map.entry(Reader.class, ReaderEditor.class),
          Map.entry(URL.class, URLEditor.class),
          Map.entry(URI.class, URIEditor.class),
          Map.entry(Class.class, ClassEditor.class),
          Map.entry(Class[].class, ClassArrayEditor.class));

  @Test
  void overridesDefaultEditorsAndResolvesResourcePlaceholders() {
    var environment = new StandardEnvironment();
    String location = "classpath:infra/beans/support/ResourceEditorRegistrarTests.class";
    environment.getPropertySources().addFirst(new MapPropertySource("test", Map.of("test.location", location)));
    var loader = new PathMatchingPatternResourceLoader();
    var registrar = new ResourceEditorRegistrar(loader, environment);
    var registry = new SimpleTypeConverter();
    var original = registry.getDefaultEditor(File.class);

    registrar.registerCustomEditors(registry);

    assertThat(registrar.overridesDefaultEditors()).isTrue();
    EDITOR_TYPES.forEach((type, editorType) -> {
      assertThat(registry.getDefaultEditor(type)).isInstanceOf(editorType);
      assertThat(registry.findCustomEditor(type, null)).isNull();
    });
    assertThat(registry.getDefaultEditor(File.class)).isNotSameAs(original);
    assertThat(registry.getDefaultEditor(Resource[].class)).isInstanceOf(ResourceArrayPropertyEditor.class);
    PropertyEditor resourceEditor = registry.getDefaultEditor(Resource.class);
    assertThat(registry.getDefaultEditor(ContextResource.class)).isSameAs(resourceEditor);
    assertThat(registry.getDefaultEditor(WritableResource.class)).isSameAs(resourceEditor);
    resourceEditor.setAsText("${test.location}");
    assertThat(resourceEditor.getValue()).isEqualTo(loader.getResource(location));

    var secondRegistry = new SimpleTypeConverter();
    registrar.registerCustomEditors(secondRegistry);
    assertThat(secondRegistry.getDefaultEditor(Resource.class)).isNotSameAs(resourceEditor);
  }

  @Test
  void registersCustomEditorsWithoutPatternSupportOnPlainRegistry() {
    var registrar = new ResourceEditorRegistrar(new DefaultResourceLoader(), new StandardEnvironment());
    var registry = mock(PropertyEditorRegistry.class);

    registrar.registerCustomEditors(registry);

    EDITOR_TYPES.forEach((type, editorType) -> verify(registry).registerCustomEditor(eq(type), isA(editorType)));
    verifyNoMoreInteractions(registry);
  }

}
