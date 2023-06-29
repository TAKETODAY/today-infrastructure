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

package cn.taketoday.mail.javamail;

import org.junit.jupiter.api.Test;

import java.io.File;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class ConfigurableMimeFileTypeMapTests {

  @Test
  public void againstDefaultConfiguration() throws Exception {
    ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
    ftm.afterPropertiesSet();

    assertThat(ftm.getContentType("foobar.HTM")).as("Invalid content type for HTM").isEqualTo("text/html");
    assertThat(ftm.getContentType("foobar.html")).as("Invalid content type for html").isEqualTo("text/html");
    assertThat(ftm.getContentType("foobar.c++")).as("Invalid content type for c++").isEqualTo("text/plain");
    assertThat(ftm.getContentType("foobar.svf")).as("Invalid content type for svf").isEqualTo("image/vnd.svf");
    assertThat(ftm.getContentType("foobar.dsf")).as("Invalid content type for dsf").isEqualTo("image/x-mgx-dsf");
    assertThat(ftm.getContentType("foobar.foo")).as("Invalid default content type").isEqualTo("application/octet-stream");
  }

  @Test
  public void againstDefaultConfigurationWithFilePath() throws Exception {
    ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
    assertThat(ftm.getContentType(new File("/tmp/foobar.HTM"))).as("Invalid content type for HTM").isEqualTo("text/html");
  }

  @Test
  public void withAdditionalMappings() throws Exception {
    ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
    ftm.setMappings(new String[] { "foo/bar HTM foo", "foo/cpp c++" });
    ftm.afterPropertiesSet();

    assertThat(ftm.getContentType("foobar.HTM")).as("Invalid content type for HTM - override didn't work").isEqualTo("foo/bar");
    assertThat(ftm.getContentType("foobar.c++")).as("Invalid content type for c++ - override didn't work").isEqualTo("foo/cpp");
    assertThat(ftm.getContentType("bar.foo")).as("Invalid content type for foo - new mapping didn't work").isEqualTo("foo/bar");
  }

  @Test
  public void withCustomMappingLocation() throws Exception {
    Resource resource = new ClassPathResource("test.mime.types", getClass());

    ConfigurableMimeFileTypeMap ftm = new ConfigurableMimeFileTypeMap();
    ftm.setMappingLocation(resource);
    ftm.afterPropertiesSet();

    assertThat(ftm.getContentType("foobar.foo")).as("Invalid content type for foo").isEqualTo("text/foo");
    assertThat(ftm.getContentType("foobar.bar")).as("Invalid content type for bar").isEqualTo("text/bar");
    assertThat(ftm.getContentType("foobar.fimg")).as("Invalid content type for fimg").isEqualTo("image/foo");
    assertThat(ftm.getContentType("foobar.bimg")).as("Invalid content type for bimg").isEqualTo("image/bar");
  }

}
