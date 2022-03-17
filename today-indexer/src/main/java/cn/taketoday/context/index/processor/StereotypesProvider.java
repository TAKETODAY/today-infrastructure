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

import java.util.Set;

import javax.lang.model.element.Element;

/**
 * Provide the list of stereotypes that match an {@link Element}.
 * <p>If an element has one or more stereotypes, it is referenced in the index
 * of candidate components and each stereotype can be queried individually.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
interface StereotypesProvider {

  /**
   * Return the stereotypes that are present on the given {@link Element}.
   *
   * @param element the element to handle
   * @return the stereotypes or an empty set if none were found
   */
  Set<String> getStereotypes(Element element);

}
