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
package cn.taketoday.web.mapping;

import cn.taketoday.web.Constant;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * views request mapping
 * 
 * @author Today <br>
 * 
 *         2018-06-25 19:58:07
 * @version 2.0.0
 */
@Setter
@Getter
@NoArgsConstructor
public final class ViewMapping implements Serializable {

	private static final long	serialVersionUID	= -8130348090936881368L;
	/** 返回类型 */
	private int					returnType			= Constant.TYPE_FORWARD; // default -> forward
	/** 资源路径 */
	private String				assetsPath			= "";

	@Override
	public String toString() {
		return "[returnType=" + returnType + ", assetsPath=" + assetsPath + "]";
	}

}
