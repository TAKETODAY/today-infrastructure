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

package cn.taketoday.framework.env;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.env.OriginTrackedPropertiesLoader.Document;

/**
 * Strategy to load '.properties' files into a {@link PropertySource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class PropertiesPropertySourceLoader implements PropertySourceLoader {

  private static final String XML_FILE_EXTENSION = ".xml";

  @Override
  public String[] getFileExtensions() {
    return new String[] { "properties", "xml" };
  }

  @Override
  public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
    List<Map<String, ?>> properties = loadProperties(resource);
    if (properties.isEmpty()) {
      return Collections.emptyList();
    }
    List<PropertySource<?>> propertySources = new ArrayList<>(properties.size());
    for (int i = 0; i < properties.size(); i++) {
      String documentNumber = (properties.size() != 1) ? " (document #" + i + ")" : "";
      propertySources.add(new OriginTrackedMapPropertySource(name + documentNumber,
              Collections.unmodifiableMap(properties.get(i)), true));
    }
    return propertySources;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private List<Map<String, ?>> loadProperties(Resource resource) throws IOException {
    String filename = resource.getName();
    List<Map<String, ?>> result = new ArrayList<>();
    if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
      result.add((Map) PropertiesUtils.loadProperties(resource));
    }
    else {
      List<Document> documents = new OriginTrackedPropertiesLoader(resource).load();
      documents.forEach((document) -> result.add(document.asMap()));
    }
    return result;
  }

}
