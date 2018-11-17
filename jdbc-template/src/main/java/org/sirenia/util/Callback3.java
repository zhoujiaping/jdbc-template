package org.sirenia.util;
/**
 * 3个入参，没有返回值
 */
public interface Callback3<T1,T2,T3>{
	public void apply(T1 t1,T2 t2,T3 t3);
}
