/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.support;

import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import infra.beans.BeanWrapper;
import infra.beans.PropertyEditorRegistrar;
import infra.beans.PropertyEditorRegistry;
import infra.beans.PropertyEditorRegistrySupport;
import infra.beans.propertyeditors.ClassArrayEditor;
import infra.beans.propertyeditors.ClassEditor;
import infra.beans.propertyeditors.FileEditor;
import infra.beans.propertyeditors.InputSourceEditor;
import infra.beans.propertyeditors.InputStreamEditor;
import infra.beans.propertyeditors.PathEditor;
import infra.beans.propertyeditors.ReaderEditor;
import infra.beans.propertyeditors.URIEditor;
import infra.beans.propertyeditors.URLEditor;
import infra.core.env.Environment;
import infra.core.env.PropertyResolver;
import infra.core.io.ContextResource;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceArrayPropertyEditor;
import infra.core.io.ResourceEditor;
import infra.core.io.ResourceLoader;
import infra.core.io.WritableResource;

/**
 * PropertyEditorRegistrar implementation that populates a given
 * {@link PropertyEditorRegistry}
 * (typically a {@link BeanWrapper} used for bean
 * creation within an {@link infra.context.ApplicationContext})
 * with resource editors. Used by
 * {@link infra.context.support.AbstractApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 11:09
 */
public class ResourceEditorRegistrar implements PropertyEditorRegistrar {

  private final PropertyResolver propertyResolver;

  private final ResourceLoader resourceLoader;

  /**
   * Create a new ResourceEditorRegistrar for the given {@link ResourceLoader}
   * and {@link PropertyResolver}.
   *
   * @param resourceLoader the ResourceLoader (or PatternResourceLoader)
   * to create editors for (usually an ApplicationContext)
   * @param propertyResolver the PropertyResolver (usually an Environment)
   * @see Environment
   * @see PatternResourceLoader
   * @see infra.context.ApplicationContext
   */
  public ResourceEditorRegistrar(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
    this.resourceLoader = resourceLoader;
    this.propertyResolver = propertyResolver;
  }

  /**
   * Populate the given {@code registry} with the following resource editors:
   * ResourceEditor, InputStreamEditor, InputSourceEditor, FileEditor, URLEditor,
   * URIEditor, ClassEditor, ClassArrayEditor.
   * <p>If this registrar has been configured with a {@link PatternResourceLoader},
   * a ResourceArrayPropertyEditor will be registered as well.
   *
   * @see ResourceEditor
   * @see InputStreamEditor
   * @see InputSourceEditor
   * @see FileEditor
   * @see URLEditor
   * @see URIEditor
   * @see ClassEditor
   * @see ClassArrayEditor
   * @see ResourceArrayPropertyEditor
   */
  @Override
  public void registerCustomEditors(PropertyEditorRegistry registry) {
    ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
    doRegisterEditor(registry, Resource.class, baseEditor);
    doRegisterEditor(registry, ContextResource.class, baseEditor);
    doRegisterEditor(registry, WritableResource.class, baseEditor);
    doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
    doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
    doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
    doRegisterEditor(registry, Path.class, new PathEditor(baseEditor));
    doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
    doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

    ClassLoader classLoader = this.resourceLoader.getClassLoader();
    doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
    doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
    doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

    if (this.resourceLoader instanceof PatternResourceLoader patternResourceLoader) {
      doRegisterEditor(registry, Resource[].class,
              new ResourceArrayPropertyEditor(patternResourceLoader, this.propertyResolver));
    }
  }

  /**
   * Override default editor, if possible (since that's what we really mean to do here);
   * otherwise register as a custom editor.
   */
  private static void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
    if (registry instanceof PropertyEditorRegistrySupport registrySupport) {
      registrySupport.overrideDefaultEditor(requiredType, editor);
    }
    else {
      registry.registerCustomEditor(requiredType, editor);
    }
  }

  /**
   * Indicate the use of {@link PropertyEditorRegistrySupport#overrideDefaultEditor} above.
   */
  @Override
  public boolean overridesDefaultEditors() {
    return true;
  }

}
