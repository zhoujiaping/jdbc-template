package org.sirenia.util;
/**
 * 3个入参，1个返回值
 */
@FunctionalInterface
public interface Callback41<R,T1,T2,T3,T4>{
	public R apply(T1 t1,T2 t2,T3 t3,T4 t4);
}
