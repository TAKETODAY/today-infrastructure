/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package test.demo.domain;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.annotation.Value;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Singleton
@SuppressWarnings("serial")
@Prototype("prototype_config")
public final class Config implements Serializable {

	private Integer id;

	@Value("#{site.cdn}")
	private String cdn;

	@Value("#{site.icp}")
	private String icp;

	@Value("#{site.host}")
	private String host;

	@Value("#{site.index}")
	private String index;

	@Value("#{site.upload}")
	private String upload;

	@Value("#{site.keywords}")
	private String keywords;

	@Value("#{site.name}")
	private String siteName;

	@Value("#{site.copyright}")
	private String copyright;

	@Value("#{site.server.path}")
	private String serverPath;

	@Value("#{site.description}")
	private String description;

	@Value("#{site.otherFooterInfo}")
	private String otherFooterInfo;

	@Autowired(required = false)
	User user;

	public Config() {
		
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{\n\t\"id\":\"").append(id).append("\",\n\t\"cdn\":\"").append(cdn).append("\",\n\t\"icp\":\"").append(icp).append(
				"\",\n\t\"host\":\"").append(host).append("\",\n\t\"index\":\"").append(index).append("\",\n\t\"upload\":\"").append(
						upload).append("\",\n\t\"keywords\":\"").append(keywords).append("\",\n\t\"siteName\":\"").append(siteName).append(
								"\",\n\t\"copyright\":\"").append(copyright).append("\",\n\t\"serverPath\":\"").append(serverPath).append(
										"\",\n\t\"description\":\"").append(description).append("\",\n\t\"otherFooterInfo\":\"").append(
												otherFooterInfo).append("\",\n\t\"user\":\"").append(user).append("\"\n}");
		return builder.toString();
	}

}
