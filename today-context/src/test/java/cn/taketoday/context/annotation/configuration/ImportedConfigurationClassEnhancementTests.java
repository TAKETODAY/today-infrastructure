/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests cornering the bug exposed in SPR-6779.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class ImportedConfigurationClassEnhancementTests {

  @Test
  public void autowiredConfigClassIsEnhancedWhenImported() {
    autowiredConfigClassIsEnhanced(ConfigThatDoesImport.class);
  }

  @Test
  public void autowiredConfigClassIsEnhancedWhenRegisteredViaConstructor() {
    autowiredConfigClassIsEnhanced(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
  }

  @SuppressWarnings("deprecation")
  private void autowiredConfigClassIsEnhanced(Class<?>... configClasses) {
    ApplicationContext ctx = new StandardApplicationContext(configClasses);
    Config config = ctx.getBean(Config.class);
    assertThat(AopUtils.isCglibProxy(config.autowiredConfig)).as("autowired config class has not been enhanced").isTrue();
  }

  @Test
  public void autowiredConfigClassBeanMethodsRespectScopingWhenImported() {
    autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesImport.class);
  }

  @Test
  public void autowiredConfigClassBeanMethodsRespectScopingWhenRegisteredViaConstructor() {
    autowiredConfigClassBeanMethodsRespectScoping(ConfigThatDoesNotImport.class, ConfigToBeAutowired.class);
  }

  private void autowiredConfigClassBeanMethodsRespectScoping(Class<?>... configClasses) {
    ApplicationContext ctx = new StandardApplicationContext(configClasses);
    Config config = ctx.getBean(Config.class);
    TestBean testBean1 = config.autowiredConfig.testBean();
    TestBean testBean2 = config.autowiredConfig.testBean();
    assertThat(testBean1)
            .as("got two distinct instances of testBean when singleton scoping was expected")
            .isSameAs(testBean2);
  }

  @Test
  public void importingNonConfigurationClassCausesBeanDefinitionParsingException() {
    ApplicationContext ctx = new StandardApplicationContext(ConfigThatImportsNonConfigClass.class);
    ConfigThatImportsNonConfigClass config = ctx.getBean(ConfigThatImportsNonConfigClass.class);
    assertThat(config.testBean).isSameAs(ctx.getBean(TestBean.class));
  }

  @Configuration
  static class ConfigToBeAutowired {

    public @Bean
    TestBean testBean() {
      return new TestBean();
    }
  }

  static class Config {

    @Autowired
    ConfigToBeAutowired autowiredConfig;
  }

  @Import(ConfigToBeAutowired.class)
  @Configuration
  static class ConfigThatDoesImport extends Config {
  }

  @Configuration
  static class ConfigThatDoesNotImport extends Config {
  }

  @Configuration
  @Import(TestBean.class)
  static class ConfigThatImportsNonConfigClass {

    @Autowired
    TestBean testBean;
  }

}
