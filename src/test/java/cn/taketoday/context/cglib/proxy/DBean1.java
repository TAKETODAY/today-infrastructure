package cn.taketoday.context.cglib.proxy;

class DBean1 implements DI1 {
    public String getName() {
        return "Chris";
    }

    public String herby() {
        return "Herby";
    }
}
