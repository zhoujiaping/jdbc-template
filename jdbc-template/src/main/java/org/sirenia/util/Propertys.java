package org.sirenia.util;

public class Propertys {
	/**
	 * 驼峰命名转下划线命名
	 * */
	public static String camel2underline(String prop){
		return prop.replaceAll("(?<Uper>[A-Z])", "_${Uper}").toLowerCase();
	}
	/**
	 * 下划线转驼峰命名
	 * @param prop
	 * @return
	 */
	public static String underline2camel(String column){
		if(column==null){ 
		return null; 
		} 
		column = column.toLowerCase();
		String[] array = column.split("_(?=[a-z])");
		if(array.length==1){
			return column;
		}
		//System.out.println(String.join(",", array));
		for(int i=1;i<array.length;i++){
			array[i] = array[i].substring(0, 1).toUpperCase()+array[i].substring(1);
		}
		return String.join("", array); 
	}
}
