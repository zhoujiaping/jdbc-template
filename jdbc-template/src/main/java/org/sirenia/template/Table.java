package org.sirenia.template;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={ElementType.TYPE})
public @interface Table {
	String name();//表名
	String versionColumn() default "version";//乐观锁版本号字段
	String validColumn() default "valid";//是否删除的标记字段
}
