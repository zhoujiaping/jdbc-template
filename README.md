# jdbc-template
## 项目背景
用过hibernate、mybatis，了解过spring的jdbc-template。觉得hibernate框架太重太复杂，sql优化不方便。
mybatis强制使用xml配置提供sql（注解方式提供sql能力有限，provider方式提供sql也不方便），
而且要配置ResultMap、关联查询又要比较复杂的配置，多表关联查询时对不同表字段名相同的情况处理不够令人满意。
mybatis框架在启动时将很多配置固化，运行时很难动态改变，不灵活。
spring的jdbc-template写起来也要很多代码，orm方面不足，特别是多表关联查询方面。
所以希望有一个持久层框架能够更简洁方便，提供orm、方便sql优化、仅仅需要极简的配置、能融合分页、乐观锁等内容。
## 主要功能
  * 提供单表curd
  * 提供面向对象方式的接口
  * 提供面向sql方式的接口
  * 提供分页查询支持（有两个接口，一个用自动生成的sql统计总记录数，一个用用户提供的sql统计总记录数）
  * 提供乐观锁支持
  * 提供逻辑删除支持（毕竟生产环境的数据一般不物理删除，即使需要物理删除，一般也是将数据搬到其他地方保存）
  * 多表关联查询支持（史上最好的多表关联查询）
## 优点
  * 支持与spring整合，也支持单独使用。与spring整合不需要额外的包，仅仅在运行时有依赖，编译时并不依赖spring。
  * 极简的配置即可实现强大的功能。
  * 框架代码少，抽象适度，底层实现很容易理解。
  * 支持分页、乐观锁、逻辑删除、多表关联查询，更切合实践。
  * insert一条记录时自动返回id，无需任何配置。
  * 其他
## 局限性
  * 有些地方使用强制约定，比如要求所有的表提供主键，不太适合旧项目迁移过来。
  * 个别接口只支持mysql，虽然可以做到兼容其他数据库，由于时间问题没有去实现。有兴趣的可以贡献一下，谢谢！
  * 没有做多数据源支持。用户可以通过其他方式实现，比如创建多个数据源，多个JdbcTemplate。
  * 不支持联合主键。个人认为联合主键是一种不好的设计。
  * 不支持缓存。个人认为缓存涉及到业务逻辑，在service层使用缓存更合理。在使用mybatis时，缓存一般也是关闭的。
## 使用方式
  比如在spring项目中使用。
  1. 添加jar包依赖，包括本项目，以及fastjson、mysql驱动、slf4j。
  2. 在spring中配置数据源，配置SessionFactory（org.sirenia.session.SpringSessionFactory），
  配置JdbcTemplate（org.sirenia.template.JdbcTemplate）。
  3. 调用JdbcTemplate接口（或者获取Session调用Session的接口）。单表操作接口，用户的实体类必须使用Table注解。
  
