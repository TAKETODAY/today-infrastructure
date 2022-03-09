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

package cn.taketoday.web.framework.server.light;

import java.io.File;
import java.io.IOException;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Singleton;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.POST;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.framework.WebApplication;
import cn.taketoday.web.multipart.MultipartConfiguration;
import cn.taketoday.web.multipart.MultipartFile;
import lombok.Data;
import test.framework.NettyApplication;

/**
 * @author TODAY 2021/4/13 19:42
 */
@Controller
@EnableLightHttpHandling
@Import({ NettyApplication.class, LightWebApplication.AppConfig.class })
public class LightWebApplication {

  public static void main(String[] args) {
    WebApplication.runReactive(LightWebApplication.class);
  }

  @ActionMapping(value = { "/", "/index", "/index.html" }, method = { HttpMethod.GET, HttpMethod.POST })
  public String index(RequestContext request, @RequestParam String arr) {

    String userId = request.getParameter("userId");
    String userName = request.getParameter("userName");
    request.setAttribute("q", arr);
    request.setAttribute("userId", userId);
    request.setAttribute("userName", userName);
    request.setAttribute("url", request.getRequestURL());

    return "index/index.ftl";
  }

  @POST("/upload")
  public UploadResult upload(MultipartFile file, String other) throws IOException {
    final String fileName = file.getFileName();
    final long size = file.getSize();
    final String content = new String(file.getBytes());
    final String name = file.getName();

    final File file1 = new File("D:/dev/temp/upload", fileName);
    file.save(file1);

    return new UploadResult(fileName, size, content, name, other);
  }

  @Data
  static class UploadResult {
    final String fileName;
    final long size;
    final String content;
    final String name;
    final String other;

    UploadResult(String fileName, long size, String content, String name, String other) {
      this.fileName = fileName;
      this.size = size;
      this.content = content;
      this.name = name;
      this.other = other;
    }
  }

  @Configuration
  static class AppConfig {

    @Singleton
    LightHttpConfig lightHttpConfig(MultipartConfiguration multipartConfig) {
      final LightHttpConfig lightHttpConfig = LightHttpConfig.defaultConfig();
      lightHttpConfig.setMultipartConfig(multipartConfig);
      multipartConfig.setLocation("D:/dev/temp/upload/");
      return lightHttpConfig;
    }

  }

}
