package pl.chmieleo.model;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import javax.sql.DataSource;
import java.io.*;
import java.util.Properties;

public class DataSourceManager {
    private volatile static DataSource INSTANCE;

    private DataSourceManager() {}

    public static DataSource getDataSource() {
        if(INSTANCE == null) {
            synchronized (DataSourceManager.class) {
                if(INSTANCE == null) {
                    INSTANCE = loadDataSource();
                }
            }
        }
        return INSTANCE;
    }

    private static MysqlDataSource loadDataSource() {
        MysqlDataSource mysqlDS = new MysqlDataSource();
        Properties props = new Properties();
        try(FileInputStream inputStream = new FileInputStream("db.properties")) {
            props.load(inputStream);
        } catch (IOException e ) {
            e.printStackTrace();
        }
        mysqlDS.setURL( "jdbc:mysql://" + props.getProperty("jdbc.url") );
        mysqlDS.setUser( props.getProperty("jdbc.username") );
        mysqlDS.setPassword( props.getProperty("jdbc.password") );
        return mysqlDS;
    }
}
