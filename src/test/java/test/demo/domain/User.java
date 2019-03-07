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

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("serial")
public class User implements Serializable {

	private Integer id;
	private String userName;
	private Integer age;
	private String passwd;
	private String userId;
	private String sex;
	private Date brithday;

	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\n\t\"id\":\"").append(id)//
				.append("\",\n\t\"userName\":\"").append(userName)//
				.append("\",\n\t\"age\":\"").append(age)//
				.append("\",\n\t\"passwd\":\"").append(passwd)//
				.append("\",\n\t\"userId\":\"").append(userId)//
				.append("\",\n\t\"sex\":\"").append(sex)//
				.append("\",\n\t\"brithday\":\"").append(brithday)//
				.append("\"\n}")//
				.toString();
	}

}
