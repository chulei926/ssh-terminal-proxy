package com.leichu.ssh.terminal.proxy;

@FunctionalInterface
public interface Callback<T> {

	 void emit(T t);

}
