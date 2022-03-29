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

package cn.taketoday.framework.json;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.lang.Assert;

/**
 * Thin wrapper to adapt Snake {@link Yaml} to {@link JsonParser}.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @see JsonParser#lookup()
 * @since 4.0
 */
public class YamlJsonParser extends AbstractJsonParser {

  private final Yaml yaml = new Yaml(new TypeLimitedConstructor());

  @Override
  @SuppressWarnings("unchecked")
  public Map<String, Object> parseMap(String json) {
    return parseMap(json, (trimmed) -> this.yaml.loadAs(trimmed, Map.class));
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Object> parseList(String json) {
    return parseList(json, (trimmed) -> this.yaml.loadAs(trimmed, List.class));
  }

  private static class TypeLimitedConstructor extends Constructor {

    private static final Set<String> SUPPORTED_TYPES;

    static {
      Set<Class<?>> supportedTypes = new LinkedHashSet<>();
      supportedTypes.add(List.class);
      supportedTypes.add(Map.class);
      SUPPORTED_TYPES = supportedTypes.stream().map(Class::getName)
              .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
    }

    @Override
    protected Class<?> getClassForName(String name) throws ClassNotFoundException {
      Assert.state(SUPPORTED_TYPES.contains(name),
              () -> "Unsupported '" + name + "' type encountered in YAML document");
      return super.getClassForName(name);
    }

  }

}
