/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package test.demo;

import java.net.HttpCookie;

import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.exception.UnauthorizedException;

/**
 * @author TODAY <br>
 *         2019-07-02 21:17
 */
@RestController
@ComponentScan({ "cn.taketoday.framework", "test.demo" })
public class NettyApplication {

    public static void main(String[] args) {
        WebApplication.run(NettyApplication.class, args);
    }

    @GET("index")
    public String index() {
        return "Hello Netty";
    }

    @GET("ex")
    void ex() {
        throw new UnauthorizedException("您没登录");
    }

    @GET("json")
    public Json json() {
        return Json.ok("Hello Netty");
    }

    @GET("cookie")
    public void cookie(RequestContext context) {
        final HttpCookie test = new HttpCookie("test", "HelloNetty");
        test.setMaxAge(1000);
        System.err.println(test.toString());
        context.addCookie(test);
    }

    @GET("/cookie/delete")
    public void delete(@RequestParam(required = true) HttpCookie test, RequestContext context) {
        if (test != null) {
            test.setMaxAge(-1);
            System.err.println(test.toString());
            context.addCookie(test);
        }
    }

}
