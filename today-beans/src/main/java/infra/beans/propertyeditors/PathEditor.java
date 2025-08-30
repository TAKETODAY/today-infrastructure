/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import infra.core.io.Resource;
import infra.core.io.ResourceEditor;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.util.ResourceUtils;

/**
 * Editor for {@code java.nio.file.Path}, to directly populate a Path
 * property instead of using a String property as bridge.
 *
 * <p>Based on {@link Paths#get(URI)}'s resolution algorithm, checking
 * registered NIO file system providers, including the default file system
 * for "file:..." paths. Also supports Framework-style URL notation: any fully
 * qualified standard URL and Framework's special "classpath:" pseudo-URL, as
 * well as Framework's context-specific relative file paths. As a fallback, a
 * path will be resolved in the file system via {@code Paths#get(String)}
 * if no existing context-relative resource could be found.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Path
 * @see Paths#get(URI)
 * @see ResourceEditor
 * @see ResourceLoader
 * @see FileEditor
 * @see URLEditor
 * @since 4.0
 */
public class PathEditor extends PropertyEditorSupport {

  private final ResourceEditor resourceEditor;

  /**
   * Create a new PathEditor, using the default ResourceEditor underneath.
   */
  public PathEditor() {
    this.resourceEditor = new ResourceEditor();
  }

  /**
   * Create a new PathEditor, using the given ResourceEditor underneath.
   *
   * @param resourceEditor the ResourceEditor to use
   */
  public PathEditor(ResourceEditor resourceEditor) {
    Assert.notNull(resourceEditor, "ResourceEditor is required");
    this.resourceEditor = resourceEditor;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    boolean nioPathCandidate = !text.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
    if (nioPathCandidate && !text.startsWith("/")) {
      try {
        URI uri = ResourceUtils.toURI(text);
        String scheme = uri.getScheme();
        if (scheme != null) {
          // No NIO candidate except for "C:" style drive letters
          nioPathCandidate = (scheme.length() == 1);
          // Let's try NIO file system providers via Paths.get(URI)
          setValue(Paths.get(uri).normalize());
          return;
        }
      }
      catch (URISyntaxException ex) {
        // Not a valid URI; potentially a Windows-style path after
        // a file prefix (let's try as Infra resource location)
        nioPathCandidate = !text.startsWith(ResourceUtils.FILE_URL_PREFIX);
      }
      catch (FileSystemNotFoundException | IllegalArgumentException ex) {
        // URI scheme not registered for NIO or not meeting Paths requirements:
        // let's try URL protocol handlers via Infra resource mechanism.
      }
    }

    this.resourceEditor.setAsText(text);
    Resource resource = (Resource) this.resourceEditor.getValue();
    if (resource == null) {
      setValue(null);
    }
    else if (nioPathCandidate && (!resource.isFile() || !resource.exists())) {
      setValue(Paths.get(text).normalize());
    }
    else {
      try {
        setValue(resource.getFile().toPath());
      }
      catch (IOException ex) {
        String msg = "Could not resolve \"%s\" to 'java.nio.file.Path' for %s: %s".formatted(text, resource, ex.getMessage());
        if (nioPathCandidate) {
          msg += " - In case of ambiguity, consider adding the 'file:' prefix for an explicit reference to a file system resource of the same name: \"file:%s\""
                  .formatted(text);
        }
        throw new IllegalArgumentException(msg);
      }
    }
  }

  @Override
  public String getAsText() {
    Path value = (Path) getValue();
    return (value != null ? value.toString() : "");
  }

}
