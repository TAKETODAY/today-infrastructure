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
package cn.taketoday.context;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 
 * @author Today <br>
 *         2018-01-16 10:56
 */
public interface Constant extends Serializable {

	//@off
	String	KEY_USE_SIMPLE_NAME		= "context.name.simple";
	String 	KEY_ACTIVE_PROFILES 	= "context.active.profiles";
	
	String 	DEFAULT_ENCODING 		= "UTF-8";
	Charset DEFAULT_CHARSET 		= StandardCharsets.UTF_8;

	String 	GET_BEAN 				= "getBean";
	String 	ON_APPLICATION_EVENT 	= "onApplicationEvent";
	String 	PROPERTIES_SUFFIX 		= ".properties";
	String 	PLACE_HOLDER_PREFIX 	= "#{";
	String 	PLACE_HOLDER_SUFFIX 	= "}";
	String 	SPLIT_REGEXP 			= "[;|,]";

	String	BLANK					= "";
	String	CLASS_FILE_SUFFIX		= ".class";
	String 	SCOPE 					= "scope";
	String 	VALUE 					= "value";
	String 	EQUALS 					= "equals";
	String 	HASH_CODE 				= "hashCode";
	String 	TO_STRING 				= "toString";
	String 	ANNOTATION_TYPE 		= "annotationType";
	String	DEFAULT					= "default";
	
	/**********************************************
	 * @since 2.1.2
	 */
	String 	FREAMWORK_PACKAGE		= "cn.taketoday";
//	String 	COMPONENT_SCAN			= "component-scan";
	String 	CLASS_PATH_PREFIX 		= "classpath:";
	String	INIT_METHODS			= "initMethods";
	String	DESTROY_METHODS			= "destroyMethods";
	String 	TYPE 					= "type";
	
	/**
	 **********************************************/
}
