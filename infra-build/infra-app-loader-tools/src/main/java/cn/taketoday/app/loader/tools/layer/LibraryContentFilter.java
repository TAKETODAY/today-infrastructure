/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.tools.layer;

import java.util.regex.Pattern;

import cn.taketoday.app.loader.tools.Library;
import cn.taketoday.app.loader.tools.LibraryCoordinates;
import cn.taketoday.lang.Assert;

/**
 * {@link ContentFilter} that matches {@link Library} items based on a coordinates
 * pattern.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LibraryContentFilter implements ContentFilter<Library> {

  private final Pattern pattern;

  public LibraryContentFilter(String coordinatesPattern) {
    Assert.hasText(coordinatesPattern, "CoordinatesPattern must not be empty");
    StringBuilder regex = new StringBuilder();
    for (int i = 0; i < coordinatesPattern.length(); i++) {
      char c = coordinatesPattern.charAt(i);
      if (c == '.') {
        regex.append("\\.");
      }
      else if (c == '*') {
        regex.append(".*");
      }
      else {
        regex.append(c);
      }
    }
    this.pattern = Pattern.compile(regex.toString());
  }

  @Override
  public boolean matches(Library library) {
    return this.pattern.matcher(LibraryCoordinates.toStandardNotationString(library.getCoordinates())).matches();
  }

}
