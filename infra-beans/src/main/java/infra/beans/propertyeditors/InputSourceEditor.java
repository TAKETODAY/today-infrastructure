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

package infra.beans.propertyeditors;

import org.xml.sax.InputSource;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import infra.core.io.Resource;
import infra.core.io.ResourceEditor;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;

/**
 * Editor for {@code org.xml.sax.InputSource}, converting from a
 * Framework resource location String to a SAX InputSource object.
 *
 * <p>Supports Framework-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Framework's special "classpath:" pseudo-URL.
 *
 * @author Juergen Hoeller
 * @see InputSource
 * @see ResourceEditor
 * @see ResourceLoader
 * @see URLEditor
 * @see FileEditor
 * @since 4.0
 */
public class InputSourceEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new InputSourceEditor,
   * using the default ResourceEditor underneath.
   */
  public InputSourceEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new InputSourceEditor,
   * using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public InputSourceEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor is required");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    this.resourceEditor.setAsText(text);
    Resource resource = (Resource) this.resourceEditor.getValue();
    try {
      setValue(resource != null ? new InputSource(resource.getURL().toString()) : null);
    }
    catch (IOException ex) {
      throw new IllegalArgumentException(
              "Could not retrieve URL for " + resource + ": " + ex.getMessage());
    }
  }

  @Override
  public String getAsText() {
    InputSource value = (InputSource) getValue();
    return (value != null ? value.getSystemId() : "");
  }

}
