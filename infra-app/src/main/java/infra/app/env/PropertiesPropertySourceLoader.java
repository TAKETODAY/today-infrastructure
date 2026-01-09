/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.env;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.core.env.PropertySource;
import infra.core.io.PropertiesUtils;
import infra.core.io.Resource;

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
      List<OriginTrackedPropertiesLoader.Document> documents = new OriginTrackedPropertiesLoader(resource).load();
      documents.forEach((document) -> result.add(document.asMap()));
    }
    return result;
  }

}
