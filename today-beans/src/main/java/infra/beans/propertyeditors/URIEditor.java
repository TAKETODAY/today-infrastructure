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

import infra.core.io.ClassPathResource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * Editor for {@code java.net.URI}, to directly populate a URI property
 * instead of using a String property as bridge.
 *
 * <p>Supports Framework-style URI notation: any fully qualified standard URI
 * ("file:", "http:", etc) and Framework's special "classpath:" pseudo-URL,
 * which will be resolved to a corresponding URI.
 *
 * <p>By default, this editor will encode Strings into URIs. For instance,
 * a space will be encoded into {@code %20}. This behavior can be changed
 * by calling the {@link #URIEditor(boolean)} constructor.
 *
 * <p>Note: A URI is more relaxed than a URL in that it does not require
 * a valid protocol to be specified. Any scheme within a valid URI syntax
 * is allowed, even without a matching protocol handler being registered.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see URI
 * @see URLEditor
 * @since 4.0
 */
public class URIEditor extends PropertyEditorSupport {

  @Nullable
  private final ClassLoader classLoader;

  private final boolean encode;

  /**
   * Create a new, encoding URIEditor, converting "classpath:" locations into
   * standard URIs (not trying to resolve them into physical resources).
   */
  public URIEditor() {
    this(true);
  }

  /**
   * Create a new URIEditor, converting "classpath:" locations into
   * standard URIs (not trying to resolve them into physical resources).
   *
   * @param encode indicates whether Strings will be encoded or not
   */
  public URIEditor(boolean encode) {
    this.classLoader = null;
    this.encode = encode;
  }

  /**
   * Create a new URIEditor, using the given ClassLoader to resolve
   * "classpath:" locations into physical resource URLs.
   *
   * @param classLoader the ClassLoader to use for resolving "classpath:" locations
   * (may be {@code null} to indicate the default ClassLoader)
   */
  public URIEditor(@Nullable ClassLoader classLoader) {
    this(classLoader, true);
  }

  /**
   * Create a new URIEditor, using the given ClassLoader to resolve
   * "classpath:" locations into physical resource URLs.
   *
   * @param classLoader the ClassLoader to use for resolving "classpath:" locations
   * (may be {@code null} to indicate the default ClassLoader)
   * @param encode indicates whether Strings will be encoded or not
   */
  public URIEditor(@Nullable ClassLoader classLoader, boolean encode) {
    this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    this.encode = encode;
  }

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      String uri = text.trim();
      if (this.classLoader != null && uri.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
        ClassPathResource resource = new ClassPathResource(
                uri.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length()), this.classLoader);
        try {
          setValue(resource.getURI());
        }
        catch (IOException ex) {
          throw new IllegalArgumentException("Could not retrieve URI for " + resource + ": " + ex.getMessage());
        }
      }
      else {
        try {
          setValue(createURI(uri));
        }
        catch (URISyntaxException ex) {
          throw new IllegalArgumentException("Invalid URI syntax: " + ex.getMessage());
        }
      }
    }
    else {
      setValue(null);
    }
  }

  /**
   * Create a URI instance for the given user-specified String value.
   * <p>The default implementation encodes the value into an RFC-2396 compliant URI.
   *
   * @param value the value to convert into a URI instance
   * @return the URI instance
   * @throws java.net.URISyntaxException if URI conversion failed
   */
  protected URI createURI(String value) throws URISyntaxException {
    int colonIndex = value.indexOf(':');
    if (this.encode && colonIndex != -1) {
      int fragmentIndex = value.indexOf('#', colonIndex + 1);
      String scheme = value.substring(0, colonIndex);
      String ssp = value.substring(colonIndex + 1, (fragmentIndex > 0 ? fragmentIndex : value.length()));
      String fragment = (fragmentIndex > 0 ? value.substring(fragmentIndex + 1) : null);
      return new URI(scheme, ssp, fragment);
    }
    else {
      // not encoding or the value contains no scheme - fallback to default
      return new URI(value);
    }
  }

  @Override
  public String getAsText() {
    URI value = (URI) getValue();
    return (value != null ? value.toString() : "");
  }

}
