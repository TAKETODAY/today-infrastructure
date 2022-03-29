/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.context.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import cn.taketoday.framework.env.ConfigTreePropertySource;
import cn.taketoday.lang.Assert;

/**
 * {@link ConfigDataResource} backed by a config tree directory.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @see ConfigTreePropertySource
 * @since 4.0
 */
public class ConfigTreeConfigDataResource extends ConfigDataResource {

  private final Path path;

  ConfigTreeConfigDataResource(String path) {
    Assert.notNull(path, "Path must not be null");
    this.path = Paths.get(path).toAbsolutePath();
  }

  ConfigTreeConfigDataResource(Path path) {
    Assert.notNull(path, "Path must not be null");
    this.path = path.toAbsolutePath();
  }

  Path getPath() {
    return this.path;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ConfigTreeConfigDataResource other = (ConfigTreeConfigDataResource) obj;
    return Objects.equals(this.path, other.path);
  }

  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

  @Override
  public String toString() {
    return "config tree [" + this.path + "]";
  }

}
