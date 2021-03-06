package pl.chmieleo.controller;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.chmieleo.model.Hop;
import pl.chmieleo.model.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TwojbrowarShopParser extends  BaseShopParser {

    private static final String ITEM_PRICE_SELECTOR = "span#our_price_display";
    private static final String ITEM_PRICE_ATTRIBUTE = "content";

    private static final String ITEM_AVAILABILITY_SELECTOR = "span#availability_value";
    private static final String ITEM_AVAILABILITY_TEXT_IF_AVAILABLE = "Produkt na magazynie";

    private static final String ITEM_TITLE_SELECTOR = "h1[itemprop=name]";

    private static final String ITEM_DESCRIPTION_SELECTOR = "p#short_description_content";
    private static final String ITEM_DESCRIPTION_END_SELECTOR = "a";

    private static final String ITEM_PROPERTIES_SELECTOR = "div.rte > p";

    private static final String ITEM_ALTERNATIVE_PROPERTIES_SELECTOR = "table.table-data-sheet > tbody > tr > td";


    @Override
    protected int setShopID() {
        return 1;
    }

    @Override
    protected String setBaseURI() {
        return "http://twojbrowar.pl/";
    }

    @Override
    protected String setHopListURI() {
        return "pl/10-chmiel?p=";
    }

    @Override
    protected String setItemFromListURLSelector() {
        return "a.product_img_link";
    }

    @Override
    protected String setItemFromListURLAttribute() {
        return "href";
    }

    @Override
    protected void parseItemImage(Document doc, Item.Builder<?>  builder) {
        builder.image(HOP_IMAGE_IN_SERVER_PATH);
    }

    @Override
    protected List<String> getAllInCategoryItemsUrls(String categoryUri, int limit) {
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

    @Override
    protected void parseItemPrice(Document doc, Item.Builder<?> builder) {
        Element priceElem = doc.selectFirst(ITEM_PRICE_SELECTOR);
        if(priceElem != null) {
            String price = priceElem.attr(ITEM_PRICE_ATTRIBUTE);
            try {
                builder.currentPrice( Math.round( Double.parseDouble(price) * 100.0) / 100.0 );
            } catch(NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void parseItemAvailability(Document doc, Item.Builder<?> builder) {
        try {
            if(doc.selectFirst(ITEM_AVAILABILITY_SELECTOR)
                    .text()
                    .contains(ITEM_AVAILABILITY_TEXT_IF_AVAILABLE)
                    ) {
                builder.currentAvailability(true);
            }
        } catch( NullPointerException e) {
            e.printStackTrace();
        }
    }
//"А а \tБ б \tВ в \tГ г \tҐ ґ \tД д \tЕ е \tЄ є \tЖ ж \tЗ з \tИ и\n"+
//        "І і \tЇ ї \tЙ й \tК к \tЛ л \tМ м \tН н \tО о \tП п \tР р \tС с\n"+
//        "Т т \tУ у \tФ ф \tХ х \tЦ ц \tЧ ч \tШ ш \tЩ щ \tЬ ь \tЮ ю \tЯ я"
    @Override
    protected boolean parseHopVariety(Document doc, Hop.HopBuilder hopBuilder) {
        String title = getTitleString(doc);
        if(title == null) {
            return false;
        }
        if( checkExtremeBoundaryConditions(title, hopBuilder) ) {
            return true;
        }
        List<String> titleParts = new ArrayList<>(Arrays.asList(getTitleStringArr(doc)));
        removeRedundantFromHopTitle(titleParts.iterator(), hopBuilder);
        StringBuilder variety = new StringBuilder();
        for (String s : titleParts) {
            if ( COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1 ||
                    COUNTRY_LONG_NAME_LIST.indexOf(s) != -1 ||
                    ITEM_WEIGHT_PATTERN.matcher(s).matches()) {
                break;
            } else {
                s = s.substring(0,1).toUpperCase() + s.substring(1);
                variety.append(s).append(" ");
            }
        }
        if(variety.length() == 0) {
            return false;
        } else {
            hopBuilder.variety( variety.toString()
                    .replaceAll("[\\u2122\\u00AE!]","")
                    .trim() );
            return true;
        }
    }


    @Override
    protected boolean parseHopCountry(Document doc, Hop.HopBuilder hopBuilder) {
        Elements propertiesTableElem = doc.select(ITEM_ALTERNATIVE_PROPERTIES_SELECTOR);
        return propertiesTableElem.stream()
                .map( s -> Character.toUpperCase(s.text().charAt(0)) + s.text().substring(1) )
                .anyMatch( s -> {
                    if (COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1) {
                        hopBuilder.country( COUNTRY_SHORT_NAME_LIST.get( COUNTRY_SHORT_NAME_LIST.indexOf(s) ) );
                        return true;
                    } else if (COUNTRY_LONG_NAME_LIST.indexOf(s) != -1) {
                        hopBuilder.country( COUNTRY_SHORT_NAME_LIST.get( COUNTRY_LONG_NAME_LIST.indexOf(s) ) );
                        return true;
                    }
                    return false;
                });
    }

    @Override
    protected void parseHopCountryAltHook(Document doc, Hop.HopBuilder hopBuilder) {
        List<String> titleParts = new ArrayList<>(Arrays.asList(getTitleStringArr(doc)));
        for (String s : titleParts) {
            if (COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1) {
                hopBuilder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_SHORT_NAME_LIST.indexOf(s)));
            } else if (COUNTRY_LONG_NAME_LIST.indexOf(s) != -1) {
                hopBuilder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_LONG_NAME_LIST.indexOf(s)));
            }
        }
    }

    @Override
    protected boolean parseHopNetWeight(Document doc, Hop.HopBuilder hopBuilder) {
        return getAlternativePropertiesStreamAndMap(doc)
                .anyMatch( s -> {
                    if(ITEM_WEIGHT_PATTERN.matcher(s).matches()) {
                        hopBuilder.netWeight(parseWeightFromPattern(s));
                        return true;
                    }
                    return false;
                });
    }

    @Override
    protected void parseHopNetWeightAltHook(Document doc, Hop.HopBuilder hopBuilder) {
        List<String> titleParts = new ArrayList<>(Arrays.asList(getTitleStringArr(doc)));
        for (String s : titleParts) {
            if (ITEM_WEIGHT_PATTERN.matcher(s).matches()) {
                hopBuilder.netWeight(parseWeightFromPattern(s));
            }
        }
    }

    @Override
    protected boolean parseHopDescription(Document doc, Hop.HopBuilder hopBuilder) {
        Element shortDescriptionElem = doc.selectFirst(ITEM_DESCRIPTION_SELECTOR);
        if(shortDescriptionElem == null)
            return false;
        String shortDescription = shortDescriptionElem.text();
        String redundantText = shortDescriptionElem.selectFirst(ITEM_DESCRIPTION_END_SELECTOR).text();
        String description = shortDescription.replace(redundantText, "").trim();
        if(description.isEmpty()) {
            return false;
        }
        hopBuilder.description(description);
        return true;
    }

    @Override
    protected boolean parseHopHarvestYear(Document doc, Hop.HopBuilder hopBuilder) {
        Elements propertiesElements = doc.select(ITEM_PROPERTIES_SELECTOR);
        if(propertiesElements.isEmpty())
            return false;
        StringBuilder propertiesSB = new StringBuilder();
        for(Element prop : propertiesElements) {
            propertiesSB.append(prop.text()).append(" ");
        }
        String properties = propertiesSB.toString();
        String[] propertiesSplit = properties.toLowerCase().split("zbi[oó]r:?");
        if(propertiesSplit.length<2) {
            return false;
        }
        String year = propertiesSplit[1].trim().substring(0,4);
        if( !HOP_HARVEST_YEAR_PATTERN.matcher(year).matches() ) {
            return false;
        }
        hopBuilder.harvestYear( Integer.parseInt(year) );
        return true;
    }

    @Override
    protected void parseHopHarvestYearAltHook(Document doc, Hop.HopBuilder hopBuilder) {
        List<String> titleParts = new ArrayList<>(Arrays.asList(getTitleStringArr(doc)));
        for (String s : titleParts) {
            if (HOP_HARVEST_YEAR_PATTERN.matcher(s).matches()) {
                hopBuilder.harvestYear(Integer.parseInt(s));
            }
        }
    }

    @Override
    protected boolean parseHopForm(Document doc, Hop.HopBuilder hopBuilder) {
        return getAlternativePropertiesStreamAndMap(doc)
                .anyMatch( s -> {
                    switch (s) {
                        case "Granulat":
                            hopBuilder.hopForm(Hop.HopForm.PELLET);
                            return true;
                        case "Szyszka":
                            hopBuilder.hopForm(Hop.HopForm.WHOLELEAF);
                            return true;
                    }
                    return false;
                });
    }

    @Override
    protected void parseHopPurposeHook(Document doc, Hop.HopBuilder hopBuilder) {
        getAlternativePropertiesStreamAndMap(doc)
                .forEach( s -> {
                    switch (s) {
                        case "Aromatyczny":
                            hopBuilder.purpose(Hop.Purpose.AROMA);
                        case "Goryczkowy":
                            hopBuilder.purpose(Hop.Purpose.BITTER);
                        case "Uniwersalny":
                            hopBuilder.purpose(Hop.Purpose.BOTH);
                    }
                });
    }

    private Stream<String> getAlternativePropertiesStreamAndMap(Document doc) {
        Elements propertiesTableElem = doc.select(ITEM_ALTERNATIVE_PROPERTIES_SELECTOR);
        return propertiesTableElem.stream()
                .map( s -> Character.toUpperCase(s.text().charAt(0)) + s.text().substring(1).toLowerCase() );
    }

    private String[] getTitleStringArr(Document doc) {
        Element titleElem = doc.selectFirst(ITEM_TITLE_SELECTOR);
        if(titleElem == null)
            return new String[]{""};
        return titleElem.text().split(" ");
    }

    private String getTitleString(Document doc) {
        Element titleElem = doc.selectFirst(ITEM_TITLE_SELECTOR);
        if(titleElem == null)
            return null;
        return titleElem.text();
    }
}
