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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.BeanMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author TODAY 2021/6/1 21:05
 */
public class BeanMetadataTests {

  @Test
  public void beanMetadata() {
    final BeanMetadata beanMetadata = BeanMetadata.forClass(BeanMappingTestBean.class);
    final Object instance = beanMetadata.newInstance();

    assertThat(instance).isInstanceOf(BeanMappingTestBean.class);

    BeanMappingTestBean bean = (BeanMappingTestBean) instance;

    bean.setAnotherNested(bean);

    assertThat(bean.getDoubleProperty()).isEqualTo(321.0);

    beanMetadata.setProperty(instance, "doubleProperty", 123.45);
    assertThat(bean.getDoubleProperty()).isEqualTo(123.45);

    beanMetadata.obtainBeanProperty("doubleProperty").setValue(instance, 321.0);
    assertThat(bean.getDoubleProperty()).isEqualTo(321.0);

    assertThatThrownBy(() -> {
      beanMetadata.obtainBeanProperty("1243");
    }).hasMessageStartingWith(String.format("Invalid property '1243' of bean class [%s]: Property not found", BeanMappingTestBean.class.getName()));

  }
}
