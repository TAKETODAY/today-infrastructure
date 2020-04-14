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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.exception;

import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.web.Constant;
import cn.taketoday.web.annotation.ResponseStatus;

/**
 * @author TODAY <br>
 *         2018-12-02 09:14
 */
@SuppressWarnings("serial")
@ResponseStatus(value = 500, msg = Constant.INTERNAL_SERVER_ERROR)
public class InternalServerException extends WebRuntimeException {

    public InternalServerException(Throwable cause) {
        super(cause);
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalServerException(String message) {
        super(message);
        LoggerFactory.getLogger(InternalServerException.class).error(message);
    }

    public InternalServerException() {
        super(Constant.INTERNAL_SERVER_ERROR);
    }

}
