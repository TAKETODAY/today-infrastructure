/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.expression.lang;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serial;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.FunctionMapper;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Jacob Hookom [jacob@hookom.net]
 * @since 4.0 2021/12/25 16:25
 */
public class FunctionMapperImpl
        extends FunctionMapper implements Externalizable {

  @Serial
  private static final long serialVersionUID = 1L;

  protected Map<String, Function> functions = null;

  /*
   * (non-Javadoc)
   *
   * @see javax.el.FunctionMapper#resolveFunction(java.lang.String,
   * java.lang.String)
   */
  public Method resolveFunction(String prefix, String localName) {
    if (this.functions != null) {
      Function f = this.functions.get(prefix + ":" + localName);
      return f.getMethod();
    }
    return null;
  }

  public void addFunction(String prefix, String localName, Method m) {
    if (this.functions == null) {
      this.functions = new HashMap<>();
    }
    Function f = new Function(prefix, localName, m);
    synchronized(this) {
      this.functions.put(prefix + ":" + localName, f);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(this.functions);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
   */
  // Safe cast
  @SuppressWarnings("unchecked")
  public void readExternal(ObjectInput in) throws IOException,
          ClassNotFoundException {
    this.functions = (Map<String, Function>) in.readObject();
  }

  public static class Function implements Externalizable {

    protected transient Method m;
    protected String owner;
    protected String name;
    protected Class<?>[] types;
    protected String prefix;
    protected String localName;

    /**
     *
     */
    public Function(String prefix, String localName, Method m) {
      if (localName == null) {
        throw new NullPointerException("LocalName cannot be null");
      }
      if (m == null) {
        throw new NullPointerException("Method cannot be null");
      }
      this.prefix = prefix;
      this.localName = localName;
      this.m = m;
    }

    public Function() {
      // for serialization
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {

      out.writeUTF((this.prefix != null) ? this.prefix : "");
      out.writeUTF(this.localName);

      if (this.owner != null) {
        out.writeUTF(this.owner);
      }
      else {
        out.writeUTF(this.m.getDeclaringClass().getName());
      }
      if (this.name != null) {
        out.writeUTF(this.name);
      }
      else {
        out.writeUTF(this.m.getName());
      }
      if (this.types != null) {
        out.writeObject(this.types);
      }
      else {
        out.writeObject(this.m.getParameterTypes());
      }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {

      this.prefix = in.readUTF();
      if ("".equals(this.prefix))
        this.prefix = null;
      this.localName = in.readUTF();
      this.owner = in.readUTF();
      this.name = in.readUTF();
      this.types = (Class<?>[]) in.readObject();
    }

    public Method getMethod() {
      if (this.m == null) {
        try {
          Class<?> t = Class.forName(this.owner, false,
                  Thread.currentThread().getContextClassLoader());
          this.m = t.getMethod(this.name, types);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
      return this.m;
    }

    public boolean matches(String prefix, String localName) {
      if (this.prefix != null) {
        if (prefix == null)
          return false;
        if (!this.prefix.equals(prefix))
          return false;
      }
      return this.localName.equals(localName);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
      if (obj instanceof Function) {
        return this.hashCode() == obj.hashCode();
      }
      return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return (this.prefix + this.localName).hashCode();
    }
  }

}
