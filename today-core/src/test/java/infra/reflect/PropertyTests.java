/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.reflect;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.core.annotation.AnnotationFilter;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.RepeatableContainers;
import infra.lang.Required;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/3 19:37
 */
class PropertyTests {

  @Test
  void annotations() throws Exception {
    Method getInt = Bean.class.getDeclaredMethod("getInt");
    Method setInt = Bean.class.getDeclaredMethod("setInt", Integer.class);
    Property property = new Property(Bean.class, getInt, setInt);
    MergedAnnotations annotations = MergedAnnotations.from(property, property.getAnnotations(),
            RepeatableContainers.standard(), AnnotationFilter.JAVA);

    assertThat(annotations.get(Nullable.class).isPresent()).isTrue();
    assertThat(annotations.get(Override.class).isPresent()).isFalse();
    assertThat(annotations.get(Required.class).isPresent()).isTrue();
  }

  static class Bean implements Ifc {

    @Override
    public Integer getInt() {
      return 0;
    }

    @Required
    @Override
    public void setInt(Integer val) {

    }

  }

  interface Ifc {

    @Nullable
    Integer getInt();

    void setInt(Integer val);

  }

}