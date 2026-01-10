/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.support;

import infra.beans.BeanMetadataAttributeAccessor;
import infra.beans.factory.annotation.Qualifier;
import infra.lang.Assert;

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
    Assert.notNull(typeName, "Type name is required");
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
    Assert.notNull(typeName, "Type name is required");
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

