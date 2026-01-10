/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory;

import org.junit.jupiter.api.Test;

import infra.beans.BeanMetadata;

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
