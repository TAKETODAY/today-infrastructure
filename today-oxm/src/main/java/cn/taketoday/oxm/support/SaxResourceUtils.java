/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.oxm.support;

import org.xml.sax.InputSource;

import java.io.IOException;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;

/**
 * Convenient utility methods for dealing with SAX.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class SaxResourceUtils {

  /**
   * Create a SAX {@code InputSource} from the given resource.
   * <p>Sets the system identifier to the resource's {@code URL}, if available.
   *
   * @param resource the resource
   * @return the input source created from the resource
   * @throws IOException if an I/O exception occurs
   * @see InputSource#setSystemId(String)
   * @see #getSystemId(cn.taketoday.core.io.Resource)
   */
  public static InputSource createInputSource(Resource resource) throws IOException {
    InputSource inputSource = new InputSource(resource.getInputStream());
    inputSource.setSystemId(getSystemId(resource));
    return inputSource;
  }

  /**
   * Retrieve the URL from the given resource as System ID.
   * <p>Returns {@code null} if it cannot be opened.
   */
  @Nullable
  private static String getSystemId(Resource resource) {
    try {
      return resource.getURI().toString();
    }
    catch (IOException ex) {
      return null;
    }
  }

}
