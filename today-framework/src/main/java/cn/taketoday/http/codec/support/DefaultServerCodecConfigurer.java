/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.codec.support;

import cn.taketoday.http.codec.ServerCodecConfigurer;

/**
 * Default implementation of {@link ServerCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultServerCodecConfigurer extends BaseCodecConfigurer implements ServerCodecConfigurer {

  public DefaultServerCodecConfigurer() {
    super(new ServerDefaultCodecsImpl());
  }

  private DefaultServerCodecConfigurer(BaseCodecConfigurer other) {
    super(other);
  }

  @Override
  public ServerDefaultCodecs defaultCodecs() {
    return (ServerDefaultCodecs) super.defaultCodecs();
  }

  @Override
  public DefaultServerCodecConfigurer clone() {
    return new DefaultServerCodecConfigurer(this);
  }

  @Override
  protected BaseDefaultCodecs cloneDefaultCodecs() {
    return new ServerDefaultCodecsImpl((ServerDefaultCodecsImpl) defaultCodecs());
  }
}
