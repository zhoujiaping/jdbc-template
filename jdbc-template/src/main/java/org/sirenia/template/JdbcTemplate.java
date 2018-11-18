package org.sirenia.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sirenia.session.Session;
import org.sirenia.session.SessionFactory;
import org.sirenia.util.Propertys;
import org.sirenia.util.Reflects;
import org.sirenia.util.page.PageRes;
import org.sirenia.util.page.Pageable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class JdbcTemplate {
	private SessionFactory sessionFactory;
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}
	/**
	 * 新增记录，返回主键
	 * @param object
	 * @return
	 */
	public <T> T insertSelective(Object object){
		Session session = sessionFactory.getSession(true);
		JSONObject params = (JSONObject) JSONObject.toJSON(object);
		Table table =object.getClass().getAnnotation(Table.class);
		List<String> columnNameList = new ArrayList<>();
		List<String> valueList = new ArrayList<>();
		StringBuilder sql = new StringBuilder("insert into ${tableName}(");
		JSONArray columnsMetaData = session.getColumnsMetaData(null,table.name());
		for(int i=0;i<columnsMetaData.size();i++){
			JSONObject columnMetaData = columnsMetaData.getJSONObject(i);
			String columnName = columnMetaData.getString("columnName");
			String propName = Propertys.underline2camel(columnName);
			Object value = params.get(propName);
			if(value!=null){
				columnNameList.add(columnName);
				valueList.add("#{"+propName+"}");
			}
		}
		sql.append(String.join(",", columnNameList)).append(")values(").append(String.join(",", valueList)).append(");");
		params.put("tableName", table.name());
		session.update(sql.toString(), params);
		JSONObject idWraper = session.queryOne("select last_insert_id() as id;");
		return (T)idWraper.get("id");
	}

	/**
	 * 物理删除
	 * @param pk
	 * @param clazz
	 */
	public <T> int deleteByPk(Object pk,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		Table table =clazz.getAnnotation(Table.class);
		String pkColumn = session.getPkColumn(table.name());
		JSONObject params = new JSONObject();
		params.put("tableName", table.name());
		params.put("pkColumn", pkColumn);
		params.put("pkValue", pk);
		return session.update("delete from ${tableName} where ${pkColumn}=#{pkValue}", params);
	}
	/**
	 * 物理删除,支持乐观锁
	 * @param pk
	 * @param clazz
	 */
	public <T> int deleteByPkAndVersion(Object pk,Integer version,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		Table table =clazz.getAnnotation(Table.class);
		String pkColumn = session.getPkColumn(table.name());
		JSONObject params = new JSONObject();
		params.put("tableName", table.name());
		params.put("versionColumn", table.versionColumn());
		params.put("versionValue", version);
		params.put("pkColumn", pkColumn);
		params.put("pkValue", pk);
		return session.update("delete from ${tableName} where ${pkColumn}=#{pkValue} and ${versionColumn}=#{versionValue}", params);
	}
	/**
	 * 物理删除,支持乐观锁。重载版
	 * @param pk
	 * @param clazz
	 */
	public <T> int deleteByPkAndVersion(T obj){
		Session session = sessionFactory.getSession(true);
		Table table =obj.getClass().getAnnotation(Table.class);
		String pkColumn = session.getPkColumn(table.name());
		JSONObject params = new JSONObject();
		params.put("tableName", table.name());
		params.put("versionColumn", table.versionColumn());
		String versionProp = Propertys.underline2camel(table.versionColumn());
		params.put("versionValue", Reflects.getValueByFieldName(obj, versionProp));
		params.put("pkColumn", pkColumn);
		String pkProp = Propertys.underline2camel(pkColumn);
		params.put("pkValue", Reflects.getValueByFieldName(obj, pkProp));
		return session.update("delete from ${tableName} where ${pkColumn}=#{pkValue} and ${versionColumn}=#{versionValue}", params);
	}
	/**
	 * 逻辑删除
	 * @param pk
	 * @param clazz
	 */
	public <T> int deleteByPkLogically(Object pk,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		Table table =clazz.getAnnotation(Table.class);
		String pkColumn = session.getPkColumn(table.name());
		JSONObject params = new JSONObject();
		params.put("tableName", table.name());
		params.put("validColumn", table.validColumn());
		params.put("validValue", false);
		params.put("pkColumn", pkColumn);
		params.put("pkValue", pk);
		return session.update("update ${tableName} set ${validColumn}=#{validValue} where ${pkColumn}=#{pkValue}", params);
	}
	/**
	 * 逻辑删除,支持乐观锁
	 * @param pk
	 * @param clazz
	 */
	public <T> int deleteByPkAndVersionLogically(Object pk,Integer version,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		Table table =clazz.getAnnotation(Table.class);
		String pkColumn = session.getPkColumn(table.name());
		JSONObject params = new JSONObject();
		params.put("tableName", table.name());
		params.put("validColumn", table.validColumn());
		params.put("validValue", false);
		params.put("versionColumn", table.versionColumn());
		params.put("versionValue", version);
		params.put("pkColumn", pkColumn);
		params.put("pkValue", pk);
		return session.update("update ${tableName} set ${validColumn}=#{validValue} where ${pkColumn}=#{pkValue} and ${versionColumn}=#{versionValue}", params);
	}
	/**
	 * 逻辑删除,支持乐观锁。重载版
	 * @param pk
	 * @param clazz
	 */
	public <T> int deleteByPkAndVersionLogically(Object obj){
		Session session = sessionFactory.getSession(true);
		Table table =obj.getClass().getAnnotation(Table.class);
		String pkColumn = session.getPkColumn(table.name());
		JSONObject params = new JSONObject();
		params.put("tableName", table.name());
		params.put("validColumn", table.validColumn());
		params.put("validValue", false);
		params.put("versionColumn", table.versionColumn());
		String versionProp = Propertys.underline2camel(table.versionColumn());
		params.put("versionValue", Reflects.getValueByFieldName(obj, versionProp));
		params.put("pkColumn", pkColumn);
		String pkProp = Propertys.underline2camel(pkColumn);
		params.put("pkValue", Reflects.getValueByFieldName(obj, pkProp));
		return session.update("update ${tableName} set ${validColumn}=#{validValue} where ${pkColumn}=#{pkValue}", params);
	}
	/**
	 * 根据主键查询
	 * @param pk
	 * @param clazz
	 */
	public <T> T selectByPk(Object pk,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		Table table = clazz.getAnnotation(Table.class);
		JSONObject params = new JSONObject();
		params.put("tableName", table.name());
		String sql = "select * from ${tableName} where ${pkColumn}=#{pkValue}";
		String pkColumn = session.getPkColumn(table.name());
		params.put("pkColumn", pkColumn);
		params.put("pkValue", pk);
		JSONObject res = session.queryOne(sql, params);
		return res.toJavaObject(clazz);
	}
	public int updateByPkSelective(Object object){
		Session session = sessionFactory.getSession(true);
		JSONObject params = (JSONObject) JSONObject.toJSON(object);
		Table table =object.getClass().getAnnotation(Table.class);
		List<String> setList = new ArrayList<>();
		StringBuilder sql = new StringBuilder("update ${tableName} set ");
		JSONArray columnsMetaData = session.getColumnsMetaData(null);
		String pkColumn = session.getPkColumn(table.name());
		Object pkValue = null;
		for(int i=0;i<columnsMetaData.size();i++){
			JSONObject columnMetaData = columnsMetaData.getJSONObject(i);
			String column = columnMetaData.getString("columnName");
			String prop = Propertys.underline2camel(column);
			Object value = params.get(prop);
			if(value!=null){
				if(Objects.equals(column, pkColumn)){
					pkValue = value;
				}else{
					setList.add(column+"=#{"+prop+"}");
				}
			}
		}
		sql.append(String.join(",", setList)).append(" where ${pkColumn}=#{pkValue}");
		params.put("tableName", table.name());
		params.put("pkColumn", pkColumn);
		params.put("pkValue", pkValue);
		session.update(sql.toString(), params);
		return session.update(sql.toString(), params);
	}
	/**
	 * 支持乐观锁的更新
	 * @param object
	 * @return
	 */
	public int updateByPkAndVersionSelective(Object object){
		Session session = sessionFactory.getSession(true);
		JSONObject params = (JSONObject) JSONObject.toJSON(object);
		Table table =object.getClass().getAnnotation(Table.class);
		List<String> setList = new ArrayList<>();
		StringBuilder sql = new StringBuilder("update ${tableName} set ");
		JSONArray columnsMetaData = session.getColumnsMetaData(null);
		String pkColumn = session.getPkColumn(table.name());
		Object pkValue = null;
		String versionColumn = table.versionColumn();
		Object versionValue = null;
		for(int i=0;i<columnsMetaData.size();i++){
			JSONObject columnMetaData = columnsMetaData.getJSONObject(i);
			String column = columnMetaData.getString("columnName");
			String prop = Propertys.underline2camel(column);
			Object value = params.get(prop);
			if(value!=null){
				if(Objects.equals(column, pkColumn)){
					pkValue = value;
				}else if(Objects.equals(column, versionColumn)){
					setList.add(column+"="+column+"+1");
					versionValue = value;
				}else{
					setList.add(column+"=#{"+prop+"}");
				}
			}
		}
		sql.append(String.join(",", setList)).append(" where ${pkColumn}=#{pkValue} and ${versionColumn}=#{versionValue}");
		params.put("tableName", table.name());
		params.put("pkColumn", pkColumn);
		params.put("pkValue", pkValue);
		params.put("versionColumn", pkValue);
		params.put("versionValue", versionValue);
		session.update(sql.toString(), params);
		return session.update(sql.toString(), params);
	}
	/**
	 * 分页查询
	 * @param sql
	 * @param params
	 * @param pageable
	 * @param clazz
	 * @return
	 */
	public <T> PageRes<T> selectWithPage(String sql,JSONObject params,Pageable pageable,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		JSONObject res = session.queryWithPage(sql, params,pageable);
		return res.toJavaObject(PageRes.class);
	}
	/**
	 * 关联查询，得到嵌套的结果。比如user（用户）有orderList（订单列表）
	 * @param sql
	 * @param params
	 * @param config
	 * @param clazz
	 * @return
	 */
	public <T> List<T> selectWithNest(String sql,JSONObject params,List<String> config,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		JSONArray array = session.queryWithNest(sql, params, config);
		return array.toJavaList(clazz);
	}
	/**
	 * 关联查询，得到嵌套的结果。比如user（用户）有orderList（订单列表）
	 * @param sql
	 * @param params
	 * @param config
	 * @param clazz
	 * @return
	 */
	public <T> T selectOneWithNest(String sql,JSONObject params,List<String> config,Class<T> clazz){
		Session session = sessionFactory.getSession(true);
		JSONArray array = session.queryWithNest(sql, params, config);
		if(array.size()==0){
			return null;
		}else if(array.size()==1){
			return array.getObject(0, clazz);
		}else{
			throw new RuntimeException("selectOneWithNest返回了【"+array.size()+"】条结果");
		}
	}
}
