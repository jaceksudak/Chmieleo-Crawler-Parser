package pl.chmieleo;

import pl.chmieleo.controller.TwojbrowarShopParser;
import pl.chmieleo.model.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;


public class Main {

    public static void main(String[] args) {
        TwojbrowarShopParser parser = new TwojbrowarShopParser();
        DatabaseUpdater dbUpdater = new DatabaseUpdater(DataSourceManager.getDataSource());

        Stream<Item> stream = parser.parseAllOneCategoryItemsFromShopToStream(TwojbrowarShopParser.HOP_URI);
        stream.forEach(item -> {
            try {
                dbUpdater.addOrUpdateItem((Hop)item,parser.SHOP_ID, TwojbrowarShopParser.BASE_URI + item.getUri());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
