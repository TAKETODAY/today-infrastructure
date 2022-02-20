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
package cn.taketoday.web.cors;

import java.io.IOException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * A strategy that takes a request and a {@link CorsConfiguration} and updates
 * the response.
 *
 * <p>
 * This component is not concerned with how a {@code CorsConfiguration} is
 * selected but rather takes follow-up actions such as applying CORS validation
 * checks and either rejecting the response or adding CORS headers to the
 * response.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author TODAY
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @since 2019-12-23 21:01
 */
public interface CorsProcessor {

  /**
   * Process a request given a {@code CorsConfiguration}.
   *
   * @param configuration the applicable CORS configuration (possibly {@code null})
   * @param context the current HTTP context
   * @return {@code false} if the request is rejected, {@code true} otherwise
   */
  boolean process(@Nullable CorsConfiguration configuration, RequestContext context) throws IOException;

}
