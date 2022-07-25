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

package cn.taketoday.transaction.testfixture.beans;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 17:17
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.parsing.AliasDefinition;
import cn.taketoday.beans.factory.parsing.ComponentDefinition;
import cn.taketoday.beans.factory.parsing.DefaultsDefinition;
import cn.taketoday.beans.factory.parsing.ImportDefinition;
import cn.taketoday.beans.factory.parsing.ReaderEventListener;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class CollectingReaderEventListener implements ReaderEventListener {

  private final List<DefaultsDefinition> defaults = new ArrayList<>();

  private final Map<String, ComponentDefinition> componentDefinitions = new LinkedHashMap<>(8);

  private final Map<String, List<AliasDefinition>> aliasMap = new LinkedHashMap<>(8);

  private final List<ImportDefinition> imports = new ArrayList<>();

  @Override
  public void defaultsRegistered(DefaultsDefinition defaultsDefinition) {
    this.defaults.add(defaultsDefinition);
  }

  public List<DefaultsDefinition> getDefaults() {
    return Collections.unmodifiableList(this.defaults);
  }

  @Override
  public void componentRegistered(ComponentDefinition componentDefinition) {
    this.componentDefinitions.put(componentDefinition.getName(), componentDefinition);
  }

  public ComponentDefinition getComponentDefinition(String name) {
    return this.componentDefinitions.get(name);
  }

  public ComponentDefinition[] getComponentDefinitions() {
    Collection<ComponentDefinition> collection = this.componentDefinitions.values();
    return collection.toArray(new ComponentDefinition[0]);
  }

  @Override
  public void aliasRegistered(AliasDefinition aliasDefinition) {
    List<AliasDefinition> aliases = this.aliasMap.computeIfAbsent(aliasDefinition.getBeanName(), k -> new ArrayList<>());
    aliases.add(aliasDefinition);
  }

  public List<AliasDefinition> getAliases(String beanName) {
    List<AliasDefinition> aliases = this.aliasMap.get(beanName);
    return (aliases != null ? Collections.unmodifiableList(aliases) : null);
  }

  @Override
  public void importProcessed(ImportDefinition importDefinition) {
    this.imports.add(importDefinition);
  }

  public List<ImportDefinition> getImports() {
    return Collections.unmodifiableList(this.imports);
  }

}
