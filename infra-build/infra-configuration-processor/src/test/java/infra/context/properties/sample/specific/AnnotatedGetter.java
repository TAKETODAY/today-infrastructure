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

package infra.context.properties.sample.specific;

import infra.context.properties.sample.ConfigurationProperties;
import jakarta.validation.constraints.NotEmpty;

/**
 * An annotated getter with {@code NotEmpty} that triggers a different class type in the
 * compiler. See #11512
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties("specific")
public class AnnotatedGetter {

  private String name;

  @NotEmpty
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
