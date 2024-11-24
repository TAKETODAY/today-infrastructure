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

package infra.app.context.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.io.Resource;
import infra.app.env.PropertySourceLoader;

/**
 * {@link PropertySourceLoader} for tests.
 *
 * @author Madhura Bhave
 */
class TestPropertySourceLoader2 implements PropertySourceLoader {

  @Override
  public String[] getFileExtensions() {
    return new String[] { "custom" };
  }

  @Override
  public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
    Map<String, Object> map = Collections.singletonMap("customloader2", "true");
    return Collections.singletonList(new MapPropertySource(name, map));
  }

}
