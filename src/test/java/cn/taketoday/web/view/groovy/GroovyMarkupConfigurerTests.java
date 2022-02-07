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

package cn.taketoday.web.view.groovy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.i18n.LocaleContextHolder;
import groovy.text.TemplateEngine;
import groovy.text.markup.MarkupTemplateEngine;
import groovy.text.markup.TemplateConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Unit tests for
 * {@link cn.taketoday.web.view.groovy.GroovyMarkupConfigurer}.
 *
 * @author Brian Clozel
 */
public class GroovyMarkupConfigurerTests {

  private static final String RESOURCE_LOADER_PATH = "classpath:cn/taketoday/web/servlet/view/groovy/";

  private StaticApplicationContext applicationContext;

  private static final String TEMPLATE_PREFIX = "cn/taketoday/web/servlet/view/groovy/";

  private GroovyMarkupConfigurer configurer;

  @BeforeEach
  public void setup() throws Exception {
    this.applicationContext = new StaticApplicationContext();
    this.configurer = new GroovyMarkupConfigurer();
    this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH);
  }

  @Test
  public void defaultTemplateEngine() throws Exception {
    this.configurer.setApplicationContext(this.applicationContext);
    this.configurer.afterPropertiesSet();

    TemplateEngine engine = this.configurer.getTemplateEngine();
    assertThat(engine).isNotNull();
    assertThat(engine.getClass()).isEqualTo(MarkupTemplateEngine.class);

    MarkupTemplateEngine markupEngine = (MarkupTemplateEngine) engine;
    TemplateConfiguration configuration = markupEngine.getTemplateConfiguration();
    assertThat(configuration).isNotNull();
    assertThat(configuration.getClass()).isEqualTo(GroovyMarkupConfigurer.class);
  }

  @Test
  public void customTemplateEngine() throws Exception {
    this.configurer.setApplicationContext(this.applicationContext);
    this.configurer.setTemplateEngine(new TestTemplateEngine());
    this.configurer.afterPropertiesSet();

    TemplateEngine engine = this.configurer.getTemplateEngine();
    assertThat(engine).isNotNull();
    assertThat(engine.getClass()).isEqualTo(TestTemplateEngine.class);
  }

  @Test
  public void customTemplateConfiguration() throws Exception {
    this.configurer.setApplicationContext(this.applicationContext);
    this.configurer.setCacheTemplates(false);
    this.configurer.afterPropertiesSet();

    TemplateEngine engine = this.configurer.getTemplateEngine();
    assertThat(engine).isNotNull();
    assertThat(engine.getClass()).isEqualTo(MarkupTemplateEngine.class);

    MarkupTemplateEngine markupEngine = (MarkupTemplateEngine) engine;
    TemplateConfiguration configuration = markupEngine.getTemplateConfiguration();
    assertThat(configuration).isNotNull();
    assertThat(configuration.isCacheTemplates()).isFalse();
  }

  @Test
  @SuppressWarnings("resource")
  public void parentLoader() throws Exception {

    this.configurer.setApplicationContext(this.applicationContext);

    ClassLoader classLoader = this.configurer.createTemplateClassLoader();
    assertThat(classLoader).isNotNull();
    URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
    assertThat(urlClassLoader.getURLs()).hasSize(1);
    assertThat(urlClassLoader.getURLs()[0].toString())
            .endsWith("cn/taketoday/web/servlet/view/groovy/");

    this.configurer.setResourceLoaderPath(RESOURCE_LOADER_PATH + ",classpath:cn/taketoday/web/servlet/view/");
    classLoader = this.configurer.createTemplateClassLoader();
    assertThat(classLoader).isNotNull();
    urlClassLoader = (URLClassLoader) classLoader;
    assertThat(urlClassLoader.getURLs()).hasSize(2);
    assertThat(urlClassLoader.getURLs()[0].toString())
            .endsWith("cn/taketoday/web/servlet/view/groovy/");
    assertThat(urlClassLoader.getURLs()[1].toString())
            .endsWith("cn/taketoday/web/servlet/view/");
  }

  private class TestTemplateEngine extends MarkupTemplateEngine {

    public TestTemplateEngine() {
      super(new TemplateConfiguration());
    }
  }

  @Test
  public void resolveSampleTemplate() throws Exception {
    URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "test.tpl");
    assertThat(url).isNotNull();
  }

  @Test
  public void resolveI18nFullLocale() throws Exception {
    LocaleContextHolder.setLocale(Locale.GERMANY);
    URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
    assertThat(url).isNotNull();
    assertThat(url.getPath()).contains("i18n_de_DE.tpl");
  }

  @Test
  public void resolveI18nPartialLocale() throws Exception {
    LocaleContextHolder.setLocale(Locale.FRANCE);
    URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
    assertThat(url).isNotNull();
    assertThat(url.getPath()).contains("i18n_fr.tpl");
  }

  @Test
  public void resolveI18nDefaultLocale() throws Exception {
    LocaleContextHolder.setLocale(Locale.US);
    URL url = this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "i18n.tpl");
    assertThat(url).isNotNull();
    assertThat(url.getPath()).contains("i18n.tpl");
  }

  @Test
  public void failMissingTemplate() throws Exception {
    LocaleContextHolder.setLocale(Locale.US);
    assertThatIOException().isThrownBy(() ->
            this.configurer.resolveTemplate(getClass().getClassLoader(), TEMPLATE_PREFIX + "missing.tpl"));
  }

}
