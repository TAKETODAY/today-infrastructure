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
package cn.taketoday.context.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * configuration exception
 * 
 * @author Today <br>
 * 
 *         2018-08-08 09:55
 */
@Slf4j
public class ConfigurationException extends Exception {

	private static final long serialVersionUID = -5325643331368019892L;

	public ConfigurationException() {
		this("");
	}
	
	public ConfigurationException(Throwable cause) {
		super(cause);
	}

	public ConfigurationException(String message, Object ... args) {
		super(message);
		log.error("Configuration Exception Message -> [" + message + "] , Your Application Will Be Shutdown.", args);
		System.exit(0);
	}
}
