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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package test.jdbc.model;

import java.io.Serializable;

import com.alibaba.fastjson.annotation.JSONField;

import cn.taketoday.jdbc.annotation.Id;
import cn.taketoday.jdbc.annotation.Table;
import cn.taketoday.jdbc.annotation.Transient;

@Table("t_user")
@SuppressWarnings("serial")
public class User implements Serializable {

    @Id
    //    @Column(value = "ID", increment = true, notNull = true)
    private int id = 0;
    /** User's Name */
    //    @Column("USER_NAME")
    private String name = "无名氏";
    /*** age */
    //    @Column("AGE")
    private Integer age;

    /** state */
//    @Transient
    @JSONField(serialize = false)
    private UserStatus status;

    @Override
    public String toString() {
        return String.format("{\n\t\"id\":\"%s\",\n\t\"name\":\"%s\",\n\t\"age\":\"%s\"\n}", id, name, age);
    }

    public final int getId() {
        return id;
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final Integer getAge() {
        return age;
    }

    public final void setAge(Integer age) {
        this.age = age;
    }

}
