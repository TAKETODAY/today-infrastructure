/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.context.index.processor;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents one entry in the index. The type defines the identify of the target
 * candidate (usually fully qualified name) and the stereotypes are "markers" that can
 * be used to retrieve the candidates. A typical use case is the presence of a given
 * annotation on the candidate.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class ItemMetadata {

  private final String type;

  private final Set<String> stereotypes;

  public ItemMetadata(String type, Set<String> stereotypes) {
    this.type = type;
    this.stereotypes = new HashSet<>(stereotypes);
  }

  public String getType() {
    return this.type;
  }

  public Set<String> getStereotypes() {
    return this.stereotypes;
  }

}
