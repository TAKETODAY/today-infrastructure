/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.persistence.model;

import java.util.Objects;

import infra.core.style.ToStringBuilder;
import infra.persistence.Id;
import infra.persistence.Table;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:57
 */
@Table("t_user")
public class UserModel {

  @Id
  public Integer id;

  public Integer age;

  public String name;
  public String avatar;
  public String password;
  public String introduce;
  public String mobilePhone;
  public String email;

  public Gender gender;

  public UserModel() {

  }

  public UserModel(String name, Gender gender, int age) {
    this.age = age;
    this.name = name;
    this.gender = gender;
  }

  public Integer getAge() {
    return age;
  }

  public String getAvatar() {
    return avatar;
  }

  public String getEmail() {
    return email;
  }

  public Gender getGender() {
    return gender;
  }

  public Integer getId() {
    return id;
  }

  public String getIntroduce() {
    return introduce;
  }

  public String getMobilePhone() {
    return mobilePhone;
  }

  public String getName() {
    return name;
  }

  public String getPassword() {
    return password;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setGender(Gender gender) {
    this.gender = gender;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setIntroduce(String introduce) {
    this.introduce = introduce;
  }

  public void setMobilePhone(String mobilePhone) {
    this.mobilePhone = mobilePhone;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public static UserModel male(String name, int age) {
    UserModel userModel = new UserModel();
    userModel.name = name;
    userModel.gender = Gender.MALE;
    userModel.age = age;
    return userModel;
  }

  public static UserModel forId(int id) {
    UserModel userModel = new UserModel();
    userModel.id = id;
    return userModel;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof UserModel userModel))
      return false;
    return Objects.equals(age, userModel.age)
            && gender == userModel.gender
            && Objects.equals(id, userModel.id)
            && Objects.equals(name, userModel.name)
            && Objects.equals(email, userModel.email)
            && Objects.equals(avatar, userModel.avatar)
            && Objects.equals(password, userModel.password)
            && Objects.equals(introduce, userModel.introduce)
            && Objects.equals(mobilePhone, userModel.mobilePhone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, age, name, avatar, password, introduce, mobilePhone, email, gender);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("id", id)
            .append("age", age)
            .append("name", name)
            .append("avatar", avatar)
            .append("password", password)
            .append("introduce", introduce)
            .append("mobilePhone", mobilePhone)
            .append("email", email)
            .append("gender", gender)
            .toString();
  }

}
