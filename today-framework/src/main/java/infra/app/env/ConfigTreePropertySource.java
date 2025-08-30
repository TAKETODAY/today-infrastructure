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

package infra.app.env;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import infra.core.env.EnumerablePropertySource;
import infra.core.env.Environment;
import infra.core.env.PropertySource;
import infra.core.io.InputStreamSource;
import infra.core.io.PathResource;
import infra.core.io.Resource;
import infra.format.support.ApplicationConversionService;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.origin.OriginProvider;
import infra.origin.TextResourceOrigin;
import infra.origin.TextResourceOrigin.Location;
import infra.util.FileCopyUtils;
import infra.util.StringUtils;

/**
 * {@link PropertySource} backed by a directory tree that contains files for each value.
 * The {@link PropertySource} will recursively scan a given source directory and expose a
 * property for each file found. The property name will be the filename, and the property
 * value will be the contents of the file.
 * <p>
 * Directories are only scanned when the source is first created. The directory is not
 * monitored for updates, so files should not be added or removed. However, the contents
 * of a file can be updated as long as the property source was created with a
 * {@link Option#ALWAYS_READ} option. Nested directories are included in the source, but
 * with a {@code '.'} rather than {@code '/'} used as the path separator.
 * <p>
 * Property values are returned as {@link Value} instances which allows them to be treated
 * either as an {@link InputStreamSource} or as a {@link CharSequence}. In addition, if
 * used with an {@link Environment} configured with an
 * {@link ApplicationConversionService}, property values can be converted to a
 * {@code String} or {@code byte[]}.
 * <p>
 * This property source is typically used to read Kubernetes {@code configMap} volume
 * mounts.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class ConfigTreePropertySource extends EnumerablePropertySource<Path> implements OriginLookup<String> {

  private static final int MAX_DEPTH = 100;

  private final Map<String, PropertyFile> propertyFiles;

  private final String[] names;

  private final Set<Option> options;

  /**
   * Create a new {@link ConfigTreePropertySource} instance.
   *
   * @param name the name of the property source
   * @param sourceDirectory the underlying source directory
   */
  public ConfigTreePropertySource(String name, Path sourceDirectory) {
    this(name, sourceDirectory, EnumSet.noneOf(Option.class));
  }

  /**
   * Create a new {@link ConfigTreePropertySource} instance.
   *
   * @param name the name of the property source
   * @param sourceDirectory the underlying source directory
   * @param options the property source options
   */
  public ConfigTreePropertySource(String name, Path sourceDirectory, Option... options) {
    this(name, sourceDirectory, EnumSet.copyOf(Arrays.asList(options)));
  }

  private ConfigTreePropertySource(String name, Path sourceDirectory, Set<Option> options) {
    super(name, sourceDirectory);
    if (Files.notExists(sourceDirectory)) {
      throw new IllegalArgumentException("Directory '%s' does not exist".formatted(sourceDirectory));
    }

    if (!Files.isDirectory(sourceDirectory)) {
      throw new IllegalArgumentException("File '%s' is not a directory".formatted(sourceDirectory));
    }
    this.options = options;
    this.propertyFiles = PropertyFile.findAll(sourceDirectory, options);
    this.names = StringUtils.toStringArray(this.propertyFiles.keySet());
  }

  @Override
  public String[] getPropertyNames() {
    return names;
  }

  @Nullable
  @Override
  public Value getProperty(String name) {
    PropertyFile propertyFile = this.propertyFiles.get(name);
    return propertyFile != null ? propertyFile.getContent() : null;
  }

  @Nullable
  @Override
  public Origin getOrigin(String name) {
    PropertyFile propertyFile = this.propertyFiles.get(name);
    return propertyFile != null ? propertyFile.getOrigin() : null;
  }

  @Override
  public boolean isImmutable() {
    return !this.options.contains(Option.ALWAYS_READ);
  }

  /**
   * Property source options.
   */
  public enum Option {

    /**
     * Always read the value of the file when accessing the property value. When this
     * option is not set the property source will cache the value when it's first
     * read.
     */
    ALWAYS_READ,

    /**
     * Convert file and directory names to lowercase.
     */
    USE_LOWERCASE_NAMES,

    /**
     * Automatically attempt trim trailing new-line characters.
     */
    AUTO_TRIM_TRAILING_NEW_LINE

  }

  /**
   * A value returned from the property source which exposes the contents of the
   * property file. Values can either be treated as {@link CharSequence} or as an
   * {@link InputStreamSource}.
   */
  public interface Value extends CharSequence, InputStreamSource {

  }

  /**
   * A single property file that was found when the source was created.
   */
  private static final class PropertyFile {

    private static final Location START_OF_FILE = new Location(0, 0);

    private final Path path;

    private final PathResource resource;

    private final Origin origin;

    @Nullable
    private final PropertyFileContent cachedContent;

    private final boolean autoTrimTrailingNewLine;

    private PropertyFile(Path path, Set<Option> options) {
      this.path = path;
      this.resource = new PathResource(path);
      this.origin = new TextResourceOrigin(this.resource, START_OF_FILE);
      this.autoTrimTrailingNewLine = options.contains(Option.AUTO_TRIM_TRAILING_NEW_LINE);
      this.cachedContent = options.contains(Option.ALWAYS_READ)
              ? null
              : new PropertyFileContent(path, this.resource, this.origin, true, this.autoTrimTrailingNewLine);
    }

    PropertyFileContent getContent() {
      if (this.cachedContent != null) {
        return this.cachedContent;
      }
      return new PropertyFileContent(this.path, this.resource, this.origin, false, this.autoTrimTrailingNewLine);
    }

    Origin getOrigin() {
      return this.origin;
    }

    static Map<String, PropertyFile> findAll(Path sourceDirectory, Set<Option> options) {
      try {
        var propertyFiles = new TreeMap<String, PropertyFile>();
        try (Stream<Path> pathStream = Files.find(sourceDirectory, MAX_DEPTH,
                PropertyFile::isPropertyFile, FileVisitOption.FOLLOW_LINKS)) {
          pathStream.forEach((path) -> {
            String name = getName(sourceDirectory.relativize(path));
            if (StringUtils.hasText(name)) {
              if (options.contains(Option.USE_LOWERCASE_NAMES)) {
                name = name.toLowerCase(Locale.getDefault());
              }
              propertyFiles.put(name, new PropertyFile(path, options));
            }
          });
        }
        return Collections.unmodifiableMap(propertyFiles);
      }
      catch (IOException ex) {
        throw new IllegalStateException("Unable to find files in '" + sourceDirectory + "'", ex);
      }
    }

    private static boolean isPropertyFile(Path path, BasicFileAttributes attributes) {
      return !hasHiddenPathElement(path) && (attributes.isRegularFile() || attributes.isSymbolicLink());
    }

    private static boolean hasHiddenPathElement(Path path) {
      for (Path value : path) {
        if (value.toString().startsWith("..")) {
          return true;
        }
      }
      return false;
    }

    private static String getName(Path relativePath) {
      int nameCount = relativePath.getNameCount();
      if (nameCount == 1) {
        return relativePath.toString();
      }
      StringBuilder name = new StringBuilder();
      for (int i = 0; i < nameCount; i++) {
        name.append((i != 0) ? "." : "");
        name.append(relativePath.getName(i));
      }
      return name.toString();
    }

  }

  /**
   * The contents of a found property file.
   */
  private static final class PropertyFileContent implements Value, OriginProvider {

    private final Path path;

    private final ReentrantLock resourceLock = new ReentrantLock();

    private final Resource resource;

    private final Origin origin;

    private final boolean cacheContent;

    private final boolean autoTrimTrailingNewLine;

    private volatile byte[] content;

    private PropertyFileContent(Path path, Resource resource, Origin origin, boolean cacheContent,
            boolean autoTrimTrailingNewLine) {
      this.path = path;
      this.resource = resource;
      this.origin = origin;
      this.cacheContent = cacheContent;
      this.autoTrimTrailingNewLine = autoTrimTrailingNewLine;
    }

    @Override
    public Origin getOrigin() {
      return this.origin;
    }

    @Override
    public int length() {
      return toString().length();
    }

    @Override
    public char charAt(int index) {
      return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return toString().subSequence(start, end);
    }

    @Override
    public String toString() {
      String string = new String(getBytes());
      if (this.autoTrimTrailingNewLine) {
        string = autoTrimTrailingNewLine(string);
      }
      return string;
    }

    private String autoTrimTrailingNewLine(String string) {
      if (!string.endsWith("\n")) {
        return string;
      }
      int numberOfLines = 0;
      for (int i = 0; i < string.length(); i++) {
        char ch = string.charAt(i);
        if (ch == '\n') {
          numberOfLines++;
        }
      }
      if (numberOfLines > 1) {
        return string;
      }
      return (string.endsWith("\r\n")) ? string.substring(0, string.length() - 2)
              : string.substring(0, string.length() - 1);
    }

    @Override
    public InputStream getInputStream() throws IOException {
      if (!this.cacheContent) {
        assertStillExists();
        return this.resource.getInputStream();
      }
      return new ByteArrayInputStream(getBytes());
    }

    private byte[] getBytes() {
      try {
        if (!this.cacheContent) {
          assertStillExists();
          return FileCopyUtils.copyToByteArray(this.resource.getInputStream());
        }
        if (this.content == null) {
          assertStillExists();
          this.resourceLock.lock();
          try {
            if (this.content == null) {
              this.content = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
            }
          }
          finally {
            this.resourceLock.unlock();
          }
        }
        return this.content;
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    private void assertStillExists() {
      Assert.state(Files.exists(this.path), () -> "The property file '" + this.path + "' no longer exists");
    }

  }

}
