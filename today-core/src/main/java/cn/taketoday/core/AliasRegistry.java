/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.core;

import java.util.List;

/**
 * Common interface for managing aliases. Serves as a super-interface for
 * {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/9/30 22:38
 * @since 4.0
 */
public interface AliasRegistry {

  /**
   * Given a name, register an alias for it.
   *
   * @param name the canonical name
   * @param alias the alias to be registered
   * @throws IllegalStateException if the alias is already in use
   * and may not be overridden
   */
  void registerAlias(String name, String alias);

  /**
   * Remove the specified alias from this registry.
   *
   * @param alias the alias to remove
   * @throws IllegalStateException if no such alias was found
   */
  void removeAlias(String alias);

  /**
   * Determine whether the given name is defined as an alias
   * (as opposed to the name of an actually registered component).
   *
   * @param name the name to check
   * @return whether the given name is an alias
   */
  boolean isAlias(String name);

  /**
   * Return the aliases for the given name, if defined.
   *
   * @param name the name to check for aliases
   * @return the aliases, or an empty array if none
   */
  String[] getAliases(String name);

  /**
   * Return the aliases list for the given name, if defined.
   *
   * @param name the name to check for aliases
   * @return the aliases, or an empty array-list if none
   */
  List<String> getAliasList(String name);

}
