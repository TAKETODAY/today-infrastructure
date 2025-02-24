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

package infra.app.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

import infra.core.io.ClassPathResource;
import infra.core.io.ContextResource;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.FileSystemResource;
import infra.core.io.ProtocolResolver;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.util.StringUtils;

/**
 * Class can be used to obtain {@link ResourceLoader ResourceLoaders} supporting
 * additional {@link ProtocolResolver ProtocolResolvers} registered in
 * {@code today.strategies}.
 * <p>
 * When not delegating to an existing resource loader, plain paths without a qualifier
 * will resolve to file system resources. This is different from
 * {@code DefaultResourceLoader}, which resolves unqualified paths to classpath resources.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationResourceLoader extends DefaultResourceLoader {

  @Override
  protected Resource getResourceByPath(String path) {
    return new ApplicationResource(path);
  }

  /**
   * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
   * ProtocolResolvers} registered in {@code today.strategies}. The factories file will
   * be resolved using the default class loader at the time this call is made. Resources
   * will be resolved using the default class loader at the time they are resolved.
   *
   * @return a {@link ResourceLoader} instance
   */
  public static ResourceLoader of() {
    return of((ClassLoader) null);
  }

  /**
   * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
   * ProtocolResolvers} registered in {@code today.strategies}. The factories files and
   * resources will be resolved using the specified class loader.
   *
   * @param classLoader the class loader to use or {@code null} to use the default class
   * loader
   * @return a {@link ResourceLoader} instance
   */
  public static ResourceLoader of(@Nullable ClassLoader classLoader) {
    return of(classLoader, TodayStrategies.forDefaultResourceLocation(classLoader));
  }

  /**
   * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
   * ProtocolResolvers} registered in {@code today.strategies}.
   *
   * @param classLoader the class loader to use or {@code null} to use the default class
   * loader
   * @param strategies the {@link TodayStrategies} used to load
   * {@link ProtocolResolver ProtocolResolvers}
   * @return a {@link ResourceLoader} instance
   */
  public static ResourceLoader of(@Nullable ClassLoader classLoader, TodayStrategies strategies) {
    return of(classLoader, strategies, null);
  }

  /**
   * Return a {@link ResourceLoader} supporting additional {@link ProtocolResolver
   * ProtocolResolvers} registered in {@code today.strategies}.
   *
   * @param classLoader the class loader to use or {@code null} to use the default class
   * loader
   * @param strategies the {@link TodayStrategies} used to load
   * {@link ProtocolResolver ProtocolResolvers}
   * @param workingDirectory the working directory
   * @return a {@link ResourceLoader} instance
   */
  public static ResourceLoader of(@Nullable ClassLoader classLoader, TodayStrategies strategies, @Nullable Path workingDirectory) {
    return of(ApplicationFileSystemResourceLoader.get(classLoader, workingDirectory), strategies);
  }

  /**
   * Return a {@link ResourceLoader} delegating to the given resource loader and
   * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
   * {@code today.strategies}. The factories file will be resolved using the default
   * class loader at the time this call is made.
   *
   * @param resourceLoader the delegate resource loader
   * @return a {@link ResourceLoader} instance
   */
  public static ResourceLoader of(ResourceLoader resourceLoader) {
    return of(resourceLoader, false);
  }

  /**
   * Return a {@link ResourceLoader} delegating to the given resource loader and
   * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
   * {@code today.strategies}. The factories file will be resolved using the default
   * class loader at the time this call is made.
   *
   * @param resourceLoader the delegate resource loader
   * @param preferFileResolution if file based resolution is preferred over
   * {@code ServletContextResource} or {@link ClassPathResource} when no resource prefix
   * is provided.
   * @return a {@link ResourceLoader} instance
   */
  public static ResourceLoader of(ResourceLoader resourceLoader, boolean preferFileResolution) {
    Assert.notNull(resourceLoader, "'resourceLoader' is required");
    return of(resourceLoader, TodayStrategies.forDefaultResourceLocation(resourceLoader.getClassLoader()), preferFileResolution);
  }

  /**
   * Return a {@link ResourceLoader} delegating to the given resource loader and
   * supporting additional {@link ProtocolResolver ProtocolResolvers} registered in
   * {@code today.strategies}.
   *
   * @param resourceLoader the delegate resource loader
   * @param strategies the {@link TodayStrategies} used to load
   * {@link ProtocolResolver ProtocolResolvers}
   * @return a {@link ResourceLoader} instance
   */
  public static ResourceLoader of(ResourceLoader resourceLoader, TodayStrategies strategies) {
    return of(resourceLoader, strategies, false);
  }

  private static ResourceLoader of(ResourceLoader resourceLoader, TodayStrategies strategies, boolean preferFileResolution) {
    Assert.notNull(resourceLoader, "'resourceLoader' is required");
    Assert.notNull(strategies, "'strategies' is required");
    return new ProtocolResolvingResourceLoader(resourceLoader, strategies.load(ProtocolResolver.class), preferFileResolution);
  }

  /**
   * Internal {@link ResourceLoader} used to load {@link ApplicationResource}.
   */
  private static final class ApplicationFileSystemResourceLoader extends DefaultResourceLoader {

    private static final ResourceLoader shared = new ApplicationFileSystemResourceLoader(null, null);

    @Nullable
    private final Path workingDirectory;

    private ApplicationFileSystemResourceLoader(@Nullable ClassLoader classLoader, @Nullable Path workingDirectory) {
      super(classLoader);
      this.workingDirectory = workingDirectory;
    }

    @Override
    public Resource getResource(String location) {
      Resource resource = super.getResource(location);
      if (this.workingDirectory == null) {
        return resource;
      }
      if (!resource.isFile()) {
        return resource;
      }
      return resolveFile(resource, workingDirectory);
    }

    private Resource resolveFile(Resource resource, Path workingDirectory) {
      try {
        File file = resource.getFile();
        return new ApplicationResource(workingDirectory.resolve(file.toPath()));
      }
      catch (FileNotFoundException ex) {
        return resource;
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    @Override
    protected Resource getResourceByPath(String path) {
      return new ApplicationResource(path);
    }

    static ResourceLoader get(@Nullable ClassLoader classLoader, @Nullable Path workingDirectory) {
      if (classLoader == null && workingDirectory != null) {
        throw new IllegalArgumentException(
                "It's not possible to use null as 'classLoader' but specify a 'workingDirectory'");
      }
      return classLoader != null
              ? new ApplicationFileSystemResourceLoader(classLoader, workingDirectory)
              : ApplicationFileSystemResourceLoader.shared;
    }

  }

  /**
   * An application {@link Resource}.
   */
  private static final class ApplicationResource extends FileSystemResource implements ContextResource {

    ApplicationResource(String path) {
      super(path);
    }

    ApplicationResource(Path path) {
      super(path);
    }

    @Override
    public String getPathWithinContext() {
      return getPath();
    }

  }

  /**
   * {@link ResourceLoader} decorator that adds support for additional
   * {@link ProtocolResolver ProtocolResolvers}.
   */
  private static class ProtocolResolvingResourceLoader implements ResourceLoader {

    private final ResourceLoader resourceLoader;

    private final List<ProtocolResolver> protocolResolvers;

    private final boolean preferFileResolution;

    ProtocolResolvingResourceLoader(ResourceLoader resourceLoader,
            List<ProtocolResolver> protocolResolvers, boolean preferFileResolution) {
      this.resourceLoader = resourceLoader;
      this.protocolResolvers = protocolResolvers;
      this.preferFileResolution = preferFileResolution;
    }

    @Override
    public Resource getResource(String location) {
      if (StringUtils.isNotEmpty(location)) {
        for (ProtocolResolver protocolResolver : protocolResolvers) {
          Resource resource = protocolResolver.resolve(location, this);
          if (resource != null) {
            return resource;
          }
        }
      }

      Resource resource = resourceLoader.getResource(location);
      if (preferFileResolution && isClassPathResourceByPath(location, resource)) {
        resource = new ApplicationResource(location);
      }
      return resource;
    }

    private boolean isClassPathResourceByPath(String location, Resource resource) {
      return resource instanceof ClassPathResource && !location.startsWith(CLASSPATH_URL_PREFIX);
    }

    @Nullable
    @Override
    public ClassLoader getClassLoader() {
      return this.resourceLoader.getClassLoader();
    }

  }

}
