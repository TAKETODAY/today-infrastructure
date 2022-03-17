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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.BeanMetadataAttributeAccessor;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.lang.Assert;

/**
 * Qualifier for resolving autowire candidates. A bean definition that
 * includes one or more such qualifiers enables fine-grained matching
 * against annotations on a field or parameter to be autowired.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Qualifier
 * @since 4.0 2021/12/22 23:33
 */
@SuppressWarnings("serial")
public class AutowireCandidateQualifier extends BeanMetadataAttributeAccessor {

  /**
   * The name of the key used to store the value.
   */
  public static final String VALUE_KEY = "value";

  private final String typeName;

  /**
   * Construct a qualifier to match against an annotation of the
   * given type.
   *
   * @param type the annotation type
   */
  public AutowireCandidateQualifier(Class<?> type) {
    this(type.getName());
  }

  /**
   * Construct a qualifier to match against an annotation of the
   * given type name.
   * <p>The type name may match the fully-qualified class name of
   * the annotation or the short class name (without the package).
   *
   * @param typeName the name of the annotation type
   */
  public AutowireCandidateQualifier(String typeName) {
    Assert.notNull(typeName, "Type name must not be null");
    this.typeName = typeName;
  }

  /**
   * Construct a qualifier to match against an annotation of the
   * given type whose {@code value} attribute also matches
   * the specified value.
   *
   * @param type the annotation type
   * @param value the annotation value to match
   */
  public AutowireCandidateQualifier(Class<?> type, Object value) {
    this(type.getName(), value);
  }

  /**
   * Construct a qualifier to match against an annotation of the
   * given type name whose {@code value} attribute also matches
   * the specified value.
   * <p>The type name may match the fully-qualified class name of
   * the annotation or the short class name (without the package).
   *
   * @param typeName the name of the annotation type
   * @param value the annotation value to match
   */
  public AutowireCandidateQualifier(String typeName, Object value) {
    Assert.notNull(typeName, "Type name must not be null");
    this.typeName = typeName;
    setAttribute(VALUE_KEY, value);
  }

  /**
   * Retrieve the type name. This value will be the same as the
   * type name provided to the constructor or the fully-qualified
   * class name if a Class instance was provided to the constructor.
   */
  public String getTypeName() {
    return this.typeName;
  }

}

