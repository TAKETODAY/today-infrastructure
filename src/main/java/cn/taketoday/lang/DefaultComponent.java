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
package cn.taketoday.lang;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author TODAY <br>
 * 2018-08-22 17:29
 */
@SuppressWarnings("all")
public final class DefaultComponent implements Component {

  private String[] value = Constant.EMPTY_STRING_ARRAY;
  private String[] initMethods = Constant.EMPTY_STRING_ARRAY;
  private String destroyMethod ;

  @Override
  public Class<? extends Annotation> annotationType() {
    return Component.class;
  }

  @Override
  public String[] value() {
    return value;
  }

  @Override
  public String[] initMethods() {
    return initMethods;
  }

  @Override
  public String destroyMethod() {
    return destroyMethod;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, initMethods, destroyMethod);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Component))
      return false;
    final Component that = (Component) o;
    return Arrays.equals(value, that.value())
            && Arrays.equals(initMethods, that.initMethods())
            && Objects.equals(destroyMethod, that.destroyMethod());
  }

  @Override
  public String toString() {
    return new StringBuilder()//
            .append("@")//
            .append(Component.class.getName())//
            .append("(value=")//
            .append(Arrays.toString(value))//
            .append(", initMethods=")//
            .append(Arrays.toString(initMethods))//
            .append(", destroyMethods=")//
            .append(destroyMethod)//
            .append(")")//
            .toString();
  }

}
