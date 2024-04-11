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

package cn.taketoday.persistence;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.BeanMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/15 13:43
 */
class IdPropertyDiscoverTests {

  @Test
  void composite() {
    IdPropertyDiscover propertyDiscover = IdPropertyDiscover.composite(
            IdPropertyDiscover.forPropertyName("myId"),
            IdPropertyDiscover.forPropertyName("myId_"),
            IdPropertyDiscover.forAnnotation(MyId.class));
    BeanMetadata metadata = BeanMetadata.from(MyIdEntity.class);
    propertyDiscover.isIdProperty(metadata.obtainBeanProperty("id"));
    assertThat(propertyDiscover.isIdProperty(metadata.obtainBeanProperty("id"))).isFalse();
    assertThat(propertyDiscover.isIdProperty(metadata.obtainBeanProperty("myId"))).isTrue();

    assertThat(propertyDiscover.isIdProperty(BeanMetadata.from(MyIdAnnoEntity.class).obtainBeanProperty("id"))).isTrue();

  }

  static class MyIdEntity {

    Long id;

    Long myId;
  }

  static class MyIdAnnoEntity {

    @MyId
    Long id;

    Long myId1;
  }

  @Target({ ElementType.FIELD, ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface MyId {

  }

}