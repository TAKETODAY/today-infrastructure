/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.aot.generate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import infra.core.io.FileSystemResource;
import infra.core.io.InputStreamSource;
import infra.lang.Assert;
import infra.util.function.ThrowingConsumer;

/**
 * {@link GeneratedFiles} implementation that stores generated files using a
 * {@link FileSystem}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FileSystemGeneratedFiles implements GeneratedFiles {

  private final Function<Kind, Path> roots;

  /**
   * Create a new {@link FileSystemGeneratedFiles} instance with all files
   * stored under the specific {@code root}. The following subdirectories are
   * created for the different file {@link Kind kinds}:
   * <ul>
   * <li>{@code sources}</li>
   * <li>{@code resources}</li>
   * <li>{@code classes}</li>
   * </ul>
   *
   * @param root the root path
   * @see #FileSystemGeneratedFiles(Function)
   */
  public FileSystemGeneratedFiles(Path root) {
    this(conventionRoots(root));
  }

  /**
   * Create a new {@link FileSystemGeneratedFiles} instance with all files
   * stored under the root provided by the given {@link Function}.
   *
   * @param roots a function that returns the root to use for the given
   * {@link Kind}
   */
  public FileSystemGeneratedFiles(Function<Kind, Path> roots) {
    Assert.notNull(roots, "'roots' is required");
    Assert.isTrue(Arrays.stream(Kind.values()).map(roots).noneMatch(Objects::isNull),
            "'roots' must return a value for all file kinds");
    this.roots = roots;
  }

  private static Function<Kind, Path> conventionRoots(Path root) {
    Assert.notNull(root, "'root' is required");
    return kind -> switch (kind) {
      case SOURCE -> root.resolve("sources");
      case RESOURCE -> root.resolve("resources");
      case CLASS -> root.resolve("classes");
    };
  }

  @Override
  public void handleFile(Kind kind, String path, ThrowingConsumer<FileHandler> handler) {
    FileSystemFileHandler fileHandler = new FileSystemFileHandler(toPath(kind, path));
    handler.accept(fileHandler);
  }

  private Path toPath(Kind kind, String path) {
    Assert.notNull(kind, "'kind' is required");
    Assert.hasLength(path, "'path' must not be empty");
    Path root = this.roots.apply(kind).toAbsolutePath().normalize();
    Path relativePath = root.resolve(path).toAbsolutePath().normalize();
    Assert.isTrue(relativePath.startsWith(root), "'path' must be relative");
    return relativePath;
  }

  static final class FileSystemFileHandler extends FileHandler {

    private final Path path;

    FileSystemFileHandler(Path path) {
      super(Files.exists(path), () -> new FileSystemResource(path));
      this.path = path;
    }

    @Override
    protected void copy(InputStreamSource content, boolean override) {
      if (override) {
        copy(content, StandardCopyOption.REPLACE_EXISTING);
      }
      else {
        copy(content);
      }
    }

    private void copy(InputStreamSource content, CopyOption... copyOptions) {
      try {
        try (InputStream inputStream = content.getInputStream()) {
          Files.createDirectories(this.path.getParent());
          Files.copy(inputStream, this.path, copyOptions);
        }
      }
      catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @Override
    public String toString() {
      return this.path.toString();
    }
  }

}
