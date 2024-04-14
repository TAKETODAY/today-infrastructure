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

package cn.taketoday.beans.support;

import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import cn.taketoday.beans.PropertyEditorRegistrar;
import cn.taketoday.beans.PropertyEditorRegistry;
import cn.taketoday.beans.PropertyEditorRegistrySupport;
import cn.taketoday.beans.propertyeditors.ClassArrayEditor;
import cn.taketoday.beans.propertyeditors.ClassEditor;
import cn.taketoday.beans.propertyeditors.FileEditor;
import cn.taketoday.beans.propertyeditors.InputSourceEditor;
import cn.taketoday.beans.propertyeditors.InputStreamEditor;
import cn.taketoday.beans.propertyeditors.PathEditor;
import cn.taketoday.beans.propertyeditors.ReaderEditor;
import cn.taketoday.beans.propertyeditors.URIEditor;
import cn.taketoday.beans.propertyeditors.URLEditor;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.io.ContextResource;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceArrayPropertyEditor;
import cn.taketoday.core.io.ResourceEditor;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.io.WritableResource;

/**
 * PropertyEditorRegistrar implementation that populates a given
 * {@link cn.taketoday.beans.PropertyEditorRegistry}
 * (typically a {@link cn.taketoday.beans.BeanWrapper} used for bean
 * creation within an {@link cn.taketoday.context.ApplicationContext})
 * with resource editors. Used by
 * {@link cn.taketoday.context.support.AbstractApplicationContext}.
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
   * @see cn.taketoday.core.env.Environment
   * @see cn.taketoday.core.io.PatternResourceLoader
   * @see cn.taketoday.context.ApplicationContext
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
   * @see cn.taketoday.core.io.ResourceEditor
   * @see cn.taketoday.beans.propertyeditors.InputStreamEditor
   * @see cn.taketoday.beans.propertyeditors.InputSourceEditor
   * @see cn.taketoday.beans.propertyeditors.FileEditor
   * @see cn.taketoday.beans.propertyeditors.URLEditor
   * @see cn.taketoday.beans.propertyeditors.URIEditor
   * @see cn.taketoday.beans.propertyeditors.ClassEditor
   * @see cn.taketoday.beans.propertyeditors.ClassArrayEditor
   * @see cn.taketoday.core.io.ResourceArrayPropertyEditor
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

}
