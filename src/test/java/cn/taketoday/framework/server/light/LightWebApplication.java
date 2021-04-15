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

import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.ConfigurableWebServerApplicationContext;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.RequestParam;
import test.framework.NettyApplication;

/**
 * @author TODAY 2021/4/13 19:42
 */
@Import(NettyApplication.class)
@Controller
@EnableLightHttpHandling
public class LightWebApplication {

  public static void main(String[] args) {
    final ConfigurableWebServerApplicationContext context
            = WebApplication.runReactive(LightWebApplication.class);
    System.out.println(context);
  }

  @ActionMapping(value = { "/", "/index", "/index.html" }, method = { RequestMethod.GET, RequestMethod.POST })
  public String index(RequestContext request, @RequestParam String arr) {

    String userId = request.getParameter("userId");
    String userName = request.getParameter("userName");
    request.setAttribute("q", arr);
    request.setAttribute("userId", userId);
    request.setAttribute("userName", userName);
    request.setAttribute("url", request.getRequestURL());

    return "index/index.ftl";
  }


}
