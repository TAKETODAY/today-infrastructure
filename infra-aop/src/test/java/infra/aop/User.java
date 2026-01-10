/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
