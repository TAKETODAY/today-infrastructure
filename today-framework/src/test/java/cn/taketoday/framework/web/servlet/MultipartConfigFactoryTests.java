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

package cn.taketoday.framework.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.util.DataSize;
import jakarta.servlet.MultipartConfigElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultipartConfigFactory}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class MultipartConfigFactoryTests {

  @Test
  void sensibleDefaults() {
    MultipartConfigFactory factory = new MultipartConfigFactory();
    MultipartConfigElement config = factory.createMultipartConfig();
    assertThat(config.getLocation()).isEqualTo("");
    assertThat(config.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(512).toBytes());
    assertThat(config.getMaxRequestSize()).isEqualTo(DataSize.ofGigabytes(1).toBytes());
    assertThat(config.getFileSizeThreshold()).isEqualTo(0);
  }

  @Test
  void createWithDataSizes() {
    MultipartConfigFactory factory = new MultipartConfigFactory();
    factory.setMaxFileSize(DataSize.ofBytes(1));
    factory.setMaxRequestSize(DataSize.ofKilobytes(2));
    factory.setFileSizeThreshold(DataSize.ofMegabytes(3));
    MultipartConfigElement config = factory.createMultipartConfig();
    assertThat(config.getMaxFileSize()).isEqualTo(1L);
    assertThat(config.getMaxRequestSize()).isEqualTo(2 * 1024L);
    assertThat(config.getFileSizeThreshold()).isEqualTo(3 * 1024 * 1024);
  }

  @Test
  void createWithNegativeDataSizes() {
    MultipartConfigFactory factory = new MultipartConfigFactory();
    factory.setMaxFileSize(DataSize.ofBytes(-1));
    factory.setMaxRequestSize(DataSize.ofKilobytes(-2));
    factory.setFileSizeThreshold(DataSize.ofMegabytes(-3));
    MultipartConfigElement config = factory.createMultipartConfig();
    assertThat(config.getMaxFileSize()).isEqualTo(-1L);
    assertThat(config.getMaxRequestSize()).isEqualTo(-1);
    assertThat(config.getFileSizeThreshold()).isEqualTo(0);
  }

}
