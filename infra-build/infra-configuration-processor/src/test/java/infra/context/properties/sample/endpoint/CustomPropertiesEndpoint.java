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

package infra.context.properties.sample.endpoint;

import infra.context.properties.sample.ConfigurationProperties;
import infra.context.properties.sample.Endpoint;
import infra.context.properties.sample.ReadOperation;

/**
 * An endpoint with additional custom properties.
 *
 * @author Stephane Nicoll
 */
@Endpoint(id = "customprops")
@ConfigurationProperties("management.endpoint.customprops")
public class CustomPropertiesEndpoint {

  private String name = "test";

  @ReadOperation
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
