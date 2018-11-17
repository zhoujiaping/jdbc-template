package org.sirenia.session;

import javax.annotation.Resource;

//import org.springframework.stereotype.Service;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
//@Service
public class TestServiceImpl {
	@Resource
	//private SpringSessionFactory sessionFactory;
	private DefaultSessionFactory sessionFactory;
	public void updateTest(){
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("");
		dataSource.setUsername("");
		dataSource.setPassword("");
		dataSource.setUrl("");
		sessionFactory.setDataSource(dataSource );
		Session session = sessionFactory.getSession();
		String sql = "select * from dept where 1=1 and db_source = #{dbSource}";
		JSONObject params = new JSONObject();
		params.put("dbSource", "test");
		JSONArray array = session.query(sql, params);
		System.out.println(array.toJSONString());
		sql = "update dept set dname=#{dname} where deptno=#{id}";
		params.put("dname", null);
		params.put("id", 3);
		int count = session.update(sql, params);
		System.out.println(count);
	}
}
