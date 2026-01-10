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

import java.beans.PropertyEditorSupport;
import java.net.URL;

import infra.core.io.Resource;
import infra.core.io.ResourceEditor;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;

/**
 * Editor for {@code java.net.URL}, to directly populate a URL property
 * instead of using a String property as bridge.
 *
 * <p>Supports Framework-style URL notation: any fully qualified standard URL
 * ("file:", "http:", etc) and Framework's special "classpath:" pseudo-URL,
 * as well as Framework's context-specific relative file paths.
 *
 * <p>Note: A URL must specify a valid protocol, else it will be rejected
 * upfront. However, the target resource does not necessarily have to exist
 * at the time of URL creation; this depends on the specific resource type.
 *
 * @author Juergen Hoeller
 * @see URL
 * @see ResourceEditor
 * @see ResourceLoader
 * @see FileEditor
 * @see InputStreamEditor
 * @since 4.0
 */
public class URLEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new URLEditor, using a default ResourceEditor underneath.
   */
  public URLEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new URLEditor, using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public URLEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor is required");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    resourceEditor.setAsText(text);
    if (resourceEditor.getValue() instanceof Resource resource) {
      try {
        setValue(resource.getURL());
      }
      catch (Exception ex) {
        throw new IllegalArgumentException("Could not retrieve URL for " + resource + ": " + ex.getMessage());
      }
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    if (getValue() instanceof URL value) {
      return value.toExternalForm();
    }
    return "";
  }

}
