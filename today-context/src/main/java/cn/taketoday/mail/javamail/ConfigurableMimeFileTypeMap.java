/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.mail.javamail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import jakarta.activation.FileTypeMap;
import jakarta.activation.MimetypesFileTypeMap;

/**
 * Framework-configurable {@code FileTypeMap} implementation that will read
 * MIME type to file extension mappings from a standard JavaMail MIME type
 * mapping file, using a standard {@code MimetypesFileTypeMap} underneath.
 *
 * <p>The mapping file should be in the following format, as specified by the
 * Java Activation Framework:
 *
 * <pre class="code">
 * # map text/html to .htm and .html files
 * text/html  html htm HTML HTM</pre>
 *
 * Lines starting with {@code #} are treated as comments and are ignored. All
 * other lines are treated as mappings. Each mapping line should contain the MIME
 * type as the first entry and then each file extension to map to that MIME type
 * as subsequent entries. Each entry is separated by spaces or tabs.
 *
 * <p>By default, the mappings in the {@code mime.types} file located in the
 * same package as this class are used, which cover many common file extensions
 * (in contrast to the out-of-the-box mappings in {@code activation.jar}).
 * This can be overridden using the {@code mappingLocation} property.
 *
 * <p>Additional mappings can be added via the {@code mappings} bean property,
 * as lines that follow the {@code mime.types} file format.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setMappingLocation
 * @see #setMappings
 * @see MimetypesFileTypeMap
 * @since 4.0
 */
public class ConfigurableMimeFileTypeMap extends FileTypeMap implements InitializingBean {

  /**
   * The {@code Resource} to load the mapping file from.
   */
  private Resource mappingLocation = new ClassPathResource("mime.types", getClass());

  /**
   * Used to configure additional mappings.
   */
  @Nullable
  private String[] mappings;

  /**
   * The delegate FileTypeMap, compiled from the mappings in the mapping file
   * and the entries in the {@code mappings} property.
   */
  @Nullable
  private FileTypeMap fileTypeMap;

  /**
   * Specify the {@code Resource} from which mappings are loaded.
   * <p>Needs to follow the {@code mime.types} file format, as specified
   * by the Java Activation Framework, containing lines such as:<br>
   * {@code text/html  html htm HTML HTM}
   */
  public void setMappingLocation(Resource mappingLocation) {
    this.mappingLocation = mappingLocation;
  }

  /**
   * Specify additional MIME type mappings as lines that follow the
   * {@code mime.types} file format, as specified by the
   * Java Activation Framework. For example:<br>
   * {@code text/html  html htm HTML HTM}
   */
  public void setMappings(String... mappings) {
    this.mappings = mappings;
  }

  /**
   * Creates the final merged mapping set.
   */
  @Override
  public void afterPropertiesSet() {
    getFileTypeMap();
  }

  /**
   * Return the delegate FileTypeMap, compiled from the mappings in the mapping file
   * and the entries in the {@code mappings} property.
   *
   * @see #setMappingLocation
   * @see #setMappings
   * @see #createFileTypeMap
   */
  protected final FileTypeMap getFileTypeMap() {
    if (this.fileTypeMap == null) {
      try {
        this.fileTypeMap = createFileTypeMap(this.mappingLocation, this.mappings);
      }
      catch (IOException ex) {
        throw new IllegalStateException(
                "Could not load specified MIME type mapping file: " + this.mappingLocation, ex);
      }
    }
    return this.fileTypeMap;
  }

  /**
   * Compile a {@link FileTypeMap} from the mappings in the given mapping file
   * and the given mapping entries.
   * <p>The default implementation creates an Activation Framework {@link MimetypesFileTypeMap},
   * passing in an InputStream from the mapping resource (if any) and registering
   * the mapping lines programmatically.
   *
   * @param mappingLocation a {@code mime.types} mapping resource (can be {@code null})
   * @param mappings an array of MIME type mapping lines (can be {@code null})
   * @return the compiled FileTypeMap
   * @throws IOException if resource access failed
   * @see MimetypesFileTypeMap#MimetypesFileTypeMap(InputStream)
   * @see MimetypesFileTypeMap#addMimeTypes(String)
   */
  protected FileTypeMap createFileTypeMap(@Nullable Resource mappingLocation, @Nullable String[] mappings) throws IOException {
    MimetypesFileTypeMap fileTypeMap = null;
    if (mappingLocation != null) {
      try (InputStream is = mappingLocation.getInputStream()) {
        fileTypeMap = new MimetypesFileTypeMap(is);
      }
    }
    else {
      fileTypeMap = new MimetypesFileTypeMap();
    }
    if (mappings != null) {
      for (String mapping : mappings) {
        fileTypeMap.addMimeTypes(mapping);
      }
    }
    return fileTypeMap;
  }

  /**
   * Delegates to the underlying FileTypeMap.
   *
   * @see #getFileTypeMap()
   */
  @Override
  public String getContentType(File file) {
    return getFileTypeMap().getContentType(file);
  }

  /**
   * Delegates to the underlying FileTypeMap.
   *
   * @see #getFileTypeMap()
   */
  @Override
  public String getContentType(String fileName) {
    return getFileTypeMap().getContentType(fileName);
  }

}
