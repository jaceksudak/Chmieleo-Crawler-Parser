package pl.chmieleo;

import pl.chmieleo.controller.BaseShopParser;
import pl.chmieleo.controller.PiwnycraftShopParser;
import pl.chmieleo.controller.TwojbrowarShopParser;
import pl.chmieleo.model.*;

import java.sql.SQLException;
import java.util.stream.Stream;


public class Main {

    public static void main(String[] args) {
        BaseShopParser twojbrowarShopParser = new TwojbrowarShopParser();
        BaseShopParser piwnycraftShopParser = new PiwnycraftShopParser();

        start(piwnycraftShopParser);
        start(twojbrowarShopParser);
    }

    public static void start(BaseShopParser shopParser) {
        DatabaseUpdater dbUpdater = new DatabaseUpdater(DataSourceManager.getDataSource());
        Stream<Item> stream = shopParser.parseAllOneCategoryItemsFromShop(shopParser.hopListURI );
        stream.forEach(item -> {
            try {
                dbUpdater.addOrUpdateItem((Hop)item,shopParser.shopID, shopParser.baseURI + item.getUri());
                System.out.println("valid");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
