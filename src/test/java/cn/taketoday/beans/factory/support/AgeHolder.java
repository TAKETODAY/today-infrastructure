package cn.taketoday.beans.factory.support;

public interface AgeHolder {

	default int age() {
		return getAge();
	}

	int getAge();

	void setAge(int age);

}
