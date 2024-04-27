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

package cn.taketoday.ui.freemarker;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.io.ByteArrayResource;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/5 13:06
 */
class FreeMarkerConfigurationFactoryBeanTests {

  private final FreeMarkerConfigurationFactoryBean fcfb = new FreeMarkerConfigurationFactoryBean();

  @Test
  void getObjectType() {
    assertThat(fcfb.getObjectType()).isEqualTo(Configuration.class);
  }

  @Test
  void isSingleton() {
    assertThat(fcfb.isSingleton()).isTrue();
  }

  @Test
  public void freeMarkerConfigurationFactoryBeanWithConfigLocation() throws Exception {
    fcfb.setConfigLocation(new FileSystemResource("myprops.properties"));
    Properties props = new Properties();
    props.setProperty("myprop", "/mydir");
    fcfb.setFreemarkerSettings(props);
    assertThatIOException().isThrownBy(fcfb::afterPropertiesSet);
  }

  @Test
  public void freeMarkerConfigurationFactoryBeanWithResourceLoaderPath() throws Exception {
    fcfb.setTemplateLoaderPath("file:/mydir");
    fcfb.afterPropertiesSet();
    Configuration cfg = fcfb.getObject();
    assertThat(cfg.getTemplateLoader()).isInstanceOf(InfraTemplateLoader.class);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void freeMarkerConfigurationFactoryBeanWithNonFileResourceLoaderPath() throws Exception {
    fcfb.setTemplateLoaderPath("file:/mydir");
    Properties settings = new Properties();
    settings.setProperty("localized_lookup", "false");
    fcfb.setFreemarkerSettings(settings);
    fcfb.setResourceLoader(new ResourceLoader() {
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
    fcfb.afterPropertiesSet();
    assertThat(fcfb.getObject()).isInstanceOf(Configuration.class);
    Configuration fc = fcfb.getObject();
    Template ft = fc.getTemplate("test");
    assertThat(FreeMarkerTemplateUtils.processTemplateIntoString(ft, new HashMap())).isEqualTo("test");
  }

  @Test
  public void freeMarkerConfigurationAsBean() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition loaderDef = new RootBeanDefinition(InfraTemplateLoader.class);
    loaderDef.getConstructorArgumentValues().addGenericArgumentValue(new DefaultResourceLoader());
    loaderDef.getConstructorArgumentValues().addGenericArgumentValue("/freemarker");
    RootBeanDefinition configDef = new RootBeanDefinition(Configuration.class);
    configDef.getPropertyValues().add("templateLoader", loaderDef);
    beanFactory.registerBeanDefinition("freeMarkerConfig", configDef);
    assertThat(beanFactory.getBean(Configuration.class)).isNotNull();
  }

  @Test
  void setFreemarkerVariables() throws TemplateException, IOException {
    fcfb.setFreemarkerVariables(Map.of("foo", "bar"));
    fcfb.afterPropertiesSet();
    Configuration configuration = fcfb.getObject();
    assertThat(configuration.getSharedVariableNames()).contains("foo");
  }

  @Test
  void defaultEncoding() throws TemplateException, IOException {
    fcfb.setDefaultEncoding("GBK");
    fcfb.afterPropertiesSet();
    Configuration configuration = fcfb.getObject();
    assertThat(configuration.getDefaultEncoding()).contains("GBK");
  }

  @Test
  void setPreTemplateLoaders() throws TemplateException, IOException {
    TemplateLoader loader = mock();
    fcfb.setPreTemplateLoaders(loader);

    assertThat(fcfb.createConfiguration().getTemplateLoader()).isEqualTo(loader);

    TemplateLoader loader2 = mock();
    fcfb.setPreTemplateLoaders(loader, loader2);
    assertThat(fcfb.createConfiguration().getTemplateLoader()).isInstanceOf(MultiTemplateLoader.class);
  }

  @Test
  void setPostTemplateLoaders() throws TemplateException, IOException {
    TemplateLoader loader = mock();
    fcfb.setPostTemplateLoaders(loader);
    fcfb.afterPropertiesSet();

    Configuration configuration = fcfb.getObject();
    assertThat(configuration.getTemplateLoader()).isEqualTo(loader);
  }

  @Test
  void getAggregateTemplateLoader() {
    TemplateLoader loader = mock();

    assertThat(fcfb.getAggregateTemplateLoader(List.of())).isNull();
    assertThat(fcfb.getAggregateTemplateLoader(List.of(loader))).isEqualTo(loader);
    assertThat(fcfb.getAggregateTemplateLoader(List.of(loader, loader))).isInstanceOf(MultiTemplateLoader.class);
  }
}
