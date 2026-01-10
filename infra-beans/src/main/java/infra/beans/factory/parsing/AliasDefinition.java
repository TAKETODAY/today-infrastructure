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

package infra.beans.factory.parsing;

import org.jspecify.annotations.Nullable;

import infra.beans.BeanMetadataElement;
import infra.lang.Assert;

/**
 * Representation of an alias that has been registered during the parsing process.
 *
 * @author Juergen Hoeller
 * @see ReaderEventListener#aliasRegistered(AliasDefinition)
 * @since 4.0
 */
public class AliasDefinition implements BeanMetadataElement {

  private final String beanName;

  private final String alias;

  @Nullable
  private final Object source;

  /**
   * Create a new AliasDefinition.
   *
   * @param beanName the canonical name of the bean
   * @param alias the alias registered for the bean
   */
  public AliasDefinition(String beanName, String alias) {
    this(beanName, alias, null);
  }

  /**
   * Create a new AliasDefinition.
   *
   * @param beanName the canonical name of the bean
   * @param alias the alias registered for the bean
   * @param source the source object (may be {@code null})
   */
  public AliasDefinition(String beanName, String alias, @Nullable Object source) {
    Assert.notNull(beanName, "Bean name is required");
    Assert.notNull(alias, "Alias is required");
    this.beanName = beanName;
    this.alias = alias;
    this.source = source;
  }

  /**
   * Return the canonical name of the bean.
   */
  public final String getBeanName() {
    return this.beanName;
  }

  /**
   * Return the alias registered for the bean.
   */
  public final String getAlias() {
    return this.alias;
  }

  @Override
  @Nullable
  public final Object getSource() {
    return this.source;
  }

}
