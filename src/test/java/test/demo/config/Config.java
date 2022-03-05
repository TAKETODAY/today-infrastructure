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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.demo.config;

import java.io.File;
import java.io.Serializable;

import jakarta.annotation.PostConstruct;

import cn.taketoday.context.properties.Props;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.lang.Prototype;
import cn.taketoday.lang.Singleton;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Singleton
@SuppressWarnings("serial")
@Prototype("prototype_config")
public final class Config implements Serializable {

  private Integer id;

  @Value("${site.cdn}")
  private String cdn;

  @Value("${site.icp}")
  private String icp;

  @Value("${site.host}")
  private String host;

  @Value("${site.index}")
  private File index;

  @Value("${site.upload}")
  private File upload;

  @Value("${site.keywords}")
  private String keywords;

  @Value("${site.name}")
  private String siteName;

  @Value("${site.copyright}")
  private String copyright;

  @Value("${site.server.path}")
  private File serverPath;

  @Value("${site.description}")
  private String description;

  @Value("${site.otherFooterInfo}")
  private String otherFooterInfo;

  @Autowired(required = false)
  User user;

  @Props(prefix = "site.admin")
  User admin;

  //	@Value(value = "#{user}", required = false)
//	User user_;
//
  @PostConstruct
  public void init() {
    System.err.println("admin: " + admin);
  }

  public Config() {

  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{\n\t\"id\":\"");
    builder.append(id);
    builder.append("\", \n\t\"cdn\":\"");
    builder.append(cdn);
    builder.append("\", \n\t\"icp\":\"");
    builder.append(icp);
    builder.append("\", \n\t\"host\":\"");
    builder.append(host);
    builder.append("\", \n\t\"index\":\"");
    builder.append(index);
    builder.append("\", \n\t\"upload\":\"");
    builder.append(upload);
    builder.append("\", \n\t\"keywords\":\"");
    builder.append(keywords);
    builder.append("\", \n\t\"siteName\":\"");
    builder.append(siteName);
    builder.append("\", \n\t\"copyright\":\"");
    builder.append(copyright);
    builder.append("\", \n\t\"serverPath\":\"");
    builder.append(serverPath);
    builder.append("\", \n\t\"description\":\"");
    builder.append(description);
    builder.append("\", \n\t\"otherFooterInfo\":\"");
    builder.append(otherFooterInfo);
    builder.append("\", \n\t\"user\":\"");
    builder.append(user);
    builder.append("\", \n\t\"admin\":\"");
    builder.append(admin);
    builder.append("\"\n}");
    return builder.toString();
  }

}
