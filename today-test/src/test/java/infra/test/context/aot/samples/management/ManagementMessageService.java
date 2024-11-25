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

package infra.test.context.aot.samples.management;

import infra.test.context.aot.samples.common.MessageService;

/**
 * {@link MessageService} implemented in a different package.
 *
 * <p>See <a href="https://github.com/spring-projects/spring-framework/issues/30861">gh-30861</a>.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class ManagementMessageService implements MessageService {

  @Override
  public String generateMessage() {
    return "Hello, Management!";
  }

}