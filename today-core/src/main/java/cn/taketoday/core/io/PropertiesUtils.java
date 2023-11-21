/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.DefaultPropertiesPersister;
import cn.taketoday.util.PropertiesPersister;
import cn.taketoday.util.ResourceUtils;

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
   * Check properties file name
   *
   * @param fileName Input file name
   * @return checked properties file name
   */
  public static String checkPropertiesName(String fileName) {
    return fileName.endsWith(Constant.PROPERTIES_SUFFIX) ? fileName : fileName + Constant.PROPERTIES_SUFFIX;
  }

  /**
   * Actually load properties from the given EncodedResource into the given Properties instance.
   *
   * @param props the Properties instance to load into
   * @param resource the resource to load from
   * @param persister the PropertiesPersister to use
   * @throws IOException in case of I/O errors
   */
  static void fillProperties(
          Properties props, EncodedResource resource, PropertiesPersister persister) throws IOException {

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
    String location = checkPropertiesName(resource);
    return loadProperties(ResourceUtils.getResource(location));
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

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void fillProperties(Map properties, Resource resource) throws IOException {
    if (properties != null) {
      properties.putAll(loadProperties(resource));
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
    Assert.notNull(resourceName, "Resource name is required");
    ClassLoader classLoaderToUse = classLoader;
    if (classLoaderToUse == null) {
      classLoaderToUse = ClassUtils.getDefaultClassLoader();
    }
    Enumeration<URL> urls = (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) :
                             ClassLoader.getSystemResources(resourceName));
    Properties props = new Properties();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      URLConnection con = url.openConnection();
      ResourceUtils.useCachesIfNecessary(con);
      try (InputStream is = con.getInputStream()) {
        if (resourceName.endsWith(XML_FILE_EXTENSION)) {
          props.loadFromXML(is);
        }
        else {
          props.load(is);
        }
      }
    }
    return props;
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
