package org.sirenia.session;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.sirenia.template.JdbcTemplate;
import org.sirenia.util.Reflects;
import org.sirenia.util.page.LimitPage;
import org.sirenia.util.page.PageRes;
import org.sirenia.util.page.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:application.xml" })
public class SessionTest {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	//@Resource
	//private EleSignatureSystemUserProtocolService eleSignatureSystemUserProtocolService;
	@Resource
	private TestServiceImpl testServiceImpl;
	@Resource
	//private SpringSessionFactory sessionFactory;
	private DefaultSessionFactory sessionFactory = new DefaultSessionFactory();
	@Test
	public void testMySession() throws Exception{
		//testServiceImpl.updateTest();
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("");
		dataSource.setUsername("");
		dataSource.setPassword("");
		dataSource.setUrl("");
		sessionFactory.setDataSource(dataSource );
		Session session = sessionFactory.getSession(true);
		String sql = "select * from dept where 1=1 and db_source = #{dbSource}";
		JSONObject params = new JSONObject();
		params.put("dbSource", "test");
		JSONArray array = session.query(sql, params);
		logger.info(array.toJSONString());
		sql = "update dept set dname=#{dname} where deptno=#{id}";
		params.put("dname", null);
		params.put("id", 3);
		int count = session.update(sql, params);
		logger.info("{}",count);
		sessionFactory.closeSession(session);
	}
	@Test
	public void testJdbcTemplate(){
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("");
		dataSource.setUsername("");
		dataSource.setPassword("");
		dataSource.setUrl("");
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		Dept dept = template.selectByPk(3, Dept.class);
		System.out.println(JSON.toJSONString(dept));
	}
	@Test
	public void testMetaData(){
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUsername("root");
		dataSource.setPassword("");
		dataSource.setUrl("jdbc:mysql://localhost:3306/test?characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true");
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		Session session = template.getSessionFactory().getSession(true);
		JSONArray users = session.query("select * from sys_user");
		System.out.println(users);
	}
	@Test
	public void testJdbcTemplateSelectWithPage(){
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUsername("");
		dataSource.setPassword("");
		dataSource.setUrl("");
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		Dept dept = template.selectByPk(3, Dept.class);
		System.out.println(JSON.toJSONString(dept));
		JSONObject params = new JSONObject();
		params.put("one", 1);
		Pageable pageable = LimitPage.of(10, 0);
		Class<Dept> clazz = Dept.class;
		PageRes<Dept> res = template.selectWithPage("select * from dept where 1=#{one}", params, pageable, clazz);
		System.out.println(res);
	}
	@Test
	public void testReflectPerform() throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		List<Long> times = new ArrayList<>();
		Dept obj = new Dept();
		//obj.setDname("test");
		//Class.forName("org.sirenia.session.Dept");
		times.add(System.currentTimeMillis());
		//jvm会将加载过的class缓存起来，所以之后读取class不慢。
		for(int i=0;i<100;i++){
			Class<?> clazz = Class.forName("org.sirenia.session.Dept");
			Object dname = Reflects.getValueByFieldName(clazz.newInstance(), "dname");
			times.add(System.currentTimeMillis());
		}
		times.forEach(System.out::println);
	}
	@Test
	public void testFastjsonNullvalue() {
		Dept dept = new Dept();
		dept.setDname("abc");
		//SerializeConfig config = new SerializeConfig();
		//SerializerFeature feature = SerializerFeature.WriteMapNullValue;
		//config.config(Dept.class, feature , true);
		//JSONObject jo = (JSONObject) JSON.toJSON(dept,config);
		JSONObject jo = (JSONObject) JSON.toJSON(dept);
		System.out.println(jo.containsKey("dbSource"));
	}
	@Test
	public void testInsertSelective() {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUsername("root");
		dataSource.setPassword("");
		dataSource.setUrl("jdbc:mysql://localhost:3306/test?characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true");
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		SysUser user = new SysUser();
		user.setName("h-name");
		user.setPassword("h-password");
		user.setCreateTime(new Date());
		BigInteger id = template.insertSelective(user);
		System.out.println(id);
	}
	@Test
	public void testUpdateByPkSelective() {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUsername("root");
		dataSource.setPassword("");
		dataSource.setUrl("jdbc:mysql://localhost:3306/test?characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true");
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		SysUser user = new SysUser();
		user.setId(1L);
		user.setName("h-name");
		user.setPassword("h-password");
		user.setCreateTime(new Date());
		int count = template.updateByPkSelective(user);
		System.out.println(count);
	}
	@Test
	public void testColumns() {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUsername("root");
		dataSource.setPassword("");
		dataSource.setUrl("jdbc:mysql://localhost:3306/test?characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true");
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		Session session = sessionFactory.getSession(true);
		JSONArray columns = session.getColumnsMetaData("test");
		System.out.println(JSONArray.toJSONString(columns, true));
	}
	@Test
	public void testNest() {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUsername("postgres");
		dataSource.setPassword("123456");
		dataSource.setUrl("jdbc:postgresql://localhost:5432/test?characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true");
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		Session session = sessionFactory.getSession(true);
		JSONObject params = null;
		List<String> config = new ArrayList<>();
		config.add("t_user:user_id,t_order:order_id:orderList,t_order_item:item_id:itemList");
		config.add("t_user:user_id,t_addr:addr_id:addrList,t_addr_detail:detail_id:detailList");
		config.add("");
		//JSONArray res = session.selectWithNest("select t1.*,t2.*,t3.*,t4.*,t5.* from t_user t1 left join t_addr t2 on t1.user_id = t2.u_id left join t_order t3 on t3.u_id = t1.user_id left join t_order_item t4 on t4.o_id = t3.order_id left join t_addr_detail t5 on t5.addr_id = t2.addr_id", params , config );
		JSONArray res = session.queryWithNest("select t1.*,t5.* from t_user t1 left join t_addr t2 on t1.user_id = t2.u_id left join t_order t3 on t3.u_id = t1.user_id left join t_order_item t4 on t4.o_id = t3.order_id left join t_addr_detail t5 on t5.addr_id = t2.addr_id", params , config );
		System.out.println(JSONArray.toJSONString(res, true));
	}
	public static void main(String[] args) {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUsername("postgres");
		dataSource.setPassword("123456");
		dataSource.setUrl("jdbc:postgresql://localhost:5432/test?characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true");
		DefaultSessionFactory sessionFactory = new DefaultSessionFactory();
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		Session session = sessionFactory.getSession(true);
		JSONObject params = null;
		List<String> config = new ArrayList<>();
		config.add("t_user:user_id,t_order:order_id:orderList,t_order_item:item_id:itemList");
		//config.add("t_user:user_id,t_addr:addr_id:addr,t_addr_detail:detail_id:detail");
		config.add("");
		JSONArray res = session.queryWithNest("select t1.user_id,(t1.user_id+t1.user_id) as t_user$double_id,t2.*,t3.*,t4.*,t5.* from t_user t1 left join t_addr t2 on t1.user_id = t2.u_id left join t_order t3 on t3.u_id = t1.user_id left join t_order_item t4 on t4.o_id = t3.order_id left join t_addr_detail t5 on t5.addr_id = t2.addr_id", params , config );
		//JSONArray res = session.selectWithNest("select t1.*,t5.* from t_user t1 left join t_addr t2 on t1.user_id = t2.u_id left join t_order t3 on t3.u_id = t1.user_id left join t_order_item t4 on t4.o_id = t3.order_id left join t_addr_detail t5 on t5.addr_id = t2.addr_id", params , config );
		System.out.println(JSONArray.toJSONString(res, true));
	}
	/**
	 * 测试自连接
	 */
	@Test
	public void testSelfNest() {
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setDriverClassName("org.postgresql.Driver");
		dataSource.setUsername("postgres");
		dataSource.setPassword("123456");
		dataSource.setUrl("jdbc:postgresql://localhost:5432/test?characterEncoding=UTF-8&useOldAliasMetaDataBehavior=true");
		DefaultSessionFactory sessionFactory = new DefaultSessionFactory();
		sessionFactory.setDataSource(dataSource );
		JdbcTemplate template = new JdbcTemplate();
		template.setSessionFactory(sessionFactory);
		Session session = sessionFactory.getSession(true);
		JSONObject params = null;
		List<String> config = new ArrayList<>();
		config.add("t_user:user_id,t_user2:user_id:aList");
		config.add("");
		JSONArray res = session.queryWithNest("select t1.*,t2.user_id as t_user2$user_id from t_user t1 left join t_user t2 on t1.user_id>t2.user_id", params , config );
		//JSONArray res = session.selectWithNest("select t1.*,t5.* from t_user t1 left join t_addr t2 on t1.user_id = t2.u_id left join t_order t3 on t3.u_id = t1.user_id left join t_order_item t4 on t4.o_id = t3.order_id left join t_addr_detail t5 on t5.addr_id = t2.addr_id", params , config );
		System.out.println(JSONArray.toJSONString(res, true));
	}
	@Test
	public void testJSONArray() {
		JSONArray array = new JSONArray();
		Dept dept = new Dept();
		dept.setDname("1");
		array.add(dept);
		dept = new Dept();
		dept.setDname("2");
		array.add(dept);
		dept = new Dept();
		dept.setDname("3");
		array.add(dept);
		List<Dept> list = array.toJavaList(Dept.class);
		System.out.println(list);
	}
}

