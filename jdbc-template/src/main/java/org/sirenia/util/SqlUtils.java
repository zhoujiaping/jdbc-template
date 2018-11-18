package org.sirenia.util;

import java.util.ArrayList;
import java.util.List;

import org.sirenia.model.ParsedSql;

import com.alibaba.fastjson.JSONObject;

public class SqlUtils {
	public static ParsedSql parseSql(String sql, JSONObject params) {
		List<String> names = new ArrayList<>();
		sql = new GenericTokenParser("#{","}",(token)->{
			names.add(token);
			return "?";
		}).parse(sql);
		sql = new GenericTokenParser("${","}",(token)->{
			return String.valueOf(params.get(token));
		}).parse(sql);
		ParsedSql parsedSql = new ParsedSql();
		parsedSql.setSql(sql);
		parsedSql.setParamNames(names);
		return parsedSql;
	}
}
