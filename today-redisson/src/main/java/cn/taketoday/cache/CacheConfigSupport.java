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

package cn.taketoday.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

/**
 * @author Nikita Koksharov
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 21:33
 */
public class CacheConfigSupport {

  ObjectMapper jsonMapper = new ObjectMapper();
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public Map<String, CacheConfig> fromJSON(String content) throws IOException {
    return jsonMapper.readValue(content, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromJSON(File file) throws IOException {
    return jsonMapper.readValue(file, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromJSON(URL url) throws IOException {
    return jsonMapper.readValue(url, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromJSON(Reader reader) throws IOException {
    return jsonMapper.readValue(reader, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromJSON(InputStream inputStream) throws IOException {
    return jsonMapper.readValue(inputStream, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public String toJSON(Map<String, ? extends CacheConfig> configs) throws IOException {
    return jsonMapper.writeValueAsString(configs);
  }

  public Map<String, CacheConfig> fromYAML(String content) throws IOException {
    return yamlMapper.readValue(content, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromYAML(File file) throws IOException {
    return yamlMapper.readValue(file, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromYAML(URL url) throws IOException {
    return yamlMapper.readValue(url, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromYAML(Reader reader) throws IOException {
    return yamlMapper.readValue(reader, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public Map<String, CacheConfig> fromYAML(InputStream inputStream) throws IOException {
    return yamlMapper.readValue(inputStream, new TypeReference<Map<String, CacheConfig>>() { });
  }

  public String toYAML(Map<String, ? extends CacheConfig> configs) throws IOException {
    return yamlMapper.writeValueAsString(configs);
  }

}
