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

package cn.taketoday.format.support;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Locale;

import cn.taketoday.format.Formatter;

/**
 * {@link Formatter} for {@link InetAddress}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class InetAddressFormatter implements Formatter<InetAddress> {

  @Override
  public String print(InetAddress object, Locale locale) {
    return object.getHostAddress();
  }

  @Override
  public InetAddress parse(String text, Locale locale) throws ParseException {
    try {
      return InetAddress.getByName(text);
    }
    catch (UnknownHostException ex) {
      throw new IllegalStateException("Unknown host " + text, ex);
    }
  }

}
