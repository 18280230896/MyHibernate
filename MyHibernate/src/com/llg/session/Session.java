package com.llg.session;

import com.llg.annotation.Column;
import com.llg.annotation.ID;
import com.llg.annotation.Table;
import com.llg.exception.ClassNoMappingException;
import com.llg.exception.ClassNoMappingPrimaryKeyException;
import com.llg.exception.PrimaryKeyNotUniqueException;
import com.llg.util.DBUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 可以通过此类对象的各种方法操作数据库
 */
public class Session {
    private Connection conn;
    private PreparedStatement ps;
    private Statement stat;
    private ResultSet rs;

    public Session() {
        //在无参构造器中初始化Connection对象
        conn = DBUtil.getConnection();
    }

    /**
     * 判断表是否存在，不存在则创建
     *
     * @param clazz
     * @param <T>
     */
    private <T> void createTable(Class<T> clazz) {
        //获取该类的Table注解
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) throw new ClassNoMappingException(clazz.getName() + ClassNoMappingException.ERR_MSG);
        try {
            //判断表是否存在
            rs = conn.createStatement().executeQuery("show tables like '" + table.value() + "'");
            if (rs.next()) return;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //创建表
        try {
            conn.createStatement().executeUpdate(getCreateTableSql(clazz));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static <T> String getCreateTableSql(Class<T> clazz) {
        StringBuilder sb = new StringBuilder("create table ");
        sb.append(clazz.getAnnotation(Table.class).value());
        sb.append("(");
        boolean existId = false;
        //遍历所有字段
        for (Field field : clazz.getDeclaredFields()) {
            //获得ID注解
            ID id = field.getAnnotation(ID.class);
            if (id != null) {
                if (existId)
                    throw new PrimaryKeyNotUniqueException(clazz.getName() + PrimaryKeyNotUniqueException.ERR_MSG);
                existId = true;
                sb.append(id.value());
                sb.append(" int primary key auto_increment,");
                continue;
            }
            //获得Column注解
            Column col = field.getAnnotation(Column.class);
            if (col != null) {
                sb.append(col.value());
                sb.append(" ");
                sb.append(typeHandler(field.getType()));
                sb.append(",");
            }
        }
        String sql = sb.toString();
        sql = sql.substring(0, sql.length() - 1);
        sql += ")";
        return sql;
    }

    private static String typeHandler(Class<?> clazz) {
        String type = null;
        switch (clazz.getName()) {
            case "int":
            case "java.lang.Integer":
                type = "int";
                break;
            case "float":
            case "java.lang.Float":
                type = "float(10.2)";
                break;
            case "double":
            case "java.lang.Double":
                type = "double(12.4)";
                break;
            case "java.sql.Timestamp":
                type = "timestamp";
                break;
            case "java.util.Date":
                type = "date";
                break;
            case "java.lang.String":
            default:
                type = "varchar(100)";
                break;
        }
        return type;
    }

    /**
     * 开启事务
     */
    public void beginTransition() {
        try {
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 提交事务
     */
    public void commit() {
        try {
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        try {
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * 删除指定类所映射表的指定记录
     *
     * @param id    要删除的记录主键
     * @param clazz 要删除的表所对应的类
     * @param <T>
     * @return
     */
    public <T> boolean delete(int id, Class<T> clazz) {
        if (clazz == null) throw new NullPointerException();
        createTable(clazz);
        //获取该类的Table注解
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) throw new ClassNoMappingException(clazz.getName() + ClassNoMappingException.ERR_MSG);
        String primaryKey = null;
        //遍历所有属性，获得主键列
        for (Field field : clazz.getDeclaredFields()) {
            //获得ID注解
            ID idAnn = field.getAnnotation(ID.class);
            if (idAnn != null) {
                if (primaryKey != null)
                    throw new PrimaryKeyNotUniqueException(clazz.getName() + PrimaryKeyNotUniqueException.ERR_MSG);
                primaryKey = idAnn.value();
            }
        }
        //拼写sql
        String sql = "delete from " + table.value() + " where " + primaryKey + "=" + id;
        boolean success = false;
        try {
            //获得statement对象
            stat = conn.createStatement();
            //执行sql
            success = stat.executeUpdate(sql) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }

    /**
     * 修改指定传入的传入对象唯一表示所对应的记录，修改为该对象的当前属性值
     *
     * @param t   将数据库中记录修改为t的当前属性值
     * @param <T>
     * @return
     */
    public <T> boolean update(T t) {
        if (t == null) throw new NullPointerException();
        createTable(t.getClass());
        //定义一个标记，记录是否更新成功
        boolean b = false;
        //获得该类的Table注解对象
        Table table = t.getClass().getAnnotation(Table.class);
        if (table == null) throw new ClassNoMappingException(t.getClass().getName() + ClassNoMappingException.ERR_MSG);
        //获得sql
        String sql = getUpdateColumns(t);
        try {
            //获取ps对象
            ps = conn.prepareStatement(sql);
            //填充占位符
            fillSeat(ps, t);
            //执行
            b = ps.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * 将指定对象插入数据库表中
     *
     * @param t   要插入的数据
     * @param <T> 要插入的数据类型
     * @return 是否添加成功
     */
    public <T> boolean add(T t) {
        if (t == null) throw new NullPointerException();
        createTable(t.getClass());
        //获取该类的Table注解
        Table table = t.getClass().getAnnotation(Table.class);
        if (table == null) throw new ClassNoMappingException(t.getClass().getName() + ClassNoMappingException.ERR_MSG);
        //定义一个变量表示是否添加成功
        boolean b = false;
        //拼写sql
        StringBuilder sql = new StringBuilder("insert into " + table.value() + "(");
        sql.append(getColumns(t.getClass()));
        sql.append(") values(");
        sql.append(getColumnsSeat(t.getClass()));
        sql.append(")");
        try {
            //获得statement对象
            ps = conn.prepareStatement(sql.toString());
            fillSeat(ps, t);
            //执行sql
            b = ps.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return b;
    }

    /**
     * 获得指定类所映射表的所有数据并返回一个包含所有数据的list集合
     *
     * @param clazz 指定要查询的表所映射的类
     * @param <T>
     * @return
     */
    public <T> List<T> getList(Class<T> clazz) {
        if (clazz == null) throw new NullPointerException();
        createTable(clazz);
        //获取该类上的Table注解
        Table table = existAnnotation(clazz, Table.class);
        if (table == null) throw new ClassNoMappingException(clazz.getName() + ClassNoMappingException.ERR_MSG);
        //拼写sql
        StringBuilder sb = getSelectColumns(clazz);
        //追加表名
        sb.append(" from " + table.value());
        List<T> list = new ArrayList<>();
        try {
            //获取statement对象
            stat = conn.createStatement();
            //执行sql
            rs = stat.executeQuery(sb.toString());
            while (rs.next()) {
                //将结果封装成指定对象并添加到集合中
                list.add(CreateObject(rs, clazz));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 根据指定id查找数据并封装成指定对象返回
     *
     * @param id    要查找的id
     * @param clazz
     * @param <T>   将查询结果封装成T类型对象返回
     * @return
     */
    public <T> T get(int id, Class<T> clazz) {
        if (clazz == null) throw new NullPointerException();
        createTable(clazz);
        //获取该类上的Table注解
        Table table = existAnnotation(clazz, Table.class);
        if (table == null) throw new ClassNoMappingException(clazz.getName() + ClassNoMappingException.ERR_MSG);
        //拼写sql
        StringBuilder sb = getSelectColumns(clazz);
        //追加表名
        sb.append(" from " + table.value() + " where ");
        //追加筛选条件
        sb.append(getID(clazz) + "=?");
        String sql = sb.toString();
        T t = null;
        try {
            //获取statement对象
            ps = conn.prepareStatement(sql);
            //填充占位符
            ps.setObject(1, id);
            //执行sql
            rs = ps.executeQuery();
            if (!rs.next()) return null;
            //将结果封装成指定对象
            t = CreateObject(rs, clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }


    /**
     * 将指定对象的属性值填充到statement中的占位符
     *
     * @param statement
     * @param t
     * @param <T>
     */
    private static <T> void fillSeat(PreparedStatement statement, T t) {
        int index = 1;
        //遍历对象的所有属性
        for (Field field : t.getClass().getDeclaredFields()) {
            //判断是否有Column注解标记
            if (field.getAnnotation(Column.class) == null) break;
            try {
                field.setAccessible(true);
                statement.setObject(index++, field.get(t));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 将指定结果集的一行封装成指定对象并返回该对象
     *
     * @param rs    结果集
     * @param clazz
     * @param <T>   指定类型
     * @return
     */
    private static <T> T CreateObject(ResultSet rs, Class<T> clazz) {
        if (clazz == null) throw new NullPointerException();
        T t = null;
        try {
            //创建给类型对象
            t = clazz.getConstructor().newInstance();
            //遍历该类字段
            for (Field field : clazz.getDeclaredFields()) {
                //获取Column注解
                Column column = field.getAnnotation(Column.class);
                if (column == null) break;
                //设置该字段值
                field.setAccessible(true);
                field.set(t, rs.getObject(column.value()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /**
     * 判断指定类上是否有指定注解，有则返回该注解对象，否则返回null
     *
     * @param clazz 指定的类
     * @param annt  指定的注解
     * @param <T>
     * @param <U>
     * @return
     */
    private static <T, U extends Annotation> U existAnnotation(Class<T> clazz, Class<U> annt) {
        if (clazz == null || annt == null) throw new NullPointerException();
        return clazz.getAnnotation(annt);
    }

    /**
     * 获取指定类所映射表的唯一标识列名称
     *
     * @param clazz
     * @return
     */
    private static String getID(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException();
        String id = "";
        //遍历该类所有字段，找到第一个用ID注解标识的列名称并返回
        for (Field field : clazz.getDeclaredFields()) {
            //获取ID注解
            ID i = field.getAnnotation(ID.class);
            if (i != null) {
                id = i.value();
                break;
            }
        }
        if ("".equals(id))
            throw new ClassNoMappingPrimaryKeyException(clazz.getName() + ClassNoMappingPrimaryKeyException.ERR_MSG);
        return id;
    }

    /**
     * 返回指定类的所有被Column注解标记的字段所对应的占位符表示
     *
     * @param clazz
     * @return
     */
    private static StringBuilder getColumnsSeat(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException();
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        //遍历所有字段
        for (Field field : clazz.getDeclaredFields()) {
            //判断该字段是否有Column注解
            if (field.getAnnotation(Column.class) != null) {
                if (isFirst) isFirst = false;
                else sb.append(",");
                sb.append("?");
            }
        }
        return sb;
    }

    /**
     * 获取指定类的被Column注解标记的字段所对应的列名称的StringBuilder
     *
     * @param clazz
     * @return
     */
    private static StringBuilder getColumns(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        //遍历该类所有字段
        for (Field field : clazz.getDeclaredFields()) {
            //获取该字段上的Column注解
            Column c = field.getAnnotation(Column.class);
            if (c == null) break;
            if (isFirst) isFirst = false;
            else sb.append(",");
            sb.append(c.value());
        }
        return sb;
    }

    /**
     * 获取指定类的被Column注解标记的字段所对应的列名称的StringBuilder，并包含了select
     *
     * @param clazz
     * @return
     */
    private static StringBuilder getSelectColumns(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException();
        StringBuilder sb = new StringBuilder("select ");
        sb.append(getColumns(clazz));
        return sb;
    }

    /**
     * 将指定对象转化为更新的sql语句
     *
     * @param t
     * @param <T>
     * @return
     */
    private static <T> String getUpdateColumns(T t) {
        if (t == null) throw new NullPointerException();
        //获取class对象
        Class<?> clazz = t.getClass();
        //获得Table注解
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) throw new ClassNoMappingException(clazz.getName());
        //拼写sql
        StringBuilder sb = new StringBuilder("update " + table.value() + " set ");
        ID idAnn = null;
        Field idField = null;
        boolean isFirst = true;
        //遍历属性
        for (Field field : clazz.getDeclaredFields()) {
            //获得ID注解
            ID idan = field.getAnnotation(ID.class);
            if (idan != null) {
                if (idAnn != null)
                    throw new PrimaryKeyNotUniqueException(clazz.getName() + PrimaryKeyNotUniqueException.ERR_MSG);
                idAnn = idan;
                idField = field;
            }
            //获得Column注解
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                if (isFirst) isFirst = false;
                else sb.append(",");
                sb.append(column.value() + "=?");
            }
        }
        //追加where条件
        idField.setAccessible(true);
        try {
            sb.append(" where " + idAnn.value() + "=" + idField.get(t));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public void close() {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stat != null) {
            try {
                stat.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
