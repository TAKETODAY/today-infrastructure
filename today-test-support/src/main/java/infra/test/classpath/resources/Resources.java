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

package infra.test.classpath.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.FileSystemUtils;
import infra.util.function.ThrowingConsumer;

/**
 * A collection of resources.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class Resources {

  private final Path root;

  private final Map<String, Resource> resources = new HashMap<>();

  Resources(Path root) {
    this.root = root;
  }

  Resources addPackage(String packageName, String[] resourceNames) {
    Set<String> unmatchedNames = new HashSet<>(Arrays.asList(resourceNames));
    withPathsForPackage(packageName, (packagePath) -> {
      for (String resourceName : resourceNames) {
        Path resource = packagePath.resolve(resourceName);
        if (Files.exists(resource) && !Files.isDirectory(resource)) {
          Path target = this.root.resolve(resourceName);
          Path targetDirectory = target.getParent();
          if (!Files.isDirectory(targetDirectory)) {
            Files.createDirectories(targetDirectory);
          }
          Files.copy(resource, target);
          register(resourceName, target, true);
          unmatchedNames.remove(resourceName);
        }
      }
    });
    Assert.isTrue(unmatchedNames.isEmpty(), () -> "Package '%s' did not contain resources: %s".formatted(packageName, unmatchedNames));
    return this;
  }

  private void withPathsForPackage(String packageName, ThrowingConsumer<Path> consumer) {
    try {
      List<URL> sources = Collections
              .list(getClass().getClassLoader().getResources(packageName.replace(".", "/")));
      for (URL source : sources) {
        URI sourceUri = source.toURI();
        try {
          consumer.accept(Paths.get(sourceUri));
        }
        catch (FileSystemNotFoundException ex) {
          try (FileSystem fileSystem = FileSystems.newFileSystem(sourceUri, Collections.emptyMap())) {
            consumer.accept(Paths.get(sourceUri));
          }
        }
      }
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    catch (URISyntaxException ex) {
      throw new RuntimeException(ex);
    }
  }

  Resources addResource(String name, String content, boolean additional) {
    Path resourcePath = this.root.resolve(name);
    if (Files.isDirectory(resourcePath)) {
      throw new IllegalStateException(
              "Cannot create resource '%s' as a directory already exists at that location".formatted(name));
    }
    Path parent = resourcePath.getParent();
    try {
      if (!Files.isDirectory(resourcePath)) {
        Files.createDirectories(parent);
      }
      Files.writeString(resourcePath, processContent(content));
      register(name, resourcePath, additional);
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return this;
  }

  private void register(String name, Path resourcePath, boolean additional) {
    Resource resource = new Resource(resourcePath, additional);
    register(name, resource);
    Path ancestor = resourcePath.getParent();
    while (!this.root.equals(ancestor)) {
      Resource ancestorResource = new Resource(ancestor, additional);
      register(this.root.relativize(ancestor).toString(), ancestorResource);
      ancestor = ancestor.getParent();
    }
  }

  private void register(String name, Resource resource) {
    String normalized = name.replace("\\", "/");
    this.resources.put(normalized, resource);
    if (Files.isDirectory(resource.path())) {
      if (normalized.endsWith("/")) {
        this.resources.put(normalized.substring(0, normalized.length() - 1), resource);
      }
      else {
        this.resources.put(normalized + "/", resource);
      }
    }
  }

  private String processContent(String content) {
    return content.replace("${resourceRoot}", this.root.toString());
  }

  Resources addDirectory(String name) {
    Path directoryPath = this.root.resolve(name);
    if (Files.isRegularFile(directoryPath)) {
      throw new IllegalStateException(
              "Cannot create directory '" + name + " as a file already exists at that location");
    }
    try {
      Files.createDirectories(directoryPath);
      register(name, directoryPath, true);
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return this;
  }

  void delete() {
    try {
      FileSystemUtils.deleteRecursively(this.root);
      this.resources.clear();
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  Path getRoot() {
    return this.root;
  }

  @Nullable
  Resource find(String name) {
    return this.resources.get(name);
  }

}
