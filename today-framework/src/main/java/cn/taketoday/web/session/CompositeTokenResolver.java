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

package cn.taketoday.web.session;

import java.util.List;

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY 2021/3/20 11:20
 * @since 3.0
 */
public class CompositeTokenResolver implements TokenResolver {

  final List<TokenResolver> resolvers;

  public CompositeTokenResolver(List<TokenResolver> resolvers) {
    this.resolvers = resolvers;
  }

  @Override
  public String getToken(RequestContext context) {
    for (final TokenResolver resolver : resolvers) {
      final String token = resolver.getToken(context);
      if (token != null) {
        return token;
      }
    }
    return null;
  }

  @Override
  public void saveToken(RequestContext context, WebSession session) {
    for (final TokenResolver resolver : resolvers) {
      resolver.saveToken(context, session);
    }
  }
}
