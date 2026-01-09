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

package infra.app.loader.tools.layer;

import java.util.regex.Pattern;

import infra.app.loader.tools.Library;
import infra.app.loader.tools.LibraryCoordinates;
import infra.lang.Assert;

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
