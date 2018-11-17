package org.sirenia.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class SqlUtils {
	public static JSONObject parseSql(String sql, JSONObject params) {
		JSONObject res = new JSONObject();
		JSONArray names = new JSONArray();
		sql = new GenericTokenParser("#{","}",(token)->{
			names.add(token);
			return "?";
		}).parse(sql);
		sql = new GenericTokenParser("${","}",(token)->{
			return String.valueOf(params.get(token));
		}).parse(sql);
		res.put("sql", sql);
		res.put("names", names);
		return res;
	}
}
