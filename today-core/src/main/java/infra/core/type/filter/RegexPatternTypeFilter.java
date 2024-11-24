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

package infra.core.type.filter;

import java.util.regex.Pattern;

import infra.core.type.ClassMetadata;
import infra.lang.Assert;

/**
 * A simple filter for matching a fully-qualified class name with a regex {@link Pattern}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 4.0
 */
public class RegexPatternTypeFilter extends AbstractClassTestingTypeFilter {

  private final Pattern pattern;

  public RegexPatternTypeFilter(Pattern pattern) {
    Assert.notNull(pattern, "Pattern is required");
    this.pattern = pattern;
  }

  @Override
  protected boolean match(ClassMetadata metadata) {
    return this.pattern.matcher(metadata.getClassName()).matches();
  }

}
