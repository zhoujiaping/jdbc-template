package org.sirenia.util;
/**
 * 2个入参，没有返回值
 */
@FunctionalInterface
public interface Callback20<T1,T2>{
	public void apply(T1 t1,T2 t2);
}
