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

package infra.app.env;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.env.PropertySource;
import infra.core.io.Resource;
import infra.util.ClassUtils;

/**
 * Strategy to load '.yml' (or '.yaml') files into a {@link PropertySource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0
 */
public class YamlPropertySourceLoader implements PropertySourceLoader {

  @Override
  public String[] getFileExtensions() {
    return new String[] { "yml", "yaml" };
  }

  @Override
  public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
    if (!ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", getClass().getClassLoader())) {
      throw new IllegalStateException(
              "Attempted to load " + name + " but snakeyaml was not found on the classpath");
    }
    List<Map<String, Object>> loaded = new OriginTrackedYamlLoader(resource).load();
    if (loaded.isEmpty()) {
      return Collections.emptyList();
    }
    List<PropertySource<?>> propertySources = new ArrayList<>(loaded.size());
    for (int i = 0; i < loaded.size(); i++) {
      String documentNumber = (loaded.size() != 1) ? " (document #" + i + ")" : "";
      propertySources.add(new OriginTrackedMapPropertySource(name + documentNumber,
              Collections.unmodifiableMap(loaded.get(i)), true));
    }
    return propertySources;
  }

}
