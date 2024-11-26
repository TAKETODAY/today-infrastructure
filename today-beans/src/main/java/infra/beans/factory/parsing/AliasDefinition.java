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

package infra.beans.factory.parsing;

import infra.beans.BeanMetadataElement;
import infra.lang.Assert;
import infra.lang.Nullable;

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
