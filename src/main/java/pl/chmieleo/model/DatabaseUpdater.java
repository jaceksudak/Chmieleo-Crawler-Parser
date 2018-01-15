package pl.chmieleo.model;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;

public class DatabaseUpdater {

    private static final String QUERY_COUNTRY_ID = "SELECT " + DBStructure.TABLE_COUNTRY + "." +DBStructure.COLUMN_COUNTRY_ID + " FROM " +DBStructure.TABLE_COUNTRY +
            " WHERE " +DBStructure.TABLE_COUNTRY + "." +DBStructure.COLUMN_COUNTRY_SHORT_NAME + " = ? ORDER BY " +DBStructure.COLUMN_ITEMS_IN_SHOPS_ID + " DESC LIMIT 1";
    private static final String INSERT_COUNTRY = "INSERT INTO " +DBStructure.TABLE_COUNTRY + "(" +
           DBStructure.COLUMN_COUNTRY_SHORT_NAME + ") VALUES (?)";

    private static final String QUERY_HOP_ID = "SELECT " +DBStructure.TABLE_HOPS + "." +DBStructure.COLUMN_HOPS_ID + " FROM " +DBStructure.TABLE_HOPS +
            " INNER JOIN " +DBStructure.TABLE_ITEMS + " ON " +DBStructure.TABLE_HOPS + "." +DBStructure.COLUMN_HOPS_ID + " = " +DBStructure.TABLE_ITEMS + "." +DBStructure.COLUMN_ITEMS_ID +
            " WHERE " +DBStructure.COLUMN_HOPS_HARVEST_YEAR + " = ? AND " +DBStructure.COLUMN_HOPS_FORM + " = ? AND " +
           DBStructure.COLUMN_ITEMS_VARIETY + " = ? AND " +DBStructure.COLUMN_ITEMS_COUNTRY_ID + " = ? AND " +DBStructure.COLUMN_ITEMS_NET_WEIGHT +
            " = ? ORDER BY " +DBStructure.COLUMN_ITEMS_IN_SHOPS_ID + " DESC LIMIT 1";
    private static final String INSERT_ITEM = "INSERT INTO " +DBStructure.TABLE_ITEMS + "(" +
           DBStructure.COLUMN_ITEMS_CATEGORY_ID + ", " +DBStructure.COLUMN_ITEMS_ITEM_NAME + ", " +DBStructure.COLUMN_ITEMS_VARIETY + ", " +DBStructure.COLUMN_ITEMS_COUNTRY_ID + ", " +
           DBStructure.COLUMN_ITEMS_NET_WEIGHT + ", " +DBStructure.COLUMN_ITEMS_DESCRIPTION + ", " +DBStructure.COLUMN_ITEMS_MANUFACTURER +
            ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_HOP = "INSERT INTO " +DBStructure.TABLE_HOPS + "(" +
           DBStructure.COLUMN_HOPS_ID + ", " +DBStructure.COLUMN_HOPS_HARVEST_YEAR + ", " +DBStructure.COLUMN_HOPS_FORM + ", " +
           DBStructure.COLUMN_HOPS_PURPOSE + ", " +DBStructure.COLUMN_HOPS_ALPHA_ACIDS + ") VALUES (?, ?, ?, ?, ?)";

    private static final String QUERY_ITEM_IN_SHOP_ID = "SELECT " +DBStructure.COLUMN_ITEMS_IN_SHOPS_ID + " FROM " +DBStructure.TABLE_ITEMS_IN_SHOPS +
            " WHERE " +DBStructure.COLUMN_ITEMS_IN_SHOPS_ITEM_ID + " = ? AND " +DBStructure.COLUMN_ITEMS_IN_SHOPS_SHOP_ID +
            " = ? ORDER BY " +DBStructure.COLUMN_ITEMS_IN_SHOPS_ID + " DESC LIMIT 1";
    private static final String INSERT_ITEM_IN_SHOP = "INSERT INTO " +DBStructure.TABLE_ITEMS_IN_SHOPS + "(" +
           DBStructure.COLUMN_ITEMS_IN_SHOPS_ITEM_ID + ", " +DBStructure.COLUMN_ITEMS_IN_SHOPS_SHOP_ID + ", " +DBStructure.COLUMN_ITEMS_IN_SHOPS_ITEM_URL +
            ") VALUES (?, ?, ?)";

    private static final String QUERY_ITEMS_CURRENT_STATE = "SELECT " +DBStructure.COLUMN_ITEMS_CURRENT_STATES_PRICE + "," +DBStructure.COLUMN_ITEMS_CURRENT_STATES_AVAILABILITY +
            " FROM " +DBStructure.TABLE_ITEMS_CURRENT_STATES + " WHERE " +DBStructure.COLUMN_ITEMS_CURRENT_STATES_ITEM_IN_SHOP_ID +
            " = ? ORDER BY " +DBStructure.COLUMN_ITEMS_CURRENT_STATES_ID + " DESC LIMIT 1";
    private static final String INSERT_ITEMS_CURRENT_STATE = "INSERT INTO " +DBStructure.TABLE_ITEMS_CURRENT_STATES + "(" +
           DBStructure.COLUMN_ITEMS_CURRENT_STATES_ITEM_IN_SHOP_ID + ", " +DBStructure.COLUMN_ITEMS_CURRENT_STATES_PRICE + ", " +DBStructure.COLUMN_ITEMS_CURRENT_STATES_AVAILABILITY + ", " +
           DBStructure.COLUMN_ITEMS_CURRENT_STATES_UPDATE_DATE + ") VALUES (?, ?, ?, ?)";


    private DataSource dataSource;

    public DatabaseUpdater(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean addOrUpdateItem(Hop item, int shopID, String url) throws SQLException {
        int countryID = getOrAddCountry(item);
        // sprawdzenie czy istnieje country, dodanie jesli nie
        int itemID = getOrAddItem(item, countryID);
        // czy jest Hop z tym variety, form i year, country, net weight, dodanie jesli nie
        int itemInShopID = getOrAddItemToShop(itemID, shopID, url);
        // czy jest w danym sklepi, jak nie to dodaj
        return getOrAddItemState(item, itemInShopID);
        // sprawdzenie czy stan sie zeminil, jak tak to dodanie nowego
    }

    private int getOrAddCountry(Item item) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY_COUNTRY_ID);
             PreparedStatement ps2 = conn.prepareStatement(INSERT_COUNTRY, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getCountry());
            int countryID = 0;
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    countryID = rs.getInt(DBStructure.COLUMN_COUNTRY_ID);
                }
            }
            if(countryID != 0) {                        // CountryID retrieved from DB
                return countryID;
            } else {                                    // Adding new country to DB
                ps2.setString(1, item.getCountry());
                int affectedRows = ps2.executeUpdate();
                if(affectedRows == 0) {
                    throw new SQLException("Couldn't insert new country! (1)");
                }
                try (ResultSet generatedKeys = ps2.getGeneratedKeys()) {
                    if(generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Couldn't insert new country! (2)");
                    }
                }
            }
        }
    }

    private int getOrAddItem(Hop item, int countryID) throws SQLException {
        int itemID = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY_HOP_ID);
             PreparedStatement ps2 = conn.prepareStatement(INSERT_ITEM, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement ps3 = conn.prepareStatement(INSERT_HOP)) {
            ps.setInt(1, item.getHarvestYear());
            ps.setString(2, item.getHopForm().toString());
            ps.setString(3, item.getVariety());
            ps.setInt(4, countryID);
            ps.setInt(5, item.getNetWeight());
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    itemID = rs.getInt(DBStructure.COLUMN_ITEMS_ID);
                }
            }
            if(itemID != 0) {                           // ItemID retrieved form DB
                return itemID;
            } else {                                    // ItemID not in a DB, adding new Item to DB
                ps2.setInt(1, item.getCategoryId());
                ps2.setString(2, item.getName());
                ps2.setString(3, item.getVariety());
                ps2.setInt(4, countryID);
                ps2.setInt(5, item.getNetWeight());
                ps2.setString(6, item.getDescription());
                ps2.setString(7, item.getManufacturer());
                int affectedRows = ps2.executeUpdate();
                if(affectedRows == 0) {
                    throw new SQLException("Couldn't insert new item! (1)");
                }
                try (ResultSet generatedKeys = ps2.getGeneratedKeys()) {
                    if(generatedKeys.next()) {
                        itemID = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Couldn't insert new item! (2)");
                    }
                }
                ps3.setInt(1, itemID);
                ps3.setInt(2, item.getHarvestYear());
                ps3.setString(3, item.getHopForm().toString());
                ps3.setString(4, item.getPurpose().toString());
                ps3.setDouble(5, item.getAlphaAcids());
                affectedRows = ps3.executeUpdate();
                if(affectedRows == 0) {
                    throw new SQLException("Couldn't insert new hop1!");
                }
                return itemID;
            }
        }
    }

//  private boolean getOrAddItem(Malt item, int countryID) throws SQLException {}
//  private boolean getOrAddItem(Unmalt item, int countryID) throws SQLException {}
//  private boolean getOrAddItem(Yeast item, int countryID) throws SQLException {}

    private int getOrAddItemToShop(int itemID, int shopID, String url) throws SQLException {
        int itemInShopID = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY_ITEM_IN_SHOP_ID);
             PreparedStatement ps2 = conn.prepareStatement(INSERT_ITEM_IN_SHOP, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemID);
            ps.setInt(2, shopID);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    itemInShopID = rs.getInt(DBStructure.COLUMN_ITEMS_IN_SHOPS_ID);
                }
            }
            if(itemInShopID != 0) {                 // ItemInShopID retrieved from DB
                return itemInShopID;
            } else {                                  // ItemInShopID not in DB, inserting new ItemInShop to DB
                ps2.setInt(1, itemID);
                ps2.setInt(2, shopID);
                ps2.setString(3, url);
                int affectedRows = ps2.executeUpdate();
                if(affectedRows == 0) {
                    throw new SQLException("Couldn't insert new item in shop1!");
                }
                try (ResultSet generatedKeys = ps2.getGeneratedKeys()) {
                    if(generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Couldn't insert new item in shop2!");
                    }
                }
            }
        }
    }

    private boolean getOrAddItemState(Item item, int itemInShopID) throws SQLException {
        double price = 0.0;
        boolean availability = false;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY_ITEMS_CURRENT_STATE);
             PreparedStatement ps2 = conn.prepareStatement(INSERT_ITEMS_CURRENT_STATE, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, itemInShopID);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) {
                    price = rs.getDouble(DBStructure.COLUMN_ITEMS_CURRENT_STATES_PRICE);
                    availability = rs.getBoolean(DBStructure.COLUMN_ITEMS_CURRENT_STATES_AVAILABILITY);
                }
            }
            if( (price == item.getCurrentPrice()) && (availability == item.isCurrentAvailability()) ) {
                return false;                       // ItemState didn't changed, not adding to DB
            } else {                                // ItemState changed, adding new state to DB
                ps2.setInt(1, itemInShopID);
                ps2.setDouble(2, item.getCurrentPrice());
                ps2.setInt(3, item.isCurrentAvailability() ? 1 : 0);
                ps2.setDate(4, Date.valueOf(LocalDate.now()));
                int affectedRows = ps2.executeUpdate();
                if(affectedRows == 0) {
                    throw new SQLException("Couldn't insert new item state1!");
                } else {
                    return true;
                }
            }
        }
    }





    public static void main(String[] args) {

        Item item = new Hop(125, "Marynka",
                "PL", 80, "21asldkas;1ldka;lsdk asd;lk asd ;las", "Moja fa1231bryka",
                2015, Hop.HopForm.PELLET, Hop.Purpose.AROMA, 8.2, 16.00, true);
        Shop shop = new Shop(2, "Esencje", "hhttpp:/costam", null);
        DatabaseUpdater dbu = new DatabaseUpdater(DataSourceManager.getDataSource());

        Item item3 = new Hop.HopBuilder().harvestYear(2156).categoryId(3).country("kjkjl").harvestYear(3121).purpose(Hop.Purpose.AROMA).currentPrice(12.0).alphaAcids(23).country("PL").hopForm(Hop.HopForm.PELLET).build();


        try {
            dbu.addOrUpdateItem((Hop)item,2,"/.,/.lk;lxkcv");
            dbu.addOrUpdateItem((Hop)item3,2,"/.,/.lk;lxkcv");
//            int x = dbu.getOrAddCountry(item);
//            System.out.println(x);
//            int y = dbu.getOrAddItemToShop(item, shop, "asdasd");
//            System.out.println(y);
//            System.out.println(dbu.getOrAddItemState(item, y));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println(item);
//        try {
//            if(dbu.dataSource.getConnection().isValid(1000))
//                System.out.println("Spoko");
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        Statement statement = dbu.dataSource.getConnection().createStatement();
//        statement.

    }
}
