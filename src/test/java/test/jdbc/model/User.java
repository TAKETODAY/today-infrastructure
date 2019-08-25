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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package test.jdbc.model;

import java.io.Serializable;

import cn.taketoday.jdbc.annotation.Column;
import cn.taketoday.jdbc.annotation.Id;
import cn.taketoday.jdbc.annotation.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table("t_user")
@SuppressWarnings("serial")
public class User implements Serializable {

    @Id
    @Column(value = "ID", increment = true, notNull = true)
    private int id = 0;
    /** User's Name */
    @Column("USER_NAME")
    private String name = "无名氏";
    /*** age */
    @Column("AGE")
    private Integer age;

    @Override
    public String toString() {
        return String.format("{\n\t\"id\":\"%s\",\n\t\"name\":\"%s\",\n\t\"age\":\"%s\"\n}", id, name, age);
    }
}
