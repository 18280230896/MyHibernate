package com.llg.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {

    private static String driver;
    private static String url;
    private static String username;
    private static String password;

    /**
     * 获取数据库连接
     *
     * @return 连接对象
     */
    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    //静态初始化块，读取配置信息并加载驱动
    static {
        Properties prop = new Properties();
        //读取配置文件信息
        try {
            prop.load(DBUtil.class.getClassLoader().getResourceAsStream("db.properties"));
        } catch (IOException e) {
            try {
                throw new FileNotFoundException("找不到src下的db.properties文件");
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        //将配置文件信息赋值给成员变量
        driver = prop.getProperty("jdbc.driver");
        url = prop.getProperty("jdbc.url");
        username = prop.getProperty("jdbc.username");
        password = prop.getProperty("jdbc.password");
        if (driver == null || url == null || username == null || password == null) {
            System.err.println("请按如下格式书写db.properties文件：");
            System.err.println("jdbc.driver=XXXX");
            System.err.println("jdbc.urlr=XXXX");
            System.err.println("jdbc.username=XXXX");
            System.err.println("jdbc.password=XXXX");
            throw new RuntimeException();
        }
        //加载驱动
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
