/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.MethodExecutor;
import cn.taketoday.expression.MethodResolver;
import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.expression.TypeConverter;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.ReflectionHelper;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;

///CLOVER:OFF

/**
 * Spring Security scenarios from https://wiki.springsource.com/display/SECURITY/Spring+Security+Expression-based+Authorization
 *
 * @author Andy Clement
 */
public class ScenariosForSpringSecurityExpressionTests extends AbstractExpressionTests {

  @Test
  public void testScenario01_Roles() throws Exception {
    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    Expression expr = parser.parseRaw("hasAnyRole('MANAGER','TELLER')");

    ctx.setRootObject(new Person("Ben"));
    Boolean value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isFalse();

    ctx.setRootObject(new Manager("Luke"));
    value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isTrue();
  }

  @Test
  public void testScenario02_ComparingNames() throws Exception {
    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = new StandardEvaluationContext();

    ctx.addPropertyAccessor(new SecurityPrincipalAccessor());

    // Multiple options for supporting this expression: "p.name == principal.name"
    // (1) If the right person is the root context object then "name==principal.name" is good enough
    Expression expr = parser.parseRaw("name == principal.name");

    ctx.setRootObject(new Person("Andy"));
    Boolean value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isTrue();

    ctx.setRootObject(new Person("Christian"));
    value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isFalse();

    // (2) Or register an accessor that can understand 'p' and return the right person
    expr = parser.parseRaw("p.name == principal.name");

    PersonAccessor pAccessor = new PersonAccessor();
    ctx.addPropertyAccessor(pAccessor);
    ctx.setRootObject(null);

    pAccessor.setPerson(new Person("Andy"));
    value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isTrue();

    pAccessor.setPerson(new Person("Christian"));
    value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isFalse();
  }

  @Test
  public void testScenario03_Arithmetic() throws Exception {
    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = new StandardEvaluationContext();

    // Might be better with a as a variable although it would work as a property too...
    // Variable references using a '#'
    Expression expr = parser.parseRaw("(hasRole('SUPERVISOR') or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");

    Boolean value = null;

    ctx.setVariable("a", 1.0d); // referenced as #a in the expression
    ctx.setRootObject(new Supervisor("Ben")); // so non-qualified references 'hasRole()' 'hasIpAddress()' are invoked against it
    value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isTrue();

    ctx.setRootObject(new Manager("Luke"));
    ctx.setVariable("a", 1.043d);
    value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isFalse();
  }

  // Here i'm going to change which hasRole() executes and make it one of my own Java methods
  @Test
  public void testScenario04_ControllingWhichMethodsRun() throws Exception {
    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = new StandardEvaluationContext();

    ctx.setRootObject(new Supervisor("Ben")); // so non-qualified references 'hasRole()' 'hasIpAddress()' are invoked against it);

    ctx.addMethodResolver(new MyMethodResolver()); // NEEDS TO OVERRIDE THE REFLECTION ONE - SHOW REORDERING MECHANISM
    // Might be better with a as a variable although it would work as a property too...
    // Variable references using a '#'
//		SpelExpression expr = parser.parseExpression("(hasRole('SUPERVISOR') or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");
    Expression expr = parser.parseRaw("(hasRole(3) or (#a <  1.042)) and hasIpAddress('10.10.0.0/16')");

    Boolean value = null;

    ctx.setVariable("a", 1.0d); // referenced as #a in the expression
    value = expr.getValue(ctx, Boolean.class);
    assertThat((boolean) value).isTrue();

//			ctx.setRootObject(new Manager("Luke"));
//			ctx.setVariable("a",1.043d);
//			value = (Boolean)expr.getValue(ctx,Boolean.class);
//			assertFalse(value);
  }

  static class Person {

    private final String n;

    Person(String n) { this.n = n; }

    public String[] getRoles() { return new String[] { "NONE" }; }

    public boolean hasAnyRole(String... roles) {
      if (roles == null)
        return true;
      String[] myRoles = getRoles();
      for (int i = 0; i < myRoles.length; i++) {
        for (int j = 0; j < roles.length; j++) {
          if (myRoles[i].equals(roles[j]))
            return true;
        }
      }
      return false;
    }

    public boolean hasRole(String role) {
      return hasAnyRole(role);
    }

    public boolean hasIpAddress(String ipaddr) {
      return true;
    }

    public String getName() { return n; }
  }

  static class Manager extends Person {

    Manager(String n) {
      super(n);
    }

    @Override
    public String[] getRoles() { return new String[] { "MANAGER" }; }
  }

  static class Teller extends Person {

    Teller(String n) {
      super(n);
    }

    @Override
    public String[] getRoles() { return new String[] { "TELLER" }; }
  }

  static class Supervisor extends Person {

    Supervisor(String n) {
      super(n);
    }

    @Override
    public String[] getRoles() { return new String[] { "SUPERVISOR" }; }
  }

  static class SecurityPrincipalAccessor implements PropertyAccessor {

    static class Principal {
      public String name = "Andy";
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
      return name.equals("principal");
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
      return new TypedValue(new Principal());
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
      return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue)
            throws AccessException {
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return null;
    }

  }

  static class PersonAccessor implements PropertyAccessor {

    Person activePerson;

    void setPerson(Person p) { this.activePerson = p; }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
      return name.equals("p");
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
      return new TypedValue(activePerson);
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
      return false;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue)
            throws AccessException {
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return null;
    }

  }

  static class MyMethodResolver implements MethodResolver {

    static class HasRoleExecutor implements MethodExecutor {

      TypeConverter tc;

      public HasRoleExecutor(TypeConverter typeConverter) {
        this.tc = typeConverter;
      }

      @Override
      public TypedValue execute(EvaluationContext context, Object target, Object... arguments)
              throws AccessException {
        try {
          Method m = HasRoleExecutor.class.getMethod("hasRole", String[].class);
          Object[] args = arguments;
          if (args != null) {
            ReflectionHelper.convertAllArguments(tc, args, m);
          }
          if (m.isVarArgs()) {
            args = ReflectionHelper.setupArgumentsForVarargsInvocation(m.getParameterTypes(), args);
          }
          return new TypedValue(m.invoke(null, args), new TypeDescriptor(new MethodParameter(m, -1)));
        }
        catch (Exception ex) {
          throw new AccessException("Problem invoking hasRole", ex);
        }
      }

      public static boolean hasRole(String... strings) {
        return true;
      }
    }

    @Override
    public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> arguments)
            throws AccessException {
      if (name.equals("hasRole")) {
        return new HasRoleExecutor(context.getTypeConverter());
      }
      return null;
    }
  }

}
