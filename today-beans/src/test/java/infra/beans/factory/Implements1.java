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
package infra.beans.factory;

import infra.core.testfixture.stereotype.Component;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * @author Today <br>
 *
 * 2019-01-22 19:41
 */
@Component
public class Implements1 implements Interface {
  private static final Logger log = LoggerFactory.getLogger(Implements1.class);

  @Override
  public void test() {
    log.debug("Implements1");
  }

}
