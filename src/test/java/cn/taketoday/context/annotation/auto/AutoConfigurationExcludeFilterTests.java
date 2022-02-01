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

package cn.taketoday.context.annotation.auto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.ComponentScan.Filter;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.FilterType;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 23:50
 */
class AutoConfigurationExcludeFilterTests {

  private static final Class<?> FILTERED = ExampleFilteredAutoConfiguration.class;

  private StandardApplicationContext context;

  @AfterEach
  void cleanUp() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void filterExcludeAutoConfiguration() {
    this.context = new StandardApplicationContext(Config.class);
    assertThat(this.context.getBeansOfType(String.class)).hasSize(1);
    assertThat(this.context.getBean(String.class)).isEqualTo("test");
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> BeanFactoryUtils.requiredBean(context, FILTERED));
  }

  @Configuration(proxyBeanMethods = false)
  @ComponentScan(basePackageClasses = ExampleConfiguration.class,
                 excludeFilters = @Filter(type = FilterType.CUSTOM, classes = TestAutoConfigurationExcludeFilter.class))
  static class Config {

  }

  static class TestAutoConfigurationExcludeFilter extends AutoConfigurationExcludeFilter {

    @Override
    protected List<String> getAutoConfigurations() {
      return Collections.singletonList(FILTERED.getName());
    }

  }

}
