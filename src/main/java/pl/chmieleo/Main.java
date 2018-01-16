package pl.chmieleo;

import pl.chmieleo.controller.BaseShopParser;
import pl.chmieleo.controller.TwojbrowarShopParser;
import pl.chmieleo.model.*;

import java.sql.SQLException;
import java.util.stream.Stream;


public class Main {

    public static void main(String[] args) {
        BaseShopParser parser = new TwojbrowarShopParser();

        DatabaseUpdater dbUpdater = new DatabaseUpdater(DataSourceManager.getDataSource());
        Stream<Item> stream = parser.parseAllOneCategoryItemsFromShop(parser.hopListURI);
        stream.forEach(item -> {
            try {
                dbUpdater.addOrUpdateItem((Hop)item,parser.shopID, parser.baseURI + item.getUri());
                System.out.println(item);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
