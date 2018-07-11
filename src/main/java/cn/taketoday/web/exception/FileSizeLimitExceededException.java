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
package cn.taketoday.web.exception;

/**
 * @author Today
 * @date 2018年7月10日 下午9:42:16
 */
public final class FileSizeLimitExceededException extends Exception {

	
	public FileSizeLimitExceededException() {
		
	}
	
	public FileSizeLimitExceededException(long maxRequestSize, Throwable cause) {
		super("The upload file exceeds its maximum permitted size " + maxRequestSize + " bytes", cause);
	}

	private static final long serialVersionUID = 3714652502972383301L;

	
}
