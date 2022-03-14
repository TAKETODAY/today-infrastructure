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

package cn.taketoday.beans.factory.config;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.support.DependencyInjectorAwareInstantiator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/24 17:37
 */
public class BeanDefinitionCustomizers {

  @Nullable
  protected List<BeanDefinitionCustomizer> customizers;

  @Nullable
  public List<BeanDefinitionCustomizer> getCustomizers() {
    return customizers;
  }

  public void addCustomizers(@Nullable BeanDefinitionCustomizer... customizers) {
    if (ObjectUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
  }

  public void addCustomizers(@Nullable List<BeanDefinitionCustomizer> customizers) {
    if (CollectionUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
  }

  /**
   * clear exist customizers and set
   *
   * @param customizers new customizers
   */
  public void setCustomizers(@Nullable BeanDefinitionCustomizer... customizers) {
    if (ObjectUtils.isNotEmpty(customizers)) {
      CollectionUtils.addAll(customizers(), customizers);
    }
    else {
      // clear
      if (this.customizers != null) {
        this.customizers.clear();
      }
    }
  }

  /**
   * set customizers
   *
   * @param customizers new customizers
   */
  public void setCustomizers(@Nullable List<BeanDefinitionCustomizer> customizers) {
    this.customizers = customizers;
  }

  private List<BeanDefinitionCustomizer> customizers() {
    if (customizers == null) {
      customizers = new ArrayList<>();
    }
    return customizers;
  }

  public void loadDefaultCustomizers() {
    loadDefaultCustomizers(null);
  }

  public void loadDefaultCustomizers(@Nullable BeanFactory beanFactory) {
    addCustomizers(TodayStrategies.get(
            BeanDefinitionCustomizer.class, DependencyInjectorAwareInstantiator.forFunction(beanFactory)));
  }

}
