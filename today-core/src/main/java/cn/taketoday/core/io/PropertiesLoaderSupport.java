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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.DefaultPropertiesPersister;
import cn.taketoday.util.PropertiesPersister;

/**
 * Base class for JavaBean-style components that need to load properties
 * from one or more resources. Supports local properties as well, with
 * configurable overriding.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/12 14:34
 */
public abstract class PropertiesLoaderSupport {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  protected Properties[] localProperties;

  protected boolean localOverride = false;

  @Nullable
  private Resource[] locations;

  private boolean ignoreResourceNotFound = false;

  @Nullable
  private String fileEncoding;

  private PropertiesPersister propertiesPersister = DefaultPropertiesPersister.INSTANCE;

  /**
   * Set local properties, e.g. via the "props" tag in XML bean definitions.
   * These can be considered defaults, to be overridden by properties
   * loaded from files.
   */
  public void setProperties(Properties properties) {
    this.localProperties = new Properties[] { properties };
  }

  /**
   * Set local properties, e.g. via the "props" tag in XML bean definitions,
   * allowing for merging multiple properties sets into one.
   */
  public void setPropertiesArray(Properties... propertiesArray) {
    this.localProperties = propertiesArray;
  }

  /**
   * Set a location of a properties file to be loaded.
   * <p>Can point to a classic properties file or to an XML file
   * that follows JDK 1.5's properties XML format.
   */
  public void setLocation(Resource location) {
    this.locations = new Resource[] { location };
  }

  /**
   * Set locations of properties files to be loaded.
   * <p>Can point to classic properties files or to XML files
   * that follow JDK 1.5's properties XML format.
   * <p>Note: Properties defined in later files will override
   * properties defined earlier files, in case of overlapping keys.
   * Hence, make sure that the most specific files are the last
   * ones in the given list of locations.
   */
  public void setLocations(Resource... locations) {
    this.locations = locations;
  }

  /**
   * Set whether local properties override properties from files.
   * <p>Default is "false": Properties from files override local defaults.
   * Can be switched to "true" to let local properties override defaults
   * from files.
   */
  public void setLocalOverride(boolean localOverride) {
    this.localOverride = localOverride;
  }

  /**
   * Set if failure to find the property resource should be ignored.
   * <p>"true" is appropriate if the properties file is completely optional.
   * Default is "false".
   */
  public void setIgnoreResourceNotFound(boolean ignoreResourceNotFound) {
    this.ignoreResourceNotFound = ignoreResourceNotFound;
  }

  /**
   * Set the encoding to use for parsing properties files.
   * <p>Default is none, using the {@code java.util.Properties}
   * default encoding.
   * <p>Only applies to classic properties files, not to XML files.
   */
  public void setFileEncoding(String encoding) {
    this.fileEncoding = encoding;
  }

  /**
   * Set the PropertiesPersister to use for parsing properties files.
   * The default is ResourcePropertiesPersister.
   *
   * @see DefaultPropertiesPersister#INSTANCE
   */
  public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
    this.propertiesPersister =
            (propertiesPersister != null ? propertiesPersister : DefaultPropertiesPersister.INSTANCE);
  }

  /**
   * Return a merged Properties instance containing both the
   * loaded properties and properties set on this FactoryBean.
   */
  protected Properties mergeProperties() throws IOException {
    Properties result = new Properties();

    if (localOverride) {
      // Load properties from file upfront, to let local properties override.
      loadProperties(result);
    }

    if (localProperties != null) {
      for (Properties localProp : localProperties) {
        CollectionUtils.mergePropertiesIntoMap(localProp, result);
      }
    }

    if (!localOverride) {
      // Load properties from file afterwards, to let those properties override.
      loadProperties(result);
    }

    return result;
  }

  /**
   * Load properties into the given instance.
   *
   * @param props the Properties instance to load into
   * @throws IOException in case of I/O errors
   * @see #setLocations
   */
  protected void loadProperties(Properties props) throws IOException {
    if (locations != null) {
      for (Resource location : locations) {
        if (logger.isTraceEnabled()) {
          logger.trace("Loading properties file from {}", location);
        }
        try {
          PropertiesUtils.fillProperties(
                  props, new EncodedResource(location, fileEncoding), propertiesPersister);
        }
        catch (FileNotFoundException | UnknownHostException | SocketException ex) {
          if (ignoreResourceNotFound) {
            if (logger.isDebugEnabled()) {
              logger.debug("Properties resource not found: {}", ex.getMessage());
            }
          }
          else {
            throw ex;
          }
        }
      }
    }
  }

}
