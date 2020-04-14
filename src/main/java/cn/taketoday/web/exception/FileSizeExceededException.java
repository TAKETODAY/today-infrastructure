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

import cn.taketoday.context.utils.DataSize;
import cn.taketoday.web.Constant;
import cn.taketoday.web.annotation.ResponseStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * @author TODAY <br>
 *         2018-07-10 21:42:16
 */
@Setter
@Getter
@SuppressWarnings("serial")
@ResponseStatus(value = 400, msg = Constant.BAD_REQUEST)
public class FileSizeExceededException extends WebRuntimeException {

    /** The actual size of the request. */
    private DataSize actual;
    /** The maximum permitted size of the request. */
    private final DataSize permitted;

    public FileSizeExceededException(DataSize permitted, Throwable cause) {
        super("The upload file exceeds its maximum permitted size: [" + permitted + "]", cause);
        this.permitted = permitted;
    }

}
