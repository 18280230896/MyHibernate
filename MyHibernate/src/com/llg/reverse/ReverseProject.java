package com.llg.reverse;

import com.llg.util.DBUtil;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReverseProject {
    private static Connection conn;
    private static PreparedStatement ps;
    private static ResultSet rs;
    private static final String pkg = "com.llg.bean";

    public static void reverse() {
        conn = DBUtil.getConnection();
        //获取所有表并遍历创建bean
        for (String s : getAllTableName()) {
            try {
                createBean(getTableDesc(s));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //关闭资源
        close();
    }


    /**
     * 创建bean
     *
     * @param bean
     */
    private static void createBean(Bean bean) throws IOException {
//        String path = ReverseProject.class.getClassLoader().getResource("db.properties").getPath();
//        String absolutePath = new File(path).getParentFile().getAbsolutePath() +File.separator+ pkg.replace(".",File.separator)+File.separator+bean.getClassName()+".java";
        String absolutePath = "D:\\Projects\\MyHibernate\\MyHibernate\\src"+File.separator+ pkg.replace(".",File.separator)+File.separator+bean.getClassName()+".java";
        File file = new File(absolutePath);
        File p = file.getParentFile();
        //判断父目录是否存在，不存在则创建
        if (!p.exists()) p.mkdirs();
        //判断该类文件是否存在，不存在则创建
        if (file.exists()) return;
        file.createNewFile();
        //创建打印流对象
        PrintWriter writer = new PrintWriter(new FileOutputStream(file), true);
        //输出包信息
        writer.println("package " + pkg + ";");
        writer.println();
        //输出import信息
        writer.println("import com.llg.annotation.Column;");
        writer.println("import com.llg.annotation.ID;");
        writer.println("import com.llg.annotation.Table;");
        writer.println();
        //输出类注解
        writer.println("@Table(\"" + getTableName(bean.getClassName()) + "\")");
        //输出类声明
        writer.println("public class " + bean.getClassName() + " {");
        StringBuilder conSb1 = new StringBuilder("\tpublic " + bean.getClassName() + "(");
        StringBuilder conSb2 = new StringBuilder();
        StringBuilder tosSb = new StringBuilder("\tpublic String toString() {\r\n\t\treturn \"" + bean.getClassName()+"{"+"\" +\r\n");
        boolean isFirst = true;
        String str = "";
        //遍历字段输出属性
        for (Field field : bean.getFields()) {
            //判断字段是否是主键
            if ("PRI".equalsIgnoreCase(field.getKey())) {
                //输出id注解
                writer.println("\t@ID(\"" + field.getField() + "\")");
            }
            //输出column注解
            writer.println("\t@Column(\"" + field.getField() + "\")");
            //输出该字段的值
            writer.println("\tprivate " + typeHandler(field.getType()) + " " + field.getField() + ";");

            if (isFirst) {
                isFirst = false;
                str = ",";
            }
            else conSb1.append(", ");
            //拼接构造器参数
            conSb1.append(typeHandler(field.getType()) + " " + field.getField());
            //拼接构造器体
            conSb2.append("\t\tthis." + field.getField() + " = " + field.getField() + ";" + "\r\n");
            //拼接toString体
            tosSb.append("\t\t\t\""+str+" "+field.getField()+"=\" +" + field.getField()+" +\r\n");

        }
        writer.println();
        //输出空参构造器
        writer.println("\tpublic " + bean.getClassName() + "() {");
        writer.println("\t}");
        writer.println();

        //输出带参构造器
        writer.println(conSb1.toString() + ") {");
        writer.println(conSb2.toString());
        writer.println("\t}");
        writer.println();

        //再次遍历field输出getter setter
        for (Field field : bean.getFields()) {
            //get
            writer.println("\tpublic " + typeHandler(field.getType()) + " get" + getClassName(field.getField()) + "() {");
            writer.println("\t\treturn " + field.getField()+";");
            writer.println("\t}");
            writer.println();
            //set
            writer.println("\tpublic void set" + getClassName(field.getField()) + "(" + typeHandler(field.getType()) +" "+ field.getField()+") {");
            writer.println("\t\tthis." + field.getField() + " = " + field.getField()+";");
            writer.println("\t}");
            writer.println();
        }

        //toString
        writer.println("\t@Override");
        writer.write(tosSb.toString());
        writer.println("\t\t\"}\";");
        writer.println("\t}");

        //类结束括号
        writer.println("}");

    }

    private static String typeHandler(String type) {
        String t;
        if (type.startsWith("int")) t = "Integer";
        else if (type.startsWith("varchar")) t = "String";
        else if (type.startsWith("float")) t = "float";
        else if (type.startsWith("double")) t = "double";
        else t = "String";
        return t;
    }

    /**
     * 获取表结构
     *
     * @param tableName
     * @return
     */
    private static Bean getTableDesc(String tableName) {
        Bean bean = new Bean();
        bean.setClassName(getClassName(tableName));
        List<Field> list = new ArrayList<>();
        bean.setFields(list);
        try {
            ps = conn.prepareStatement("desc " + tableName);
            rs = ps.executeQuery();
            while (rs.next()) {
                Field field = new Field();
                field.setField(rs.getString(1));
                field.setType(rs.getString(2));
                field.setIsNull(rs.getString(3));
                field.setKey(rs.getString(4));
                field.setDef(rs.getString(5));
                field.setExtra(rs.getString(6));
                list.add(field);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bean;
    }

    /**
     * 通过表名获取className
     *
     * @param tableName
     * @return
     */
    private static String getClassName(String tableName) {
        //将首字母大写并返回
        return (tableName.charAt(0) + "").toUpperCase() + tableName.substring(1);
    }

    /**
     * 通过className获取表名
     *
     * @param className
     * @return
     */
    private static String getTableName(String className) {
        //将首字母小写并返回
        return (className.charAt(0) + "").toLowerCase() + className.substring(1);
    }

    /**
     * 获取所有表名
     *
     * @return
     * @throws SQLException
     */
    private static List<String> getAllTableName() {
        List<String> list = new ArrayList<>();
        try {
            ps = conn.prepareStatement("show tables");
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 关闭资源
     */
    private static void close() {
        if (rs != null) {
            try {
                rs.close();
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
