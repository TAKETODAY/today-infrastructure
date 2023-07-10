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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import cn.taketoday.util.function.ThrowingConsumer;

/**
 * Class to work with the native-image argfile.
 *
 * @author Moritz Halbritter
 * @author Phil Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class NativeImageArgFile {

  /**
   * Location of the argfile.
   */
  public static final String LOCATION = "META-INF/native-image/argfile";

  private final List<String> excludes;

  /**
   * Constructs a new instance with the given excludes.
   *
   * @param excludes dependencies for which the reachability metadata should be excluded
   */
  public NativeImageArgFile(Collection<String> excludes) {
    this.excludes = List.copyOf(excludes);
  }

  /**
   * Write the arguments file if it is necessary.
   *
   * @param writer consumer that should write the contents
   */
  public void writeIfNecessary(ThrowingConsumer<List<String>> writer) {
    if (this.excludes.isEmpty()) {
      return;
    }
    List<String> lines = new ArrayList<>();
    for (String exclude : this.excludes) {
      int lastSlash = exclude.lastIndexOf('/');
      String jar = (lastSlash != -1) ? exclude.substring(lastSlash + 1) : exclude;
      lines.add("--exclude-config");
      lines.add(Pattern.quote(jar));
      lines.add("^/META-INF/native-image/.*");
    }
    writer.accept(lines);
  }

}
