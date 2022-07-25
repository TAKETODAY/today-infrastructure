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

package cn.taketoday.web.bind.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.annotation.SessionAttributes;

/**
 * Strategy interface for storing model attributes in a backend session.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionAttributes
 * @since 4.0 2022/4/8 23:19
 */
public interface SessionAttributeStore {

  /**
   * Store the supplied attribute in the backend session.
   * <p>Can be called for new attributes as well as for existing attributes.
   * In the latter case, this signals that the attribute value may have been modified.
   *
   * @param request the current request
   * @param attributeName the name of the attribute
   * @param attributeValue the attribute value to store
   */
  void storeAttribute(RequestContext request, String attributeName, Object attributeValue);

  /**
   * Retrieve the specified attribute from the backend session.
   * <p>This will typically be called with the expectation that the
   * attribute is already present, with an exception to be thrown
   * if this method returns {@code null}.
   *
   * @param request the current request
   * @param attributeName the name of the attribute
   * @return the current attribute value, or {@code null} if none
   */
  @Nullable
  Object retrieveAttribute(RequestContext request, String attributeName);

  /**
   * Clean up the specified attribute in the backend session.
   * <p>Indicates that the attribute name will not be used anymore.
   *
   * @param request the current request
   * @param attributeName the name of the attribute
   */
  void cleanupAttribute(RequestContext request, String attributeName);

}
