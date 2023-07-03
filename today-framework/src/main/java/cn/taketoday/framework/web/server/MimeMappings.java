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

package cn.taketoday.framework.web.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Simple server-independent abstraction for mime mappings. Roughly equivalent to the
 * {@literal &lt;mime-mapping&gt;} element traditionally found in web.xml.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public sealed class MimeMappings implements Iterable<MimeMappings.Mapping> {

  /**
   * Default mime mapping commonly used.
   */
  public static final MimeMappings DEFAULT = new DefaultMimeMappings();

  private final Map<String, Mapping> map;

  /**
   * Create a new empty {@link MimeMappings} instance.
   */
  public MimeMappings() {
    this.map = new LinkedHashMap<>();
  }

  /**
   * Create a new {@link MimeMappings} instance from the specified mappings.
   *
   * @param mappings the source mappings
   */
  public MimeMappings(MimeMappings mappings) {
    this(mappings, true);
  }

  /**
   * Create a new {@link MimeMappings} from the specified mappings.
   *
   * @param mappings the source mappings with extension as the key and mime-type as the
   * value
   */
  public MimeMappings(Map<String, String> mappings) {
    Assert.notNull(mappings, "Mappings must not be null");
    this.map = new LinkedHashMap<>();
    for (Map.Entry<String, String> entry : mappings.entrySet()) {
      add(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Internal constructor.
   *
   * @param mappings source mappings
   * @param mutable if the new object should be mutable.
   */
  MimeMappings(MimeMappings mappings, boolean mutable) {
    Assert.notNull(mappings, "Mappings must not be null");
    this.map = (mutable ? new LinkedHashMap<>(mappings.map) : Collections.unmodifiableMap(mappings.map));
  }

  /**
   * Add a new mime mapping.
   *
   * @param extension the file extension (excluding '.')
   * @param mimeType the mime type to map
   * @return any previous mapping or {@code null}
   */
  @Nullable
  public String add(String extension, String mimeType) {
    Assert.notNull(extension, "Extension must not be null");
    Assert.notNull(mimeType, "MimeType must not be null");
    Mapping previous = map.put(extension.toLowerCase(Locale.ENGLISH), new Mapping(extension, mimeType));
    return previous != null ? previous.getMimeType() : null;
  }

  /**
   * Remove an existing mapping.
   *
   * @param extension the file extension (excluding '.')
   * @return the removed mime mapping or {@code null} if no item was removed
   */
  @Nullable
  public String remove(String extension) {
    Assert.notNull(extension, "Extension must not be null");
    Mapping previous = map.remove(extension.toLowerCase(Locale.ENGLISH));
    return previous != null ? previous.getMimeType() : null;
  }

  /**
   * Get a mime mapping for the given extension.
   *
   * @param extension the file extension (excluding '.')
   * @return a mime mapping or {@code null}
   */
  @Nullable
  public String get(String extension) {
    Assert.notNull(extension, "Extension must not be null");
    Mapping mapping = map.get(extension.toLowerCase(Locale.ENGLISH));
    return mapping != null ? mapping.getMimeType() : null;
  }

  /**
   * Returns all defined mappings.
   *
   * @return the mappings.
   */
  public Collection<Mapping> getAll() {
    return this.map.values();
  }

  @Override
  public final Iterator<Mapping> iterator() {
    return getAll().iterator();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj instanceof MimeMappings other) {
      return getMap().equals(other.map);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getMap().hashCode();
  }

  Map<String, Mapping> getMap() {
    return this.map;
  }

  /**
   * Create a new unmodifiable view of the specified mapping. Methods that attempt to
   * modify the returned map will throw {@link UnsupportedOperationException}s.
   *
   * @param mappings the mappings
   * @return an unmodifiable view of the specified mappings.
   */
  public static MimeMappings unmodifiableMappings(MimeMappings mappings) {
    Assert.notNull(mappings, "Mappings must not be null");
    return new MimeMappings(mappings, false);
  }

  /**
   * Create a new lazy copy of the given mappings that will only copy entries if the
   * mappings are mutated.
   *
   * @param mappings the source mappings
   * @return a new mappings instance
   */
  public static MimeMappings lazyCopy(MimeMappings mappings) {
    Assert.notNull(mappings, "Mappings must not be null");
    return new LazyMimeMappingsCopy(mappings);
  }

  /**
   * A single mime mapping.
   */
  public static final class Mapping {

    private final String extension;

    private final String mimeType;

    public Mapping(String extension, String mimeType) {
      Assert.notNull(extension, "Extension must not be null");
      Assert.notNull(mimeType, "MimeType must not be null");
      this.extension = extension;
      this.mimeType = mimeType;
    }

    public String getExtension() {
      return this.extension;
    }

    public String getMimeType() {
      return this.mimeType;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (obj instanceof Mapping other) {
        return this.extension.equals(other.extension) && this.mimeType.equals(other.mimeType);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.extension.hashCode();
    }

    @Override
    public String toString() {
      return "Mapping [extension=" + this.extension + ", mimeType=" + this.mimeType + "]";
    }

  }

  /**
   * {@link MimeMappings} implementation used for {@link MimeMappings#DEFAULT}. Provides
   * in-memory access for common mappings and lazily loads the complete set when
   * necessary.
   */
  static final class DefaultMimeMappings extends MimeMappings {

    static final String MIME_MAPPINGS_PROPERTIES = "mime-mappings.properties";

    private static final MimeMappings COMMON;

    static {
      MimeMappings mappings = new MimeMappings();
      mappings.add("avi", "video/x-msvideo");
      mappings.add("bin", "application/octet-stream");
      mappings.add("body", "text/html");
      mappings.add("class", "application/java");
      mappings.add("css", "text/css");
      mappings.add("dtd", "application/xml-dtd");
      mappings.add("gif", "image/gif");
      mappings.add("gtar", "application/x-gtar");
      mappings.add("gz", "application/x-gzip");
      mappings.add("htm", "text/html");
      mappings.add("html", "text/html");
      mappings.add("jar", "application/java-archive");
      mappings.add("java", "text/x-java-source");
      mappings.add("jnlp", "application/x-java-jnlp-file");
      mappings.add("jpe", "image/jpeg");
      mappings.add("jpeg", "image/jpeg");
      mappings.add("jpg", "image/jpeg");
      mappings.add("js", "text/javascript");
      mappings.add("json", "application/json");
      mappings.add("otf", "font/otf");
      mappings.add("pdf", "application/pdf");
      mappings.add("png", "image/png");
      mappings.add("ps", "application/postscript");
      mappings.add("tar", "application/x-tar");
      mappings.add("tif", "image/tiff");
      mappings.add("tiff", "image/tiff");
      mappings.add("ttf", "font/ttf");
      mappings.add("txt", "text/plain");
      mappings.add("xht", "application/xhtml+xml");
      mappings.add("xhtml", "application/xhtml+xml");
      mappings.add("xls", "application/vnd.ms-excel");
      mappings.add("xml", "application/xml");
      mappings.add("xsl", "application/xml");
      mappings.add("xslt", "application/xslt+xml");
      mappings.add("wasm", "application/wasm");
      mappings.add("zip", "application/zip");
      COMMON = unmodifiableMappings(mappings);
    }

    @Nullable
    private volatile Map<String, Mapping> loaded;

    DefaultMimeMappings() {
      super(new MimeMappings(), false);
    }

    @Override
    public Collection<Mapping> getAll() {
      return load().values();
    }

    @Override
    public String get(String extension) {
      Assert.notNull(extension, "Extension must not be null");
      extension = extension.toLowerCase(Locale.ENGLISH);
      Map<String, Mapping> loaded = this.loaded;
      if (loaded != null) {
        return get(loaded, extension);
      }
      String commonMimeType = COMMON.get(extension);
      if (commonMimeType != null) {
        return commonMimeType;
      }
      loaded = load();
      return get(loaded, extension);
    }

    @Nullable
    private String get(Map<String, Mapping> mappings, String extension) {
      Mapping mapping = mappings.get(extension);
      return (mapping != null) ? mapping.getMimeType() : null;
    }

    @Override
    Map<String, Mapping> getMap() {
      return load();
    }

    private Map<String, Mapping> load() {
      Map<String, Mapping> loaded = this.loaded;
      if (loaded != null) {
        return loaded;
      }
      try {
        loaded = new LinkedHashMap<>();
        for (var entry : PropertiesUtils.loadProperties(
                new ClassPathResource(MIME_MAPPINGS_PROPERTIES, getClass())).entrySet()) {
          loaded.put((String) entry.getKey(),
                  new Mapping((String) entry.getKey(), (String) entry.getValue()));
        }
        loaded = Collections.unmodifiableMap(loaded);
        this.loaded = loaded;
        return loaded;
      }
      catch (IOException ex) {
        throw new IllegalArgumentException("Unable to load the default MIME types", ex);
      }
    }

  }

  /**
   * {@link MimeMappings} implementation used to create a lazy copy only when the
   * mappings are mutated.
   */
  static final class LazyMimeMappingsCopy extends MimeMappings {

    private final MimeMappings source;

    private final AtomicBoolean copied = new AtomicBoolean();

    LazyMimeMappingsCopy(MimeMappings source) {
      this.source = source;
    }

    @Override
    public String add(String extension, String mimeType) {
      copyIfNecessary();
      return super.add(extension, mimeType);
    }

    @Override
    public String remove(String extension) {
      copyIfNecessary();
      return super.remove(extension);
    }

    private void copyIfNecessary() {
      if (this.copied.compareAndSet(false, true)) {
        this.source.forEach((mapping) -> add(mapping.getExtension(), mapping.getMimeType()));
      }
    }

    @Override
    public String get(String extension) {
      return !this.copied.get() ? this.source.get(extension) : super.get(extension);
    }

    @Override
    public Collection<Mapping> getAll() {
      return !this.copied.get() ? this.source.getAll() : super.getAll();
    }

    @Override
    Map<String, Mapping> getMap() {
      return !this.copied.get() ? this.source.getMap() : super.getMap();
    }

  }

  static class MimeMappingsRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      hints.resources()
              .registerPattern("cn/taketoday/framework/web/server/" + DefaultMimeMappings.MIME_MAPPINGS_PROPERTIES);
    }

  }

}
