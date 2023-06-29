/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.mail.javamail;

import java.beans.PropertyEditorSupport;

import cn.taketoday.util.StringUtils;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * Editor for {@code java.mail.internet.InternetAddress},
 * to directly populate an InternetAddress property.
 *
 * <p>Expects the same syntax as InternetAddress's constructor with
 * a String argument. Converts empty Strings into null values.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InternetAddress
 * @since 4.0
 */
public class InternetAddressEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    if (StringUtils.hasText(text)) {
      try {
        setValue(new InternetAddress(text));
      }
      catch (AddressException ex) {
        throw new IllegalArgumentException("Could not parse mail address: " + ex.getMessage());
      }
    }
    else {
      setValue(null);
    }
  }

  @Override
  public String getAsText() {
    InternetAddress value = (InternetAddress) getValue();
    return (value != null ? value.toUnicodeString() : "");
  }

}
