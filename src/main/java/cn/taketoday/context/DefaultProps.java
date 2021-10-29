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
package cn.taketoday.context;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

import cn.taketoday.context.annotation.Props;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Constant;

/**
 * @author TODAY 2019-03-15 23:18
 */
@SuppressWarnings("all")
public class DefaultProps implements Props, Annotation {

  private boolean replace = false;
  private String[] value = Constant.EMPTY_STRING_ARRAY;
  private String[] prefix = Constant.EMPTY_STRING_ARRAY;
  private Class<?>[] nested = Constant.EMPTY_CLASS_ARRAY;

  public DefaultProps() { }

  public DefaultProps(Props props) {
    this.value = props.value();
    this.nested = props.nested();
    this.prefix = props.prefix();
    this.replace = props.replace();
  }

  public DefaultProps(AnnotationAttributes props) {
    this.replace = props.getBoolean("replace");
    this.nested = props.getClassArray("nested");
    this.prefix = props.getStringArray("prefix");
    this.value = props.getStringArray(MergedAnnotation.VALUE);
  }

  /**
   * @param props
   * @throws NoSuchElementException if there is no matching attribute
   */
  public DefaultProps(MergedAnnotation<Props> props) {
    this.replace = props.getBoolean("replace");
    this.nested = props.getClassArray("nested");
    this.prefix = props.getStringArray("prefix");
    this.value = props.getStringArray(MergedAnnotation.VALUE);
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return Props.class;
  }

  @Override
  public String[] value() {
    return value;
  }

  @Override
  public String[] prefix() {
    return prefix;
  }

  @Override
  public boolean replace() {
    return replace;
  }

  @Override
  public Class<?>[] nested() {
    return nested;
  }

  public DefaultProps setValue(String... value) {
    this.value = value;
    return this;
  }

  public DefaultProps setReplace(boolean replace) {
    this.replace = replace;
    return this;
  }

  public DefaultProps setPrefix(String... prefix) {
    this.prefix = prefix;
    return this;
  }

  public DefaultProps setNested(Class<?>... nested) {
    this.nested = nested;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Props))
      return false;
    final Props that = (Props) o;
    return replace == that.replace()
            && Arrays.equals(value, that.value())
            && Arrays.equals(prefix, that.prefix())
            && Arrays.equals(nested, that.nested());
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(replace);
    result = 31 * result + Arrays.hashCode(value);
    result = 31 * result + Arrays.hashCode(prefix);
    result = 31 * result + Arrays.hashCode(nested);
    return result;
  }
}
