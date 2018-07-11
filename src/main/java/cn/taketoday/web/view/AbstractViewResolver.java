/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.view;

import java.util.Locale;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;


/**
 * @author Today
 * @date 2018年6月26日 上午11:58:24
 */
@Setter
@Getter
public abstract class AbstractViewResolver implements ViewResolver {
	
	protected final Logger log	= LoggerFactory.getLogger(ViewResolver.class);
	
	protected String	prefix		= "/WEB-INF/view";
	protected String	suffix		= ".jsp";
	protected String	encoding	= "UTF-8";
	protected Locale	locale		= Locale.CHINA;
	
	ServletContext servletContext 	= null;

}
