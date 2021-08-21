/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view.template;

import cn.taketoday.web.RequestContext;

/**
 * @author TODAY <br>
 * 2018-06-23 11:59:50
 */
@FunctionalInterface
public interface TemplateViewResolver {
  String DEFAULT_TEMPLATE_PATH = "classpath:templates/";

  /**
   * Resolve template view.
   *
   * @param templateName
   *         template name
   * @param context
   *         current request context
   *
   * @throws Throwable
   *         If any {@link Exception} occurred
   */
  void resolveView(String templateName, RequestContext context) throws Throwable;

}
