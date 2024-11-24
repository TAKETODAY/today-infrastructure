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

package infra.app.test.context;

import java.io.Serial;

import infra.test.context.MergedContextConfiguration;

/**
 * Encapsulates the <em>merged</em> context configuration declared on a test class and all
 * of its superclasses for a reactive web application.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactiveWebMergedContextConfiguration extends MergedContextConfiguration {

  @Serial
  private static final long serialVersionUID = 1L;

  public ReactiveWebMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
    super(mergedConfig);
  }

}
