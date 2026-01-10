/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.ui.freemarker;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.io.ByteArrayResource;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.FileSystemResource;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

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
