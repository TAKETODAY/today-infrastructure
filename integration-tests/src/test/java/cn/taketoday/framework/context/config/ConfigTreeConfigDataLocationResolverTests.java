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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigTreeConfigDataLocationResolver}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigTreeConfigDataLocationResolverTests {

  private ConfigTreeConfigDataLocationResolver resolver = new ConfigTreeConfigDataLocationResolver(
          new DefaultResourceLoader());

  private ConfigDataLocationResolverContext context = mock(ConfigDataLocationResolverContext.class);

  @TempDir
  File temp;

  @Test
  void isResolvableWhenPrefixMatchesReturnsTrue() {
    assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.valueOf("configtree:/etc/config"))).isTrue();
  }

  @Test
  void isResolvableWhenPrefixDoesNotMatchReturnsFalse() {
    assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.valueOf("http://etc/config"))).isFalse();
    assertThat(this.resolver.isResolvable(this.context, ConfigDataLocation.valueOf("/etc/config"))).isFalse();
  }

  @Test
  void resolveReturnsConfigVolumeMountLocation() {
    List<ConfigTreeConfigDataResource> locations = this.resolver.resolve(this.context,
            ConfigDataLocation.valueOf("configtree:/etc/config/"));
    assertThat(locations.size()).isEqualTo(1);
    assertThat(locations).extracting(Object::toString)
            .containsExactly("config tree [" + new File("/etc/config").getAbsolutePath() + "]");
  }

  @Test
  void resolveWilcardPattern() throws Exception {
    File directoryA = new File(this.temp, "a");
    File directoryB = new File(this.temp, "b");
    directoryA.mkdirs();
    directoryB.mkdirs();
    FileCopyUtils.copy("test".getBytes(), new File(directoryA, "spring"));
    FileCopyUtils.copy("test".getBytes(), new File(directoryB, "boot"));
    List<ConfigTreeConfigDataResource> locations = this.resolver.resolve(this.context,
            ConfigDataLocation.valueOf("configtree:" + this.temp.getAbsolutePath() + "/*/"));
    assertThat(locations.size()).isEqualTo(2);
    assertThat(locations).extracting(Object::toString).containsExactly(
            "config tree [" + directoryA.getAbsolutePath() + "]",
            "config tree [" + directoryB.getAbsolutePath() + "]");
  }

}
