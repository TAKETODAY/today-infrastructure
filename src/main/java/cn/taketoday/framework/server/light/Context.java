/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.framework.server.light;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code Context} annotation decorates methods which are mapped
 * to a context (path) within the server, and provide its contents.
 * <p>
 * The annotated methods must have the same signature and contract
 * as {@link HTTPServer.ContextHandler#serve}, but can have arbitrary names.
 *
 * @author TODAY 2021/4/13 10:48
 * @see VirtualHost#addContexts(Object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Context {

  /**
   * The context (path) that this field maps to (must begin with '/').
   *
   * @return the context (path) that this field maps to
   */
  String value();

  /**
   * The HTTP methods supported by this context handler (default is "GET").
   *
   * @return the HTTP methods supported by this context handler
   */
  String[] methods() default "GET";
}
