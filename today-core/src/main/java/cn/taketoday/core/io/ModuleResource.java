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

package cn.taketoday.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link Resource} implementation for {@link java.lang.Module} resolution,
 * performing {@link #getInputStream()} access via {@link Module#getResourceAsStream}.
 *
 * <p>Alternatively, consider accessing resources in a module path layout via
 * {@link ClassPathResource} for exported resources, or specifically relative to
 * a {@code Class} via {@link ClassPathResource#ClassPathResource(String, Class)}
 * for local resolution within the containing module of that specific class.
 * In common scenarios, module resources will simply be transparently visible as
 * classpath resources and therefore do not need any special treatment at all.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Module#getResourceAsStream
 * @see ClassPathResource
 * @since 4.0
 */
public class ModuleResource extends AbstractResource {

  private final Module module;

  private final String path;

  /**
   * Create a new {@code ModuleResource} for the given {@link Module}
   * and the given resource path.
   *
   * @param module the runtime module to search within
   * @param path the resource path within the module
   */
  public ModuleResource(Module module, String path) {
    Assert.notNull(module, "Module must not be null");
    Assert.notNull(path, "Path must not be null");
    this.module = module;
    this.path = path;
  }

  /**
   * Return the {@link Module} for this resource.
   */
  public final Module getModule() {
    return this.module;
  }

  /**
   * Return the path for this resource.
   */
  public final String getPath() {
    return this.path;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    InputStream is = this.module.getResourceAsStream(this.path);
    if (is == null) {
      throw new FileNotFoundException(this + " cannot be opened because it does not exist");
    }
    return is;
  }

  @Override
  public Resource createRelative(String relativePath) {
    String pathToUse = ResourceUtils.getRelativePath(this.path, relativePath);
    return new ModuleResource(this.module, pathToUse);
  }

  @Override
  @Nullable
  public String getName() {
    return StringUtils.getFilename(this.path);
  }

  @Override
  public String toString() {
    return "module resource [" + this.path + "]" +
            (this.module.isNamed() ? " from module '" + this.module.getName() + "'" : "");
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    return (this == obj || (obj instanceof ModuleResource that &&
            this.module.equals(that.module) && this.path.equals(that.path)));
  }

  @Override
  public int hashCode() {
    return this.module.hashCode() * 31 + this.path.hashCode();
  }

}
