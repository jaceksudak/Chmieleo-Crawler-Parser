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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TwojbrowarShopParser {

    private static final String QUERY_COUNTRY_NAME = "SELECT " +  DBStructure.COLUMN_COUNTRY_SHORT_NAME + ", " + DBStructure.COLUMN_COUNTRY_COUNTRY_NAME +
            " FROM " + DBStructure.TABLE_COUNTRY;

    public static final List<String> COUNTRY_SHORT_NAME_LIST;
    public static final List<String> COUNTRY_LONG_NAME_LIST;

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";
    public static final int SHOP_ID = 1;
    public static final String BASE_URI = "http://twojbrowar.pl/";

    public static final String HOP_URI = "pl/10-chmiel?p=";
    public static final String HOP_IMAGE_PATH = "some/path/to/file";
    public static final String ITEM_FROM_LIST_URI_SELECTOR = "a.product_img_link";
    public static final String ITEM_FROM_LIST_URI_ATTRIBUTE = "href";

    public static final String ITEM_PRICE_SELECTOR = "span#our_price_display";
    public static final String ITEM_PRICE_ATTRIBUTE = "content";

    public static final String ITEM_AVAILABILITY_SELECTOR = "span#availability_value";
    public static final String ITEM_AVAILABILITY_TEXT_IF_AVAILABLE = "Produkt na magazynie";

    public static final String ITEM_TITLE_SELECTOR = "h1[itemprop=name]";

    public static final String ITEM_DESCRIPTION_SELECTOR = "p#short_description_content";
    public static final String ITEM_DESCRIPTION_END_SELECTOR = "a";

    public static final String ITEM_PROPERTIES_SELECTOR = "div.rte";

    public static final String ITEM_ALTERNATIVE_PROPERTIES_SELECTOR = "table.table-data-sheet > tbody > tr > td";


    public static final Pattern ITEM_WEIGHT_PATTERN = Pattern.compile("\\A0?,?[0-9]+ ?\\S?g$");
    public static final Pattern HOP_HARVEST_YEAR_PATTERN = Pattern.compile("\\A\\(?20[0-9][0-9]\\)?$");

    static {
        DataSource dataSource = DataSourceManager.getDataSource();
        List<String> countryShortNameList = new ArrayList<>();
        List<String> countryLongNameList = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY_COUNTRY_NAME)) {
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


    public Stream<Item> parseAllOneCategoryItemsFromShopToStream(String categoryUri) {
        List<String> allItemsUrls = getAllInCategoryItemsUrls(categoryUri);
        Stream<Item> stream = null;
        List<Item> allItemsList = new ArrayList<>();
        switch (categoryUri) {
            case HOP_URI:
                stream = allItemsUrls.stream()
                        .map(url -> (Item)parseHop(url))
                        .filter(Item::isValid);
                break;
//            case YEAST_URI:
//                  ... other categories here
//                break;
        }
        return stream;
    }


    public List<Item> parseAllOneCategoryItemsFromShop(String categoryUri) {
        List<String> allItemsUrls = getAllInCategoryItemsUrls(categoryUri);
        List<Item> allItemsList = new ArrayList<>();
        switch (categoryUri) {
            case HOP_URI:
                for (String url : allItemsUrls) {
                    Hop hop = parseHop(url);
                    if (hop.isValid()) {
                        allItemsList.add(hop);
                    }
                }
                break;
//            case YEAST_URI:
//                  ... other categories here
//                break;
        }
        return allItemsList;
    }

    private List<String> getAllInCategoryItemsUrls(String categoryUri) {
        int pageNumber = 1;
        List<String> allHopsUrls = new ArrayList<>();
        List<String> firstPageList = getUrlsFromSinglePage(BASE_URI + categoryUri + 1);
        List<String> currentPageList = firstPageList;
        do {
            allHopsUrls.addAll(currentPageList);
            pageNumber++;
            currentPageList = getUrlsFromSinglePage(BASE_URI + categoryUri + pageNumber);
        } while ( ! currentPageList.equals(firstPageList) );
        return allHopsUrls;
    }

    private List<String> getUrlsFromSinglePage(String url) {
        Document doc = fetchDocument(url);
        List<String> itemsUrls = new ArrayList<>();
        if(doc == null) {
            return itemsUrls;
        }
        Elements elements = doc.select(ITEM_FROM_LIST_URI_SELECTOR);
        for(Element element : elements) {
            itemsUrls.add(element.attr(ITEM_FROM_LIST_URI_ATTRIBUTE));
        }
        return itemsUrls;
    }

    private boolean parseItemAvailability(Document doc, Item.Builder builder) {
        Element availabilityElem = doc.selectFirst(ITEM_AVAILABILITY_SELECTOR);
        if(availabilityElem == null)
            return false;
        if(availabilityElem.text()
                .contains(ITEM_AVAILABILITY_TEXT_IF_AVAILABLE)) {
            builder.currentAvailability(true);
            return true;
        }
        return false;
    }

    private boolean parseItemPrice(Document doc, Item.Builder builder) {
        Element priceElem = doc.selectFirst(ITEM_PRICE_SELECTOR);
        if(priceElem == null)
            return false;
        String price = priceElem.attr(ITEM_PRICE_ATTRIBUTE);
        try {
            builder.currentPrice( Math.round( Double.parseDouble(price) * 100.0) / 100.0 );
        } catch(NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean parseItemDescription(Document doc, Item.Builder builder) {
        Element shortDescriptionElem = doc.selectFirst(ITEM_DESCRIPTION_SELECTOR);
        if(shortDescriptionElem == null)
            return false;
        String shortDescription = shortDescriptionElem.text();
        String redundantText = shortDescriptionElem.selectFirst(ITEM_DESCRIPTION_END_SELECTOR).text();
        String description = shortDescription.replace(redundantText, "").trim();
        if(description.isEmpty()) {
            return false;
        }
        builder.description(description);
        return true;
    }

    private boolean parseHopYearFromProperties(Document doc, Hop.HopBuilder builder) {
        Element propertiesElem = doc.selectFirst(ITEM_PROPERTIES_SELECTOR);
        if(propertiesElem == null)
            return false;
        String properties = propertiesElem.text();
        String[] propertiesSplit = properties.toLowerCase().split("zbi[oó]r:?");
        if(propertiesSplit.length<2) {
            return false;
        }
        String year = propertiesSplit[1].trim().substring(0,4);
        if( !HOP_HARVEST_YEAR_PATTERN.matcher(year).matches() ) {
            return false;
        }
        builder.harvestYear( Integer.parseInt(year) );
        return true;
    }

    private boolean parseHopCountryFromAlternativeProperties(Document doc, Hop.HopBuilder builder) {
        Elements propertiesTableElem = doc.select(ITEM_ALTERNATIVE_PROPERTIES_SELECTOR);
        return propertiesTableElem.stream()
                .map( s -> Character.toUpperCase(s.text().charAt(0)) + s.text().substring(1) )
                .anyMatch( s -> {
                    if (COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1) {
                        builder.country( COUNTRY_SHORT_NAME_LIST.get( COUNTRY_SHORT_NAME_LIST.indexOf(s) ) );
                        return true;
                    } else if (COUNTRY_LONG_NAME_LIST.indexOf(s) != -1) {
                        builder.country( COUNTRY_SHORT_NAME_LIST.get( COUNTRY_LONG_NAME_LIST.indexOf(s) ) );
                        return true;
                    }
                    return false;
                });
    }

    private Stream<String> getAlternativePropertiesStreamAndMap(Document doc) {
        Elements propertiesTableElem = doc.select(ITEM_ALTERNATIVE_PROPERTIES_SELECTOR);
        return propertiesTableElem.stream()
                .map( s -> Character.toUpperCase(s.text().charAt(0)) + s.text().substring(1).toLowerCase() );
    }

    private String[] getTitleString(Document doc) {
        Element titleElem = doc.selectFirst(ITEM_TITLE_SELECTOR);
        if(titleElem == null)
            return new String[]{""};
        return titleElem.text().split(" ");
    }

    private void removeRedundantFromTitle(Iterator<String> iterator, Hop.HopBuilder builder) {
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

    private boolean parseHopVarietyFromTitleWithFormYearCountryWeight(Document doc, Hop.HopBuilder builder) {
        List<String> titleParts = new ArrayList<>(Arrays.asList(getTitleString(doc)));
        removeRedundantFromTitle(titleParts.iterator(), builder);

        StringBuilder variety = new StringBuilder();
        boolean keepOnAppending = true;
        for (String s : titleParts) {
            if (HOP_HARVEST_YEAR_PATTERN.matcher(s).matches()) {
                builder.harvestYear(Integer.parseInt(s));
                keepOnAppending = false;
            } else if (COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1) {
                builder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_SHORT_NAME_LIST.indexOf(s)));
                keepOnAppending = false;
            } else if (COUNTRY_LONG_NAME_LIST.indexOf(s) != -1) {
                builder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_LONG_NAME_LIST.indexOf(s)));
                keepOnAppending = false;
            } else if (ITEM_WEIGHT_PATTERN.matcher(s).matches()) {
                builder.netWeight(parseWeightFromPattern(s));
                keepOnAppending = false;
            } else if (keepOnAppending) {
                variety.append(s).append(" ");
            }
        }
        if(variety.length() == 0) {
            return false;
        } else {
            builder.variety(variety.toString().trim());
            return true;
        }
    }

    private boolean parseHopPurposeFromAlternativeProperties(Document doc, Hop.HopBuilder builder) {
        return getAlternativePropertiesStreamAndMap(doc)
                .anyMatch( s -> {
                    switch (s) {
                        case "Aromatyczny":
                            builder.purpose(Hop.Purpose.AROMA);
                            return true;
                        case "Goryczkowy":
                            builder.purpose(Hop.Purpose.BITTER);
                            return true;
                        case "Uniwersalny":
                            builder.purpose(Hop.Purpose.BOTH);
                            return true;
                    }
                    return false;
                });
    }

    private boolean parseHopFormFromAlternativeProperties(Document doc, Hop.HopBuilder builder) {
        return getAlternativePropertiesStreamAndMap(doc)
                .anyMatch( s -> {
                    switch (s) {
                        case "Granulat":
                            builder.hopForm(Hop.HopForm.PELLET);
                            return true;
                        case "Szyszka":
                            builder.hopForm(Hop.HopForm.WHOLELEAF);
                            return true;
                    }
                    return false;
                });
    }

    private boolean parseNetWeightFromAlternativeProperties(Document doc, Hop.HopBuilder builder) {
        return getAlternativePropertiesStreamAndMap(doc)
                .anyMatch( s -> {
                    if(ITEM_WEIGHT_PATTERN.matcher(s).matches()) {
                        builder.netWeight(parseWeightFromPattern(s));
                        return true;
                    }
                    return false;
                });
    }

    private int parseWeightFromPattern(String netWeightString) {
        int multiplier = 1;
        if(netWeightString.toLowerCase().contains("k")) {
            multiplier = 1000;
        }
        netWeightString = netWeightString.replaceAll("[^0-9.,]","");
        netWeightString = netWeightString.replaceAll(",",".");
        return (int) (Double.parseDouble(netWeightString) * multiplier);
    }

    private void parseHopAlphaAcidsFromProperties(Document doc, Hop.HopBuilder builder) {
                // most of the Hops in this shop doesn't store this information
    }

    private Hop parseHop(String itemURL) {
        Document doc = fetchDocument(itemURL);
        Hop.HopBuilder hopBuilder = new Hop.HopBuilder();
        if(doc == null) {
            return hopBuilder.valid(false).build();
        }
        // ------------- SHARED FIElDS
        hopBuilder.image(HOP_IMAGE_PATH);
        hopBuilder.uri(itemURL.replace(BASE_URI, ""));
        // ------------- AVAILABILITY
        parseItemAvailability(doc, hopBuilder);
        // ------------- PRICE
        if ( !parseItemPrice(doc, hopBuilder) ) {
            return hopBuilder.valid(false).build();
        }
        // ------------- SHORT DESCRIPTION (OPT)
        parseItemDescription(doc, hopBuilder);
        // ------------- FIELDS FROM TITLE
        if ( !parseHopVarietyFromTitleWithFormYearCountryWeight(doc, hopBuilder) ) {
            return hopBuilder.valid(false).build();
        }
        // ------------- PURPOSE (OPT)
        parseHopPurposeFromAlternativeProperties(doc, hopBuilder);

        // ------------- YEAR
        if( hopBuilder.getHarvestYear() == 0.0 ) {
            if( !parseHopYearFromProperties(doc, hopBuilder)) {
                return hopBuilder.valid(false).build();
            }
        }
        // ------------- FORM
        if( hopBuilder.getHopForm() == Hop.HopForm.MISSING ) {
            if( !parseHopFormFromAlternativeProperties(doc, hopBuilder)) {
                return hopBuilder.valid(false).build();
            }
        }
        // ------------- WEIGHT
        if( hopBuilder.getNetWeight() == 0 ) {
            if( !parseNetWeightFromAlternativeProperties(doc, hopBuilder)) {
                return hopBuilder.valid(false).build();
            }
        }
        // ------------- COUNTRY
        if( hopBuilder.getCountry().equals("ND") ) {
            if( !parseHopCountryFromAlternativeProperties(doc, hopBuilder)) {
                return hopBuilder.valid(false).build();
            }
        }
        // ------------- ALPHAACIDS (OPT)
        parseHopAlphaAcidsFromProperties(doc, hopBuilder);

        return hopBuilder.build();
    }
}
