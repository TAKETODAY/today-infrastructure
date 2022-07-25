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

import java.util.ArrayList;

import cn.taketoday.http.codec.ClientCodecConfigurer;
import cn.taketoday.http.codec.HttpMessageWriter;

/**
 * Default implementation of {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultClientCodecConfigurer extends BaseCodecConfigurer implements ClientCodecConfigurer {

  public DefaultClientCodecConfigurer() {
    super(new ClientDefaultCodecsImpl());
    ((ClientDefaultCodecsImpl) defaultCodecs()).setPartWritersSupplier(this::getPartWriters);
  }

  private DefaultClientCodecConfigurer(DefaultClientCodecConfigurer other) {
    super(other);
    ((ClientDefaultCodecsImpl) defaultCodecs()).setPartWritersSupplier(this::getPartWriters);
  }

  @Override
  public ClientDefaultCodecs defaultCodecs() {
    return (ClientDefaultCodecs) super.defaultCodecs();
  }

  @Override
  public DefaultClientCodecConfigurer clone() {
    return new DefaultClientCodecConfigurer(this);
  }

  @Override
  protected BaseDefaultCodecs cloneDefaultCodecs() {
    return new ClientDefaultCodecsImpl((ClientDefaultCodecsImpl) defaultCodecs());
  }

  private ArrayList<HttpMessageWriter<?>> getPartWriters() {
    ArrayList<HttpMessageWriter<?>> result = new ArrayList<>();
    result.addAll(this.customCodecs.getTypedWriters().keySet());
    result.addAll(this.defaultCodecs.getBaseTypedWriters());
    result.addAll(this.customCodecs.getObjectWriters().keySet());
    result.addAll(this.defaultCodecs.getBaseObjectWriters());
    result.addAll(this.defaultCodecs.getCatchAllWriters());
    return result;
  }

}
