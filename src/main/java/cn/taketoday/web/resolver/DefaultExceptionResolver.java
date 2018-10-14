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
package cn.taketoday.web.resolver;

import java.io.FileNotFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.web.exception.BadRequestException;
import cn.taketoday.web.exception.FileSizeLimitExceededException;
import cn.taketoday.web.exception.MethodNotAllowedException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-25 20:27:22
 */
@Slf4j
@NoArgsConstructor
public class DefaultExceptionResolver implements ExceptionResolver {

	@Override
	public void resolveException(HttpServletRequest request, HttpServletResponse response, Throwable ex) {

		try {

			if (ex instanceof MethodNotAllowedException) {
				response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
			} else if (ex instanceof ConversionException) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
			} else if (ex instanceof BadRequestException) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			} else if (ex instanceof FileNotFoundException) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			} else if (ex instanceof FileSizeLimitExceededException) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
			}
			response.flushBuffer();

			log.error("Catch Throwable: [{}] With Msg: [{}], caused by: [{}].", ex, ex.getMessage(), ex.getCause(), ex);

		} catch (Throwable handlerException) {
			log.error("Handling of [{}] resulted in Exception", ex.getClass().getName(), handlerException);
		}
	}

}
