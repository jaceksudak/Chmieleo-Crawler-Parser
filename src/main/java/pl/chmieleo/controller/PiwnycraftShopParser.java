package pl.chmieleo.controller;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pl.chmieleo.model.Hop;
import pl.chmieleo.model.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PiwnycraftShopParser extends BaseShopParser {

    private static final String ITEM_PRICE_SELECTOR = "div.price > em";

    private static final String ITEM_AVAILABILITY_SELECTOR = "dd.availability";
    private static final String ITEM_AVAILABILITY_TEXT_IF_NOT_AVAILABLE = "brak";

    private static final String ITEM_TITLE_SELECTOR = "h1.name";

    private static final String ITEM_DESCRIPTION_ELEMENTS_SELECTOR = "div.description > div > p > span" ;

    public static final Pattern HOP_HARVEST_YEAR_PATTERN = Pattern.compile("\\(?20[0-9][0-9]\\)?");
    public static final Pattern ITEM_WEIGHT_PATTERN = Pattern.compile("0?,?[0-9]+ ?\\S?[Gg]");
    private static final Pattern HOP_ALPHA_ACIDS_PATTERN = Pattern.compile("\\d?\\d,\\d%");

    @Override
    protected int setShopID() {
        return 2;
    }

    @Override
    protected String setBaseURI() {
        return "https://www.piwnykraft.pl/";
    }

    @Override
    protected String setHopListURI() {
        return "pl/c/Chmiel/3/";
    }

    @Override
    protected String setItemFromListURLSelector() {
        return  "div.product > a.details";
    }

    @Override
    protected String setItemFromListURLAttribute() {
        return "href";
    }

    @Override
    protected void parseItemAvailability(Document doc, Item.Builder<?> builder) {
        try {
            if( ! doc.selectFirst(ITEM_AVAILABILITY_SELECTOR)
                    .text()
                    .contains(ITEM_AVAILABILITY_TEXT_IF_NOT_AVAILABLE)
                    ) {
                builder.currentAvailability(true);
            }
        } catch( NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void parseItemPrice(Document doc, Item.Builder<?> builder) {
        Element priceElem = doc.selectFirst(ITEM_PRICE_SELECTOR);
        if(priceElem != null) {
            String price = priceElem.text();
            price = price.replaceAll("[^\\d,]","");
            price = price.replaceAll(",", ".");
            try {
                builder.currentPrice( Double.parseDouble(price) );
            } catch(NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected List<String> getAllInCategoryItemsUrls(String categoryUri, int limit) {
        int pageNumber = 1;
        List<String> allHopsUrls = new ArrayList<>();
        List<String> previousPageList = getUrlsFromSinglePage(baseURI + categoryUri + 1);
        List<String> currentPageList = previousPageList;
        do {
            allHopsUrls.addAll(currentPageList);
            previousPageList = currentPageList;
            pageNumber++;
            currentPageList = getUrlsFromSinglePage(baseURI + categoryUri + pageNumber);
        } while ( ! currentPageList.equals(previousPageList) &&
                pageNumber <= limit );
        return allHopsUrls;
    }

    @Override
    protected void parseItemImage(Document doc, Item.Builder<?> builder) {
        builder.image(HOP_IMAGE_IN_SERVER_PATH);
    }

    @Override
    protected boolean parseHopVariety(Document doc, Hop.HopBuilder hopBuilder) {
        String title = parseItemTitle(doc);
        String[] titleParts = ITEM_WEIGHT_PATTERN.split(title);
        if( !titleParts[0].isEmpty() ) {
            String[] titleSmallerParts = titleParts[0].trim().split(" ");
            List<String> titlePartsList = new ArrayList<>(Arrays.asList(titleSmallerParts));
            StringBuilder variety = new StringBuilder();
            for(String s : titlePartsList) {
                if (HOP_HARVEST_YEAR_PATTERN.matcher(s).matches() ||
                        COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1 ||
                        COUNTRY_LONG_NAME_LIST.indexOf(s) != -1 ||
                        ITEM_WEIGHT_PATTERN.matcher(s).matches()) {
                    break;
                } else {
                    variety.append(s).append(" ");
                }
            }
            int varietyLength = variety.length();
            if(varietyLength == 0) {
                return false;
            } else {
                if( variety.substring(varietyLength-2)
                           .equals("- ")) {
                    hopBuilder.variety( variety.substring(0,varietyLength-3));
                } else {
                    hopBuilder.variety( variety.substring(0,varietyLength-1));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean parseHopCountry(Document doc, Hop.HopBuilder hopBuilder) {
        String title = parseItemTitle(doc);
        if( title != null ) {
            List<String> titleParts = new ArrayList<>(Arrays.asList(title.split(" ")));
            for (String s : titleParts) {
                if (COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1) {
                    hopBuilder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_SHORT_NAME_LIST.indexOf(s)));
                    return true;
                } else if (COUNTRY_LONG_NAME_LIST.indexOf(s) != -1) {
                    hopBuilder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_LONG_NAME_LIST.indexOf(s)));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void parseHopCountryAltHook(Document doc, Hop.HopBuilder hopBuilder) {
        Elements descriptionDivElements = doc.select(ITEM_DESCRIPTION_ELEMENTS_SELECTOR);
        first:
        for( Element element : descriptionDivElements ) {
            String[] elementTextParts = element.text().split(" ");
            for (String s : elementTextParts) {
                if (COUNTRY_SHORT_NAME_LIST.indexOf(s) != -1) {
                    hopBuilder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_SHORT_NAME_LIST.indexOf(s)));
                    break first;
                } else if (COUNTRY_LONG_NAME_LIST.indexOf(s) != -1) {
                    hopBuilder.country(COUNTRY_SHORT_NAME_LIST.get(COUNTRY_LONG_NAME_LIST.indexOf(s)));
                    break first;
                }
            }
        }
    }

    @Override
    protected boolean parseHopNetWeight(Document doc, Hop.HopBuilder hopBuilder) {
        String title = parseItemTitle(doc);
        if(title != null) {
            Matcher matcher = ITEM_WEIGHT_PATTERN.matcher(title);
            if (matcher.find()) {
                int weight = parseWeightFromPattern(matcher.group(0));
                hopBuilder.netWeight(weight);
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean parseHopDescription(Document doc, Hop.HopBuilder hopBuilder) {
        Element descriptionElem = doc.selectFirst(ITEM_DESCRIPTION_ELEMENTS_SELECTOR);
        if( descriptionElem != null) {
            String description = descriptionElem.text();
            if( !description.isEmpty() ) {
                hopBuilder.description(description);
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean parseHopHarvestYear(Document doc, Hop.HopBuilder hopBuilder) {
        String title = parseItemTitle(doc);
        if(title != null) {
            Matcher matcher = HOP_HARVEST_YEAR_PATTERN.matcher(title);
            if (matcher.find()) {
                int year;
                try {
                    year = Integer.parseInt(matcher.group(0));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return false;
                }
                hopBuilder.harvestYear(year);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void parseHopHarvestYearAltHook(Document doc, Hop.HopBuilder hopBuilder) {
        Elements descriptionDivElements = doc.select(ITEM_DESCRIPTION_ELEMENTS_SELECTOR);
        first:
        for( Element element : descriptionDivElements ) {
            String[] elementTextParts = element.text().split(" ");
            for (String s : elementTextParts) {
                Matcher matcher = HOP_HARVEST_YEAR_PATTERN.matcher(s);
                if( matcher.find() ) {
                    int year = Integer.parseInt( matcher.group(0) );
                    hopBuilder.harvestYear(year);
                    break first;
                }
            }
        }
    }

    @Override
    protected boolean parseHopForm(Document doc, Hop.HopBuilder hopBuilder) {
        String title = parseItemTitle(doc);
        if(title != null) {
            if( title.toLowerCase().contains("granulat") ) {
                hopBuilder.hopForm(Hop.HopForm.PELLET);
                return true;
            } else if( title.toLowerCase().contains("szyszk") ) {
                hopBuilder.hopForm(Hop.HopForm.PELLET);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void parseHopAlphaAcidsHook(Document doc, Hop.HopBuilder hopBuilder) {
        Elements descriptionDivElements = doc.select(ITEM_DESCRIPTION_ELEMENTS_SELECTOR);
        first:
        for( Element element : descriptionDivElements ) {
            String[] elementTextParts = element.text().split(" ");
            for (String s : elementTextParts) {
                Matcher matcher = HOP_ALPHA_ACIDS_PATTERN.matcher(s);
                if( matcher.find() ) {
                    try {
                        double aa = Double.parseDouble( matcher.group(0).replaceAll("[^\\d,]","").replaceAll(",",".") );
                        hopBuilder.alphaAcids(aa);
                        break first;
                    } catch ( NumberFormatException e ) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String parseItemTitle(Document doc) {
        Element priceElem = doc.selectFirst(ITEM_TITLE_SELECTOR);
        if(priceElem != null) {
            String title = priceElem.text();
            if( ! title.isEmpty() ) {
                return title;
            }
        }
        return null;
    }
}
