/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.generate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link GeneratedFiles} implementation that keeps generated files in-memory.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class InMemoryGeneratedFiles implements GeneratedFiles {

  private final Map<Kind, Map<String, InputStreamSource>> files = new HashMap<>();

  @Override
  public void addFile(Kind kind, String path, InputStreamSource content) {
    Assert.notNull(kind, "'kind' is required");
    Assert.hasLength(path, "'path' must not be empty");
    Assert.notNull(content, "'content' is required");
    Map<String, InputStreamSource> paths = this.files.computeIfAbsent(kind,
            key -> new LinkedHashMap<>());
    Assert.state(!paths.containsKey(path), () -> "Path '" + path + "' already in use");
    paths.put(path, content);
  }

  /**
   * Return a {@link Map} of the generated files of a specific {@link Kind}.
   *
   * @param kind the kind of generated file
   * @return a {@link Map} of paths to {@link InputStreamSource} instances
   */
  public Map<String, InputStreamSource> getGeneratedFiles(Kind kind) {
    Assert.notNull(kind, "'kind' is required");
    return Collections.unmodifiableMap(this.files.getOrDefault(kind, Collections.emptyMap()));
  }

  /**
   * Return the content of the specified file.
   *
   * @param kind the kind of generated file
   * @param path the path of the file
   * @return the file content or {@code null} if no file could be found
   * @throws IOException on read error
   */
  @Nullable
  public String getGeneratedFileContent(Kind kind, String path) throws IOException {
    InputStreamSource source = getGeneratedFile(kind, path);
    if (source != null) {
      return new String(source.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
    return null;
  }

  /**
   * Return the {@link InputStreamSource} of specified file.
   *
   * @param kind the kind of generated file
   * @param path the path of the file
   * @return the file source or {@code null} if no file could be found
   */
  @Nullable
  public InputStreamSource getGeneratedFile(Kind kind, String path) {
    Assert.notNull(kind, "'kind' is required");
    Assert.hasLength(path, "'path' must not be empty");
    Map<String, InputStreamSource> paths = this.files.get(kind);
    return (paths != null) ? paths.get(path) : null;
  }

}
