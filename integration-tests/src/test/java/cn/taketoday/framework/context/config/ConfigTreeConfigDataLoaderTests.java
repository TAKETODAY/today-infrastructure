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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigTreeConfigDataLoader}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigTreeConfigDataLoaderTests {

  private ConfigTreeConfigDataLoader loader = new ConfigTreeConfigDataLoader();

  private ConfigDataLoaderContext loaderContext = mock(ConfigDataLoaderContext.class);

  @TempDir
  Path directory;

  @Test
  void loadReturnsConfigDataWithPropertySource() throws IOException {
    File file = this.directory.resolve("hello").toFile();
    file.getParentFile().mkdirs();
    FileCopyUtils.copy("world\n".getBytes(StandardCharsets.UTF_8), file);
    ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource(this.directory.toString());
    ConfigData configData = this.loader.load(this.loaderContext, location);
    assertThat(configData.getPropertySources().size()).isEqualTo(1);
    PropertySource<?> source = configData.getPropertySources().get(0);
    assertThat(source.getName()).isEqualTo("Config tree '" + this.directory.toString() + "'");
    assertThat(source.getProperty("hello").toString()).isEqualTo("world");
  }

  @Test
  void loadWhenPathDoesNotExistThrowsException() {
    File missing = this.directory.resolve("missing").toFile();
    ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource(missing.toString());
    assertThatExceptionOfType(ConfigDataResourceNotFoundException.class)
            .isThrownBy(() -> this.loader.load(this.loaderContext, location));
  }

}
