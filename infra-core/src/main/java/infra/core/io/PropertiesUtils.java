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

package infra.core.io;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.DefaultPropertiesPersister;
import infra.util.PropertiesPersister;
import infra.util.ResourceUtils;

/**
 * Convenient utility methods for loading of {@code java.util.Properties},
 * performing standard handling of input streams.
 *
 * <p>For more configurable properties loading, including the option of a
 * customized encoding, consider using the PropertiesLoaderSupport class.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sebastien Deleuze
 * @author TODAY 2021/10/6 00:00
 * @since 4.0
 */
public abstract class PropertiesUtils {

  private static final String XML_FILE_EXTENSION = ".xml";

  /**
   * Load properties from the given EncodedResource,
   * potentially defining a specific encoding for the properties file.
   *
   * @see #fillProperties(java.util.Properties, EncodedResource, PropertiesPersister)
   */
  public static Properties loadProperties(EncodedResource resource) throws IOException {
    Properties props = new Properties();
    fillProperties(props, resource, DefaultPropertiesPersister.INSTANCE);
    return props;
  }

  /**
   * Actually load properties from the given EncodedResource into the given Properties instance.
   *
   * @param props the Properties instance to load into
   * @param resource the resource to load from
   * @param persister the PropertiesPersister to use
   * @throws IOException in case of I/O errors
   */
  static void fillProperties(Properties props, EncodedResource resource, PropertiesPersister persister) throws IOException {
    InputStream stream = null;
    Reader reader = null;
    try {
      String filename = resource.getResource().getName();
      if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
        stream = resource.getInputStream();
        persister.loadFromXml(props, stream);
      }
      else if (resource.requiresReader()) {
        reader = resource.getReader();
        persister.load(props, reader);
      }
      else {
        stream = resource.getInputStream();
        persister.load(props, stream);
      }
    }
    finally {
      if (stream != null) {
        stream.close();
      }
      if (reader != null) {
        reader.close();
      }
    }
  }

  /**
   * Load properties from the given resource location
   *
   * @param resource the resource to load from
   * @return the populated Properties instance
   * @throws IOException if loading failed
   * @see #fillProperties(java.util.Properties, Resource)
   */
  public static Properties loadProperties(String resource) throws IOException {
    return loadProperties(ResourceUtils.getResource(resource));
  }

  /**
   * Load properties from the given resource (in ISO-8859-1 encoding).
   *
   * @param resource the resource to load from
   * @return the populated Properties instance
   * @throws IOException if loading failed
   * @see #fillProperties(java.util.Properties, Resource)
   */
  public static Properties loadProperties(Resource resource) throws IOException {
    Properties props = new Properties();
    fillProperties(props, resource);
    return props;
  }

  /**
   * Fill the given properties from the given resource (in ISO-8859-1 encoding).
   *
   * @param props the Properties instance to fill
   * @param resource the resource to load from
   * @throws IOException if loading failed
   */
  public static void fillProperties(Properties props, Resource resource) throws IOException {
    Assert.notNull(props, "Properties is required");
    Assert.notNull(resource, "Resource is required");
    try (InputStream is = resource.getInputStream()) {
      String filename = resource.getName();
      if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
        props.loadFromXML(is);
      }
      else {
        props.load(is);
      }
    }
  }

  /**
   * Load all properties from the specified class path resource
   * (in ISO-8859-1 encoding), using the default class loader.
   * <p>Merges properties if more than one resource of the same name
   * found in the class path.
   *
   * @param resourceName the name of the class path resource
   * @return the populated Properties instance
   * @throws IOException if loading failed
   */
  public static Properties loadAllProperties(String resourceName) throws IOException {
    return loadAllProperties(resourceName, null);
  }

  /**
   * Load all properties from the specified class path resource
   * (in ISO-8859-1 encoding), using the given class loader.
   * <p>Merges properties if more than one resource of the same name
   * found in the class path.
   *
   * @param resourceName the name of the class path resource
   * @param classLoader the ClassLoader to use for loading
   * (or {@code null} to use the default class loader)
   * @return the populated Properties instance
   * @throws IOException if loading failed
   */
  public static Properties loadAllProperties(String resourceName, @Nullable ClassLoader classLoader) throws IOException {
    Properties props = new Properties();
    loadAllProperties(props, resourceName, StandardCharsets.ISO_8859_1, classLoader);
    return props;
  }

  /**
   * Load all properties from the specified class path resource
   * (in ISO-8859-1 encoding), using the given class loader.
   * <p>Merges properties if more than one resource of the same name
   * found in the class path.
   *
   * @param resourceName the name of the class path resource
   * @throws IOException if loading failed
   * @since 5.0
   */
  public static void loadAllProperties(Properties props, String resourceName) throws IOException {
    loadAllProperties(props, resourceName, StandardCharsets.ISO_8859_1);
  }

  /**
   * Load all properties from the specified class path resource
   * (in ISO-8859-1 encoding), using the given class loader.
   * <p>Merges properties if more than one resource of the same name
   * found in the class path.
   *
   * @param resourceName the name of the class path resource
   * @throws IOException if loading failed
   * @since 5.0
   */
  public static void loadAllProperties(Properties props, String resourceName, Charset charset) throws IOException {
    loadAllProperties(props, resourceName, charset, null);
  }

  /**
   * Load all properties from the specified class path resource
   * (in ISO-8859-1 encoding), using the given class loader.
   * <p>Merges properties if more than one resource of the same name
   * found in the class path.
   *
   * @param resourceName the name of the class path resource
   * @param classLoader the ClassLoader to use for loading
   * (or {@code null} to use the default class loader)
   * @throws IOException if loading failed
   * @since 5.0
   */
  public static void loadAllProperties(Properties props, String resourceName, Charset charset, @Nullable ClassLoader classLoader) throws IOException {
    Assert.notNull(props, "Properties is required");
    Assert.notNull(charset, "Charset is required");
    Assert.notNull(resourceName, "Resource name is required");
    ClassLoader classLoaderToUse = classLoader;
    if (classLoaderToUse == null) {
      classLoaderToUse = ClassUtils.getDefaultClassLoader();
    }
    Enumeration<URL> urls = classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) :
            ClassLoader.getSystemResources(resourceName);
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      URLConnection con = url.openConnection();
      ResourceUtils.useCachesIfNecessary(con);
      try (InputStream is = con.getInputStream()) {
        if (resourceName.endsWith(XML_FILE_EXTENSION)) {
          props.loadFromXML(is);
        }
        else {
          props.load(new InputStreamReader(con.getInputStream(), charset));
        }
      }
    }
  }

  /**
   * Convert {@link String} into {@link Properties}, considering it as
   * properties content.
   *
   * @param text the text to be so converted
   */
  public static Properties parse(@Nullable String text) {
    Properties props = new Properties();
    if (text != null) {
      try {
        // Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
        props.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
      }
      catch (IOException ex) {
        // Should never happen.
        throw new IllegalArgumentException(
                "Failed to parse [" + text + "] into Properties", ex);
      }
    }
    return props;
  }

}
