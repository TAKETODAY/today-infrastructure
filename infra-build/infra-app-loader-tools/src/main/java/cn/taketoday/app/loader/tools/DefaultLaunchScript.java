/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Nullable;

/**
 * Default implementation of {@link LaunchScript}. Provides the default Infra launch
 * script or can load a specific script File. Also support mustache style template
 * expansion of the form <code>{{name:default}}</code>.
 *
 * @author Phillip Webb
 * @author Justin Rosenberg
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DefaultLaunchScript implements LaunchScript {

  private static final int BUFFER_SIZE = 4096;

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)(:.*?)?\\}\\}(?!\\})");

  private static final Set<String> FILE_PATH_KEYS = Collections.singleton("inlinedConfScript");

  private final String content;

  /**
   * Create a new {@link DefaultLaunchScript} instance.
   *
   * @param file the source script file or {@code null} to use the default
   * @param properties an optional set of script properties used for variable expansion
   * @throws IOException if the script cannot be loaded
   */
  public DefaultLaunchScript(@Nullable File file, @Nullable Map<?, ?> properties) throws IOException {
    String content = loadContent(file);
    this.content = expandPlaceholders(content, properties);
  }

  private String loadContent(@Nullable File file) throws IOException {
    if (file == null) {
      return loadContent(getClass().getResourceAsStream("launch.script"));
    }
    return loadContent(new FileInputStream(file));
  }

  private String loadContent(InputStream inputStream) throws IOException {
    try (inputStream) {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      copy(inputStream, outputStream);
      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }

  private void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.flush();
  }

  private String expandPlaceholders(String content, @Nullable Map<?, ?> properties) throws IOException {
    StringBuilder expanded = new StringBuilder();
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
    while (matcher.find()) {
      String name = matcher.group(1);
      final String value;
      String defaultValue = matcher.group(2);
      if (properties != null && properties.containsKey(name)) {
        Object propertyValue = properties.get(name);
        if (FILE_PATH_KEYS.contains(name)) {
          value = parseFilePropertyValue(propertyValue);
        }
        else {
          value = propertyValue.toString();
        }
      }
      else {
        value = (defaultValue != null) ? defaultValue.substring(1) : matcher.group(0);
      }
      matcher.appendReplacement(expanded, value.replace("$", "\\$"));
    }
    matcher.appendTail(expanded);
    return expanded.toString();
  }

  private String parseFilePropertyValue(Object propertyValue) throws IOException {
    if (propertyValue instanceof File file) {
      return loadContent(file);
    }
    return loadContent(new File(propertyValue.toString()));
  }

  @Override
  public byte[] toByteArray() {
    return this.content.getBytes(StandardCharsets.UTF_8);
  }

}
