/*
 * Copyright 2017 - 2024 the original author or authors.
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
package infra.bytecode.reflect;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import infra.bytecode.reflect.sub.Parent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MethodAccessTests {

  public static class Simple { }

  public static class ThrowsSomething {
    public void foo() throws IOException {
      throw new IOException("hello");
    }
  }

  @Test
  public void testSimple() throws Throwable {
    MethodAccess.from(Simple.class).newInstance();
    MethodAccess.from(Simple.class).newInstance();
  }

  @Test
  public void testException() throws Throwable {
    MethodAccess fc = MethodAccess.from(ThrowsSomething.class);
    ThrowsSomething ts = new ThrowsSomething();
    try {
      fc.invoke("foo", new Class[0], ts, new Object[0]);
      fail("expected exception");
    }
    catch (InvocationTargetException e) {
      assertTrue(e.getTargetException() instanceof IOException);
    }
  }

  public static class Child extends Parent { }

  @Test
  public void testSuperclass() throws Throwable {
    MethodAccess fc = MethodAccess.from(Child.class);
    assertEquals("dill", new Child().getHerb());
    assertEquals("dill", fc.invoke("getHerb", new Class[0], new Child(), new Object[0]));
  }

  @Test
  public void testTypeMismatch() throws Throwable {
    MethodAccess fc = MethodAccess.from(ThrowsSomething.class);
    ThrowsSomething ts = new ThrowsSomething();
    try {
      fc.invoke("foo", new Class[] { Integer.TYPE }, ts, new Object[0]);
      fail("expected exception");
    }
    catch (IllegalArgumentException ignore) { }
  }

  @Test
  public void testComplex() throws Throwable {
    MethodAccess fc = MethodAccess.from(MemberSwitchBean.class);
    MemberSwitchBean bean = (MemberSwitchBean) fc.newInstance();
    assertEquals(0, bean.init, "bean.init");
    assertEquals("infra.bytecode.reflect.MemberSwitchBean", fc.getName(), "fc.getName()");
    assertEquals(MemberSwitchBean.class, fc.getDeclaringClass(), "fc.getDeclaringClass()");
    assertEquals(13, fc.getMaxIndex(), "fc.getMaxIndex()");

    Method m1 = MemberSwitchBean.class.getMethod("foo", Integer.TYPE, String.class);
    assertEquals(6, fc.getMethod(m1).invoke(bean,
                    new Object[]
                            { 0, "" }),
            "fc.getMethod(m1).invoke(bean, new Object[]{ new Integer(0), \"\" })");

    // TODO: should null be allowed here?
    Method m2 = MemberSwitchBean.class.getDeclaredMethod("pkg", (Class[]) null);
    assertEquals(9, fc.getMethod(m2).invoke(bean, null), "fc.getMethod(m2).invoke(bean, null)");
  }

  @Test
  public void testStatic() throws Throwable {
    MethodAccess fc = MethodAccess.from(MemberSwitchBean.class);
    // MemberSwitchBean bean = (MemberSwitchBean)fc.newInstance();
    assertEquals(fc.invoke("staticMethod", new Class[0], null, null), 10);
  }

  public static abstract class ReallyBigClass {
    public ReallyBigClass() { }

    abstract public void method1(int i, short s, float f);

    abstract public void method1(int i, byte d, float f);

    abstract public void method2(int i, short s, float f);

    abstract public void method2(int i, byte d, float f);

    abstract public void method3(int i, short s, float f);

    abstract public void method3(int i, byte d, float f);

    abstract public void method4(int i, short s, float f);

    abstract public void method4(int i, byte d, float f);

    abstract public void method5(int i, short s, float f);

    abstract public void method5(int i, byte d, float f);

    abstract public void method6(int i, short s, float f);

    abstract public void method6(int i, byte d, float f);

    abstract public void method7(int i, short s, float f);

    abstract public void method7(int i, byte d, float f);

    abstract public void method8(int i, short s, float f);

    abstract public void method8(int i, byte d, float f);

    abstract public void method9(int i, short s, float f);

    abstract public void method9(int i, byte d, float f);

    abstract public void method10(int i, short s, float f);

    abstract public void method10(int i, byte d, float f);

    abstract public void method11(int i, short s, float f);

    abstract public void method11(int i, byte d, float f);

    abstract public void method12(int i, short s, float f);

    abstract public void method12(int i, byte d, float f);

    abstract public void method13(int i, short s, float f);

    abstract public void method13(int i, byte d, float f);

    abstract public void method14(int i, short s, float f);

    abstract public void method14(int i, byte d, float f);

    abstract public void method15(int i, short s, float f);

    abstract public void method15(int i, byte d, float f);

    abstract public void method16(int i, short s, float f);

    abstract public void method16(int i, byte d, float f);

    abstract public void method17(int i, short s, float f);

    abstract public void method17(int i, byte d, float f);

    abstract public void method18(int i, short s, float f);

    abstract public void method18(int i, byte d, float f);

    abstract public void method19(int i, short s, float f);

    abstract public void method19(int i, byte d, float f);

    abstract public void method20(int i, short s, float f);

    abstract public void method20(int i, byte d, float f);

    abstract public void method21(int i, short s, float f);

    abstract public void method21(int i, byte d, float f);

    abstract public void method22(int i, short s, float f);

    abstract public void method22(int i, byte d, float f);

    abstract public void method23(int i, short s, float f);

    abstract public void method23(int i, byte d, float f);

    abstract public void method24(int i, short s, float f);

    abstract public void method24(int i, byte d, float f);

    abstract public void method25(int i, short s, float f);

    abstract public void method25(int i, byte d, float f);

    abstract public void method26(int i, short s, float f);

    abstract public void method26(int i, byte d, float f);

    abstract public void method27(int i, short s, float f);

    abstract public void method27(int i, byte d, float f);

    abstract public void method28(int i, short s, float f);

    abstract public void method28(int i, byte d, float f);

    abstract public void method29(int i, short s, float f);

    abstract public void method29(int i, byte d, float f);

    abstract public void method30(int i, short s, float f);

    abstract public void method30(int i, byte d, float f);

    abstract public void method31(int i, short s, float f);

    abstract public void method31(int i, byte d, float f);

    abstract public void method32(int i, short s, float f);

    abstract public void method32(int i, byte d, float f);

    abstract public void method33(int i, short s, float f);

    abstract public void method33(int i, byte d, float f);

    abstract public void method34(int i, short s, float f);

    abstract public void method34(int i, byte d, float f);

    abstract public void method35(int i, short s, float f);

    abstract public void method35(int i, byte d, float f);

    abstract public void method36(int i, short s, float f);

    abstract public void method36(int i, byte d, float f);

    abstract public void method37(int i, short s, float f);

    abstract public void method37(int i, byte d, float f);

    abstract public void method38(int i, short s, float f);

    abstract public void method38(int i, byte d, float f);

    abstract public void method39(int i, short s, float f);

    abstract public void method39(int i, byte d, float f);

    abstract public void method40(int i, short s, float f);

    abstract public void method40(int i, byte d, float f);

    abstract public void method41(int i, short s, float f);

    abstract public void method41(int i, byte d, float f);

    abstract public void method42(int i, short s, float f);

    abstract public void method42(int i, byte d, float f);

    abstract public void method43(int i, short s, float f);

    abstract public void method43(int i, byte d, float f);

    abstract public void method44(int i, short s, float f);

    abstract public void method44(int i, byte d, float f);

    abstract public void method45(int i, short s, float f);

    abstract public void method45(int i, byte d, float f);

    abstract public void method46(int i, short s, float f);

    abstract public void method46(int i, byte d, float f);

    abstract public void method47(int i, short s, float f);

    abstract public void method47(int i, byte d, float f);

    abstract public void method48(int i, short s, float f);

    abstract public void method48(int i, byte d, float f);

    abstract public void method49(int i, short s, float f);

    abstract public void method49(int i, byte d, float f);

    abstract public void method50(int i, short s, float f);

    abstract public void method50(int i, byte d, float f);

    abstract public void method51(int i, short s, float f);

    abstract public void method51(int i, byte d, float f);

    abstract public void method52(int i, short s, float f);

    abstract public void method52(int i, byte d, float f);

    abstract public void method53(int i, short s, float f);

    abstract public void method53(int i, byte d, float f);

    abstract public void method54(int i, short s, float f);

    abstract public void method54(int i, byte d, float f);

    abstract public void method55(int i, short s, float f);

    abstract public void method55(int i, byte d, float f);

    abstract public void method56(int i, short s, float f);

    abstract public void method56(int i, byte d, float f);

    abstract public void method57(int i, short s, float f);

    abstract public void method57(int i, byte d, float f);

    abstract public void method58(int i, short s, float f);

    abstract public void method58(int i, byte d, float f);

    abstract public void method59(int i, short s, float f);

    abstract public void method59(int i, byte d, float f);

    abstract public void method60(int i, short s, float f);

    abstract public void method60(int i, byte d, float f);

    abstract public void method61(int i, short s, float f);

    abstract public void method61(int i, byte d, float f);

    abstract public void method62(int i, short s, float f);

    abstract public void method62(int i, byte d, float f);

    abstract public void method63(int i, short s, float f);

    abstract public void method63(int i, byte d, float f);

    abstract public void method64(int i, short s, float f);

    abstract public void method64(int i, byte d, float f);

    abstract public void method65(int i, short s, float f);

    abstract public void method65(int i, byte d, float f);

    abstract public void method66(int i, short s, float f);

    abstract public void method66(int i, byte d, float f);

    abstract public void method67(int i, short s, float f);

    abstract public void method67(int i, byte d, float f);

    abstract public void method68(int i, short s, float f);

    abstract public void method68(int i, byte d, float f);

    abstract public void method69(int i, short s, float f);

    abstract public void method69(int i, byte d, float f);

    abstract public void method70(int i, short s, float f);

    abstract public void method70(int i, byte d, float f);

    abstract public void method71(int i, short s, float f);

    abstract public void method71(int i, byte d, float f);

    abstract public void method72(int i, short s, float f);

    abstract public void method72(int i, byte d, float f);

    abstract public void method73(int i, short s, float f);

    abstract public void method73(int i, byte d, float f);

    abstract public void method74(int i, short s, float f);

    abstract public void method74(int i, byte d, float f);

    abstract public void method75(int i, short s, float f);

    abstract public void method75(int i, byte d, float f);

    abstract public void method76(int i, short s, float f);

    abstract public void method76(int i, byte d, float f);

    abstract public void method77(int i, short s, float f);

    abstract public void method77(int i, byte d, float f);

    abstract public void method78(int i, short s, float f);

    abstract public void method78(int i, byte d, float f);

    abstract public void method79(int i, short s, float f);

    abstract public void method79(int i, byte d, float f);

    abstract public void method80(int i, short s, float f);

    abstract public void method80(int i, byte d, float f);

    abstract public void method81(int i, short s, float f);

    abstract public void method81(int i, byte d, float f);

    abstract public void method82(int i, short s, float f);

    abstract public void method82(int i, byte d, float f);

    abstract public void method83(int i, short s, float f);

    abstract public void method83(int i, byte d, float f);

    abstract public void method84(int i, short s, float f);

    abstract public void method84(int i, byte d, float f);

    abstract public void method85(int i, short s, float f);

    abstract public void method85(int i, byte d, float f);

    abstract public void method86(int i, short s, float f);

    abstract public void method86(int i, byte d, float f);

    abstract public void method87(int i, short s, float f);

    abstract public void method87(int i, byte d, float f);

    abstract public void method88(int i, short s, float f);

    abstract public void method88(int i, byte d, float f);

    abstract public void method89(int i, short s, float f);

    abstract public void method89(int i, byte d, float f);

    abstract public void method90(int i, short s, float f);

    abstract public void method90(int i, byte d, float f);

    abstract public void method91(int i, short s, float f);

    abstract public void method91(int i, byte d, float f);

    abstract public void method92(int i, short s, float f);

    abstract public void method92(int i, byte d, float f);

    abstract public void method93(int i, short s, float f);

    abstract public void method93(int i, byte d, float f);

    abstract public void method94(int i, short s, float f);

    abstract public void method94(int i, byte d, float f);

    abstract public void method95(int i, short s, float f);

    abstract public void method95(int i, byte d, float f);

    abstract public void method96(int i, short s, float f);

    abstract public void method96(int i, byte d, float f);

    abstract public void method97(int i, short s, float f);

    abstract public void method97(int i, byte d, float f);

    abstract public void method98(int i, short s, float f);

    abstract public void method98(int i, byte d, float f);

    abstract public void method99(int i, short s, float f);

    abstract public void method99(int i, byte d, float f);

    abstract public void method100(int i, short s, float f);

    abstract public void method100(int i, byte d, float f);

    abstract public void method101(int i, short s, float f);

    abstract public void method101(int i, byte d, float f);

    abstract public void method102(int i, short s, float f);

    abstract public void method102(int i, byte d, float f);

    abstract public void method103(int i, short s, float f);

    abstract public void method103(int i, byte d, float f);

    abstract public void method104(int i, short s, float f);

    abstract public void method104(int i, byte d, float f);

    abstract public void method105(int i, short s, float f);

    abstract public void method105(int i, byte d, float f);

    abstract public void method106(int i, short s, float f);

    abstract public void method106(int i, byte d, float f);

    abstract public void method107(int i, short s, float f);

    abstract public void method107(int i, byte d, float f);

    abstract public void method108(int i, short s, float f);

    abstract public void method108(int i, byte d, float f);

    abstract public void method109(int i, short s, float f);

    abstract public void method109(int i, byte d, float f);

    abstract public void method110(int i, short s, float f);

    abstract public void method110(int i, byte d, float f);

    abstract public void method111(int i, short s, float f);

    abstract public void method111(int i, byte d, float f);

    abstract public void method112(int i, short s, float f);

    abstract public void method112(int i, byte d, float f);

    abstract public void method113(int i, short s, float f);

    abstract public void method113(int i, byte d, float f);

    abstract public void method114(int i, short s, float f);

    abstract public void method114(int i, byte d, float f);

    abstract public void method115(int i, short s, float f);

    abstract public void method115(int i, byte d, float f);

    abstract public void method116(int i, short s, float f);

    abstract public void method116(int i, byte d, float f);

    abstract public void method117(int i, short s, float f);

    abstract public void method117(int i, byte d, float f);

    abstract public void method118(int i, short s, float f);

    abstract public void method118(int i, byte d, float f);

    abstract public void method119(int i, short s, float f);

    abstract public void method119(int i, byte d, float f);

    abstract public void method120(int i, short s, float f);

    abstract public void method120(int i, byte d, float f);

    abstract public void methodB1(int i, short s, float f);

    abstract public void methodB1(int i, byte d, float f);

    abstract public void methodB2(int i, short s, float f);

    abstract public void methodB2(int i, byte d, float f);

    abstract public void methodB3(int i, short s, float f);

    abstract public void methodB3(int i, byte d, float f);

    abstract public void methodB4(int i, short s, float f);

    abstract public void methodB4(int i, byte d, float f);

    abstract public void methodB5(int i, short s, float f);

    abstract public void methodB5(int i, byte d, float f);

    abstract public void methodB6(int i, short s, float f);

    abstract public void methodB6(int i, byte d, float f);

    abstract public void methodB7(int i, short s, float f);

    abstract public void methodB7(int i, byte d, float f);

    abstract public void methodB8(int i, short s, float f);

    abstract public void methodB8(int i, byte d, float f);

    abstract public void methodB9(int i, short s, float f);

    abstract public void methodB9(int i, byte d, float f);

    abstract public void methodB10(int i, short s, float f);

    abstract public void methodB10(int i, byte d, float f);

    abstract public void methodB11(int i, short s, float f);

    abstract public void methodB11(int i, byte d, float f);

    abstract public void methodB12(int i, short s, float f);

    abstract public void methodB12(int i, byte d, float f);

    abstract public void methodB13(int i, short s, float f);

    abstract public void methodB13(int i, byte d, float f);

    abstract public void methodB14(int i, short s, float f);

    abstract public void methodB14(int i, byte d, float f);

    abstract public void methodB15(int i, short s, float f);

    abstract public void methodB15(int i, byte d, float f);

    abstract public void methodB16(int i, short s, float f);

    abstract public void methodB16(int i, byte d, float f);

    abstract public void methodB17(int i, short s, float f);

    abstract public void methodB17(int i, byte d, float f);

    abstract public void methodB18(int i, short s, float f);

    abstract public void methodB18(int i, byte d, float f);

    abstract public void methodB19(int i, short s, float f);

    abstract public void methodB19(int i, byte d, float f);

    abstract public void methodB20(int i, short s, float f);

    abstract public void methodB20(int i, byte d, float f);

    abstract public void methodB21(int i, short s, float f);

    abstract public void methodB21(int i, byte d, float f);

    abstract public void methodB22(int i, short s, float f);

    abstract public void methodB22(int i, byte d, float f);

    abstract public void methodB23(int i, short s, float f);

    abstract public void methodB23(int i, byte d, float f);

    abstract public void methodB24(int i, short s, float f);

    abstract public void methodB24(int i, byte d, float f);

    abstract public void methodB25(int i, short s, float f);

    abstract public void methodB25(int i, byte d, float f);

    abstract public void methodB26(int i, short s, float f);

    abstract public void methodB26(int i, byte d, float f);

    abstract public void methodB27(int i, short s, float f);

    abstract public void methodB27(int i, byte d, float f);

    abstract public void methodB28(int i, short s, float f);

    abstract public void methodB28(int i, byte d, float f);

    abstract public void methodB29(int i, short s, float f);

    abstract public void methodB29(int i, byte d, float f);

    abstract public void methodB30(int i, short s, float f);

    abstract public void methodB30(int i, byte d, float f);

    abstract public void methodB31(int i, short s, float f);

    abstract public void methodB31(int i, byte d, float f);

    abstract public void methodB32(int i, short s, float f);

    abstract public void methodB32(int i, byte d, float f);

    abstract public void methodB33(int i, short s, float f);

    abstract public void methodB33(int i, byte d, float f);

    abstract public void methodB34(int i, short s, float f);

    abstract public void methodB34(int i, byte d, float f);

    abstract public void methodB35(int i, short s, float f);

    abstract public void methodB35(int i, byte d, float f);

    abstract public void methodB36(int i, short s, float f);

    abstract public void methodB36(int i, byte d, float f);

    abstract public void methodB37(int i, short s, float f);

    abstract public void methodB37(int i, byte d, float f);

    abstract public void methodB38(int i, short s, float f);

    abstract public void methodB38(int i, byte d, float f);

    abstract public void methodB39(int i, short s, float f);

    abstract public void methodB39(int i, byte d, float f);

    abstract public void methodB40(int i, short s, float f);

    abstract public void methodB40(int i, byte d, float f);

    abstract public void methodB41(int i, short s, float f);

    abstract public void methodB41(int i, byte d, float f);

    abstract public void methodB42(int i, short s, float f);

    abstract public void methodB42(int i, byte d, float f);

    abstract public void methodB43(int i, short s, float f);

    abstract public void methodB43(int i, byte d, float f);

    abstract public void methodB44(int i, short s, float f);

    abstract public void methodB44(int i, byte d, float f);

    abstract public void methodB45(int i, short s, float f);

    abstract public void methodB45(int i, byte d, float f);

    abstract public void methodB46(int i, short s, float f);

    abstract public void methodB46(int i, byte d, float f);

    abstract public void methodB47(int i, short s, float f);

    abstract public void methodB47(int i, byte d, float f);

    abstract public void methodB48(int i, short s, float f);

    abstract public void methodB48(int i, byte d, float f);

    abstract public void methodB49(int i, short s, float f);

    abstract public void methodB49(int i, byte d, float f);

    abstract public void methodB50(int i, short s, float f);

    abstract public void methodB50(int i, byte d, float f);

    abstract public void methodB51(int i, short s, float f);

    abstract public void methodB51(int i, byte d, float f);

    abstract public void methodB52(int i, short s, float f);

    abstract public void methodB52(int i, byte d, float f);

    abstract public void methodB53(int i, short s, float f);

    abstract public void methodB53(int i, byte d, float f);

    abstract public void methodB54(int i, short s, float f);

    abstract public void methodB54(int i, byte d, float f);

    abstract public void methodB55(int i, short s, float f);

    abstract public void methodB55(int i, byte d, float f);

    abstract public void methodB56(int i, short s, float f);

    abstract public void methodB56(int i, byte d, float f);

    abstract public void methodB57(int i, short s, float f);

    abstract public void methodB57(int i, byte d, float f);

    abstract public void methodB58(int i, short s, float f);

    abstract public void methodB58(int i, byte d, float f);

    abstract public void methodB59(int i, short s, float f);

    abstract public void methodB59(int i, byte d, float f);

    abstract public void methodB60(int i, short s, float f);

    abstract public void methodB60(int i, byte d, float f);

    abstract public void methodB61(int i, short s, float f);

    abstract public void methodB61(int i, byte d, float f);

    abstract public void methodB62(int i, short s, float f);

    abstract public void methodB62(int i, byte d, float f);

    abstract public void methodB63(int i, short s, float f);

    abstract public void methodB63(int i, byte d, float f);

    abstract public void methodB64(int i, short s, float f);

    abstract public void methodB64(int i, byte d, float f);

    abstract public void methodB65(int i, short s, float f);

    abstract public void methodB65(int i, byte d, float f);

    abstract public void methodB66(int i, short s, float f);

    abstract public void methodB66(int i, byte d, float f);

    abstract public void methodB67(int i, short s, float f);

    abstract public void methodB67(int i, byte d, float f);

    abstract public void methodB68(int i, short s, float f);

    abstract public void methodB68(int i, byte d, float f);

    abstract public void methodB69(int i, short s, float f);

    abstract public void methodB69(int i, byte d, float f);

    abstract public void methodB70(int i, short s, float f);

    abstract public void methodB70(int i, byte d, float f);

    abstract public void methodB71(int i, short s, float f);

    abstract public void methodB71(int i, byte d, float f);

    abstract public void methodB72(int i, short s, float f);

    abstract public void methodB72(int i, byte d, float f);

    abstract public void methodB73(int i, short s, float f);

    abstract public void methodB73(int i, byte d, float f);

    abstract public void methodB74(int i, short s, float f);

    abstract public void methodB74(int i, byte d, float f);

    abstract public void methodB75(int i, short s, float f);

    abstract public void methodB75(int i, byte d, float f);

    abstract public void methodB76(int i, short s, float f);

    abstract public void methodB76(int i, byte d, float f);

    abstract public void methodB77(int i, short s, float f);

    abstract public void methodB77(int i, byte d, float f);

    abstract public void methodB78(int i, short s, float f);

    abstract public void methodB78(int i, byte d, float f);

    abstract public void methodB79(int i, short s, float f);

    abstract public void methodB79(int i, byte d, float f);

    abstract public void methodB80(int i, short s, float f);

    abstract public void methodB80(int i, byte d, float f);

    abstract public void methodB81(int i, short s, float f);

    abstract public void methodB81(int i, byte d, float f);

    abstract public void methodB82(int i, short s, float f);

    abstract public void methodB82(int i, byte d, float f);

    abstract public void methodB83(int i, short s, float f);

    abstract public void methodB83(int i, byte d, float f);

    abstract public void methodB84(int i, short s, float f);

    abstract public void methodB84(int i, byte d, float f);

    abstract public void methodB85(int i, short s, float f);

    abstract public void methodB85(int i, byte d, float f);

    abstract public void methodB86(int i, short s, float f);

    abstract public void methodB86(int i, byte d, float f);

    abstract public void methodB87(int i, short s, float f);

    abstract public void methodB87(int i, byte d, float f);

    abstract public void methodB88(int i, short s, float f);

    abstract public void methodB88(int i, byte d, float f);

    abstract public void methodB89(int i, short s, float f);

    abstract public void methodB89(int i, byte d, float f);

    abstract public void methodB90(int i, short s, float f);

    abstract public void methodB90(int i, byte d, float f);

    abstract public void methodB91(int i, short s, float f);

    abstract public void methodB91(int i, byte d, float f);

    abstract public void methodB92(int i, short s, float f);

    abstract public void methodB92(int i, byte d, float f);

    abstract public void methodB93(int i, short s, float f);

    abstract public void methodB93(int i, byte d, float f);

    abstract public void methodB94(int i, short s, float f);

    abstract public void methodB94(int i, byte d, float f);

    abstract public void methodB95(int i, short s, float f);

    abstract public void methodB95(int i, byte d, float f);

    abstract public void methodB96(int i, short s, float f);

    abstract public void methodB96(int i, byte d, float f);

    abstract public void methodB97(int i, short s, float f);

    abstract public void methodB97(int i, byte d, float f);

    abstract public void methodB98(int i, short s, float f);

    abstract public void methodB98(int i, byte d, float f);

    abstract public void methodB99(int i, short s, float f);

    abstract public void methodB99(int i, byte d, float f);

    abstract public void methodB100(int i, short s, float f);

    abstract public void methodB100(int i, byte d, float f);

    abstract public void methodB101(int i, short s, float f);

    abstract public void methodB101(int i, byte d, float f);

    abstract public void methodB102(int i, short s, float f);

    abstract public void methodB102(int i, byte d, float f);

    abstract public void methodB103(int i, short s, float f);

    abstract public void methodB103(int i, byte d, float f);

    abstract public void methodB104(int i, short s, float f);

    abstract public void methodB104(int i, byte d, float f);

    abstract public void methodB105(int i, short s, float f);

    abstract public void methodB105(int i, byte d, float f);

    abstract public void methodB106(int i, short s, float f);

    abstract public void methodB106(int i, byte d, float f);

    abstract public void methodB107(int i, short s, float f);

    abstract public void methodB107(int i, byte d, float f);

    abstract public void methodB108(int i, short s, float f);

    abstract public void methodB108(int i, byte d, float f);

    abstract public void methodB109(int i, short s, float f);

    abstract public void methodB109(int i, byte d, float f);

    abstract public void methodB110(int i, short s, float f);

    abstract public void methodB110(int i, byte d, float f);

    abstract public void methodB111(int i, short s, float f);

    abstract public void methodB111(int i, byte d, float f);

    abstract public void methodB112(int i, short s, float f);

    abstract public void methodB112(int i, byte d, float f);

    abstract public void methodB113(int i, short s, float f);

    abstract public void methodB113(int i, byte d, float f);

    abstract public void methodB114(int i, short s, float f);

    abstract public void methodB114(int i, byte d, float f);

    abstract public void methodB115(int i, short s, float f);

    abstract public void methodB115(int i, byte d, float f);

    abstract public void methodB116(int i, short s, float f);

    abstract public void methodB116(int i, byte d, float f);

    abstract public void methodB117(int i, short s, float f);

    abstract public void methodB117(int i, byte d, float f);

    abstract public void methodB118(int i, short s, float f);

    abstract public void methodB118(int i, byte d, float f);

    abstract public void methodB119(int i, short s, float f);

    abstract public void methodB119(int i, byte d, float f);

    abstract public void methodB120(int i, short s, float f);

    abstract public void methodB120(int i, byte d, float f);
  }

  @Test
  public void testReallyBigClass() throws IOException {
    MethodAccess.Generator gen = new MethodAccess.Generator(ReallyBigClass.class);
    MethodAccess fc = gen.create();
  }

  @Test
  public void testGetMethod() throws Exception {
    MethodAccess fc = MethodAccess.from(Base.class);
    FastMethodAccessor method = fc.getMethod(
            Base.class.getDeclaredMethod("foo", new Class[]
                    { String.class }));
    assertEquals("hello world", method.invoke(new Base(), new Object[] { "hello world" }));
  }

  class Base {
    CharSequence foo(String f) {
      return f;
    }
  }

  @Test
  public void testGetMethod_covarientOverride() throws Exception {
    MethodAccess fc = MethodAccess.from(Sub.class);
    FastMethodAccessor method = fc.getMethod(
            Sub.class.getDeclaredMethod("foo", new Class[]
                    { String.class }));
    assertEquals("foofoo", method.invoke(new Sub(), new Object[] { "foo" }));
  }

  class Sub extends Base {
    String foo(String f) {
      return f + f;
    }
  }

  class HasProtectedMethod {
    protected int foo() {
      return 2;
    }
  }

  // Previously fastclass would refuse to generate accessors for protected
  // methods.
  @Test
  public void testProtectedMethod() throws Exception {
    MethodAccess fc = MethodAccess.from(HasProtectedMethod.class);
    Method fooMethod = HasProtectedMethod.class.getDeclaredMethod("foo");
    assertEquals(2, fc.getMethod(fooMethod).invoke(new HasProtectedMethod(), new Object[0]));
  }

  @Test
  public void testProtectedMethod_bootstrapClassLoader() throws Exception {
    // Can't access protected methods on the bootstrap loader
    MethodAccess fc = MethodAccess.from(ArrayList.class);
    Method removeRangeMethod = ArrayList.class.getDeclaredMethod("removeRange", int.class, int.class);
    try {
      // TODO(lukes): getMethod throws IAE if it can't be found (seems reasonable)
      // however getConstructor returns a FastConstructor with an -1 index.
      fc.getMethod(removeRangeMethod);
      fail();
    }
    catch (IllegalArgumentException iae) { }
  }

  @Test
  public void testPackagePrivateMethod_bootstrapClassLoader() throws Exception {
    // Executable getRoot()
    // versions of fastclass would try to call it and it would result in an
    // IllegalAccessException at runtime.
    MethodAccess fc = MethodAccess.from(Method.class);
    Method method = Method.class.getDeclaredMethod("getRoot");
    try {
      fc.getMethod(method);
      fail();
    }
    catch (IllegalArgumentException iae) { }
  }

}
