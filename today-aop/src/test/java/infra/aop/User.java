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
package infra.aop;

import java.io.Serializable;
import java.util.Objects;

import infra.core.style.ToStringBuilder;

/**
 * @author TODAY <br>
 * 2018-12-06 19:56
 */
@SuppressWarnings("serial")
public class User implements Serializable {

  /** id register time */
  private String id;
  /** state */
  private byte state;
  /** name */
  private String name;
  /** email */
  private String email;
  /** web site */
  private String site;
  /** type */
  private String type;
  /** passwd */
  private String password;
  /** avatar */
  private String image;
  /** description */
  private String introduce;
  /** back ground **/
  private String background;

  public void setBackground(String background) {
    this.background = background;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public void setIntroduce(String introduce) {
    this.introduce = introduce;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public void setState(byte state) {
    this.state = state;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getBackground() {
    return background;
  }

  public String getEmail() {
    return email;
  }

  public String getId() {
    return id;
  }

  public String getImage() {
    return image;
  }

  public String getIntroduce() {
    return introduce;
  }

  public String getName() {
    return name;
  }

  public String getPassword() {
    return password;
  }

  public String getSite() {
    return site;
  }

  public byte getState() {
    return state;
  }

  public String getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof User user))
      return false;
    return state == user.state && Objects.equals(id, user.id) && Objects.equals(name, user.name)
            && Objects.equals(email, user.email) && Objects.equals(site, user.site)
            && Objects.equals(type, user.type) && Objects.equals(password, user.password)
            && Objects.equals(image, user.image) && Objects.equals(introduce, user.introduce)
            && Objects.equals(background, user.background);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, state, name, email, site, type, password, image, introduce, background);
  }

  @Override
  public String toString() {
    return ToStringBuilder.forInstance(this)
            .append("background", background)
            .append("id", id)
            .append("state", state)
            .append("name", name)
            .append("email", email)
            .append("site", site)
            .append("type", type)
            .append("password", password)
            .append("image", image)
            .append("introduce", introduce)
            .toString();
  }
}
