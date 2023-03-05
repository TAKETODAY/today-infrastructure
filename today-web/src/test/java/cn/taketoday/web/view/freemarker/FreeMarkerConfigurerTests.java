/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.view.freemarker;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Properties;

import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * @author Juergen Hoeller
 * @author Issam El-atif
 * @author Sam Brannen
 */
public class FreeMarkerConfigurerTests {

  private final FreeMarkerConfigurer freeMarkerConfigurer = new FreeMarkerConfigurer();

  @Test
  public void freeMarkerConfigurerWithConfigLocation() {
    freeMarkerConfigurer.setConfigLocation(new FileSystemResource("myprops.properties"));
    Properties props = new Properties();
    props.setProperty("myprop", "/mydir");
    freeMarkerConfigurer.setFreemarkerSettings(props);
    assertThatIOException().isThrownBy(freeMarkerConfigurer::afterPropertiesSet);
  }

  @Test
  public void freeMarkerConfigurerWithResourceLoaderPath() throws Exception {
    freeMarkerConfigurer.setTemplateLoaderPath("file:/mydir");
    freeMarkerConfigurer.afterPropertiesSet();
    Configuration cfg = freeMarkerConfigurer.getConfiguration();
    assertThat(cfg.getTemplateLoader()).isInstanceOf(MultiTemplateLoader.class);
    MultiTemplateLoader multiTemplateLoader = (MultiTemplateLoader) cfg.getTemplateLoader();
    assertThat(multiTemplateLoader.getTemplateLoader(0)).isInstanceOf(InfraTemplateLoader.class);
    assertThat(multiTemplateLoader.getTemplateLoader(1)).isInstanceOf(ClassTemplateLoader.class);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void freeMarkerConfigurerWithNonFileResourceLoaderPath() throws Exception {
    freeMarkerConfigurer.setTemplateLoaderPath("file:/mydir");
    Properties settings = new Properties();
    settings.setProperty("localized_lookup", "false");
    freeMarkerConfigurer.setFreemarkerSettings(settings);
    freeMarkerConfigurer.setResourceLoader(new ResourceLoader() {
      @Override
      public Resource getResource(String location) {
        if (!("file:/mydir".equals(location) || "file:/mydir/test".equals(location))) {
          throw new IllegalArgumentException(location);
        }
        return new ByteArrayResource("test".getBytes(), "test");
      }

      @Override
      public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
      }
    });
    freeMarkerConfigurer.afterPropertiesSet();
    assertThat(freeMarkerConfigurer.getConfiguration()).isInstanceOf(Configuration.class);
    Configuration fc = freeMarkerConfigurer.getConfiguration();
    Template ft = fc.getTemplate("test");
    assertThat(FreeMarkerTemplateUtils.processTemplateIntoString(ft, new HashMap())).isEqualTo("test");
  }

}
