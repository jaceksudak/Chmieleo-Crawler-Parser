package pl.chmieleo.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.chmieleo.model.DBStructure;
import pl.chmieleo.model.DataSourceManager;
import pl.chmieleo.model.Hop;
import pl.chmieleo.model.Item;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class BaseShopParser {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";
    public static final String HOP_IMAGE_IN_SERVER_PATH = "some/path/to/file";
    public static final Pattern ITEM_WEIGHT_PATTERN = Pattern.compile("\\A0?,?[0-9]+ ?\\S?g$");
    public static final Pattern HOP_HARVEST_YEAR_PATTERN = Pattern.compile("\\A\\(?20[0-9][0-9]\\)?$");

    public static final List<String> COUNTRY_SHORT_NAME_LIST;
    public static final List<String> COUNTRY_LONG_NAME_LIST;

    static {
        String queryCountryName = "SELECT " +  DBStructure.COLUMN_COUNTRY_SHORT_NAME + ", " + DBStructure.COLUMN_COUNTRY_COUNTRY_NAME +
                " FROM " + DBStructure.TABLE_COUNTRY;
        DataSource dataSource = DataSourceManager.getDataSource();
        List<String> countryShortNameList = new ArrayList<>();
        List<String> countryLongNameList = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(queryCountryName)) {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                countryShortNameList.add(rs.getString(DBStructure.COLUMN_COUNTRY_SHORT_NAME));
                countryLongNameList.add(rs.getString(DBStructure.COLUMN_COUNTRY_COUNTRY_NAME));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        COUNTRY_SHORT_NAME_LIST = countryShortNameList;
        COUNTRY_LONG_NAME_LIST = countryLongNameList;
    }

    public final int shopID = setShopID();
    protected abstract int setShopID();

    public final String baseURI = setBaseURI();
    protected abstract String setBaseURI();

    public final String hopListURI = setHopListURI();
    protected abstract String setHopListURI();

    private final String itemFromListURLSelector = setItemFromListURLSelector();
    protected abstract String setItemFromListURLSelector();

    private final String itemFromListURLAttribute = setItemFromListURLAttribute();
    protected abstract String setItemFromListURLAttribute();


    public Stream<Item> parseAllOneCategoryItemsFromShop(String categoryUri) {
        return parseAllOneCategoryItemsFromShop(categoryUri, Integer.MAX_VALUE - 1);
    }

    public Stream<Item> parseAllOneCategoryItemsFromShop(String categoryUri, int limit) {
        List<String> allItemsUrls = getAllInCategoryItemsUrls(categoryUri, limit);
        Stream<Item> stream = null;
        if( categoryUri.equals(hopListURI) ) {
            stream = allItemsUrls.stream()
                    .map(url -> (Item)parseHop(url))
                    .filter(Item::isValid);
        }
        /*else if ( categoryUri.equals(yeastURI) ) {
            stream = allItemsUrls.stream()
                    .map(url -> (Item)parseYeast(url))
                    .filter(Item::isValid);
        } .... place for new categories ....  */
        return stream;
    }

    public Hop parseHop(String itemURL) {
        Document doc = fetchDocument(itemURL);
        Hop.HopBuilder hopBuilder = new Hop.HopBuilder();
        if(doc == null) {
            return hopBuilder.valid(false).build();
        }
        parseItemImage(doc, hopBuilder);
        parseItemURI(itemURL, hopBuilder);
        parseItemPrice(doc, hopBuilder);
        parseItemAvailability(doc, hopBuilder);
        if( !parseHopVariety(doc, hopBuilder) )
            parseHopVarietyAltHook(doc, hopBuilder);
        if( !parseHopCountry(doc, hopBuilder) )
            parseHopCountryAltHook(doc, hopBuilder);
        if( !parseHopNetWeight(doc, hopBuilder) )
            parseHopNetWeightAltHook(doc, hopBuilder);
        if( !parseHopDescription(doc, hopBuilder) )
            parseHopDescriptionAltHook(doc, hopBuilder);
        if( !parseHopHarvestYear(doc, hopBuilder) )
            parseHopHarvestYearAltHook(doc, hopBuilder);
        if( !parseHopForm(doc, hopBuilder) )
            parseHopFormAltHook(doc, hopBuilder);
        parseHopPurposeHook(doc, hopBuilder);
        parseHopAlphaAcidsHook(doc, hopBuilder);

        checkIfValidHop(hopBuilder);
        return hopBuilder.build();
    }

    protected abstract void parseItemAvailability(Document doc, Item.Builder<?> builder);

    protected abstract void parseItemPrice(Document doc, Item.Builder<?> builder);

    protected abstract void parseItemURI(String itemURL, Item.Builder<?> builder);

    protected abstract void parseItemImage(Document doc, Item.Builder<?> builder);


    protected abstract boolean parseHopVariety(Document doc, Hop.HopBuilder hopBuilder);

    protected void parseHopVarietyAltHook(Document doc, Hop.HopBuilder hopBuilder) {}

    protected abstract boolean parseHopCountry(Document doc, Hop.HopBuilder hopBuilder);

    protected void parseHopCountryAltHook(Document doc, Hop.HopBuilder hopBuilder) {}

    protected abstract boolean parseHopNetWeight(Document doc, Hop.HopBuilder hopBuilder);

    protected void parseHopNetWeightAltHook(Document doc, Hop.HopBuilder hopBuilder) {}

    protected abstract boolean parseHopDescription(Document doc, Hop.HopBuilder hopBuilder);

    protected void parseHopDescriptionAltHook(Document doc, Hop.HopBuilder hopBuilder) {}

    protected abstract boolean parseHopHarvestYear(Document doc, Hop.HopBuilder hopBuilder);

    protected void parseHopHarvestYearAltHook(Document doc, Hop.HopBuilder hopBuilder) {}

    protected abstract boolean parseHopForm(Document doc, Hop.HopBuilder hopBuilder);

    protected void parseHopFormAltHook(Document doc, Hop.HopBuilder hopBuilder) {}

    protected void parseHopPurposeHook(Document doc, Hop.HopBuilder hopBuilder) {}

    protected void parseHopAlphaAcidsHook(Document doc, Hop.HopBuilder hopBuilder) {}

    private void checkIfValidHop(Hop.HopBuilder hopBuilder) {
        checkIfItemValid(hopBuilder);
        if( hopBuilder.getCountry().equals("ND") ||
                hopBuilder.getNetWeight() == 0 ||
                hopBuilder.getHarvestYear() == 0 ||
                hopBuilder.getHopForm() == Hop.HopForm.MISSING
                ) {
            hopBuilder.valid(false);
        }
    }

    private void checkIfItemValid(Item.Builder<?> itemBuilder) {
        if( itemBuilder.getVariety().equals("missing") ||
                itemBuilder.getImage().equals("missing") ||
                itemBuilder.getCurrentPrice() == 0.0 ||
                itemBuilder.getUri().equals("")
                ) {
            itemBuilder.valid(false);
        }
    }

    protected void removeRedundantFromHopTitle(Iterator<String> iterator, Hop.HopBuilder builder) {
        while(iterator.hasNext()) {
            String s = iterator.next();
            switch (s.toLowerCase()) {
                case "szyszka":
                case "szyszki":
                    builder.hopForm(Hop.HopForm.WHOLELEAF);
                    iterator.remove();
                    break;
                case "granulat":
                case "pellet":
                    builder.hopForm(Hop.HopForm.PELLET);
                    iterator.remove();
                    break;
                case "chmiel":
                case "promocja":
                case "wyprzedaż":
                case "wyprzedaż!":
                    iterator.remove();
                    break;
            }
        }
    }

    protected int parseWeightFromPattern(String netWeightString) {
        int multiplier = 1;
        if(netWeightString.toLowerCase().contains("k")) {
            multiplier = 1000;
        }
        netWeightString = netWeightString.replaceAll("[^0-9.,]","");
        netWeightString = netWeightString.replaceAll(",",".");
        return (int) (Double.parseDouble(netWeightString) * multiplier);
    }


    private Document fetchDocument(String itemURL) {
        Document doc;
        System.out.print("Fetching " + itemURL + " --> ");
        try {
            doc = Jsoup.connect(itemURL).timeout(10*1000).userAgent(USER_AGENT).referrer("http://www.google.com").get();
        } catch (IOException e) {
            System.out.println("fail");
            e.printStackTrace();
            return null;
        }
        System.out.println("success");
        return doc;
    }

    private List<String> getAllInCategoryItemsUrls(String categoryUri, int limit) {
        int pageNumber = 1;
        List<String> allHopsUrls = new ArrayList<>();
        List<String> firstPageList = getUrlsFromSinglePage(baseURI + categoryUri + 1);
        List<String> currentPageList = firstPageList;
        do {
            allHopsUrls.addAll(currentPageList);
            pageNumber++;
            currentPageList = getUrlsFromSinglePage(baseURI + categoryUri + pageNumber);
        } while ( ! currentPageList.equals(firstPageList) &&
                pageNumber <= limit );
        return allHopsUrls;
    }

    private List<String> getUrlsFromSinglePage(String url) {
        Document doc = fetchDocument(url);
        List<String> itemsUrls = new ArrayList<>();
        if(doc == null) {
            return itemsUrls;
        }
        Elements elements = doc.select(itemFromListURLSelector);
        for(Element element : elements) {
            itemsUrls.add(element.attr(itemFromListURLAttribute));
        }
        return itemsUrls;
    }
}
