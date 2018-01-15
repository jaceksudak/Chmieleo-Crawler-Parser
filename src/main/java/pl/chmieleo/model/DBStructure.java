package pl.chmieleo.model;

public class DBStructure {
    public static final String TABLE_ITEMS = "items";
    public static final String COLUMN_ITEMS_ID = "id";
    public static final String COLUMN_ITEMS_CATEGORY_ID = "category_id";
    public static final String COLUMN_ITEMS_ITEM_NAME = "item_name";
    public static final String COLUMN_ITEMS_VARIETY = "variety";
    public static final String COLUMN_ITEMS_COUNTRY_ID = "country_id";
    public static final String COLUMN_ITEMS_NET_WEIGHT = "net_weight";
    public static final String COLUMN_ITEMS_DESCRIPTION = "description";
    public static final String COLUMN_ITEMS_MANUFACTURER= "manufacturer";
    public static final String COLUMN_ITEMS_IMAGE = "image_path";

    public static final String TABLE_HOPS = "hops";
    public static final String COLUMN_HOPS_ID = "id";
    public static final String COLUMN_HOPS_HARVEST_YEAR = "harvest_year";
    public static final String COLUMN_HOPS_FORM = "form";
    public static final String COLUMN_HOPS_PURPOSE = "purpose";
    public static final String COLUMN_HOPS_ALPHA_ACIDS = "alpha_acids";

    public static final String TABLE_COUNTRY = "country";
    public static final String COLUMN_COUNTRY_ID = "id";
    public static final String COLUMN_COUNTRY_COUNTRY_NAME = "country_name";
    public static final String COLUMN_COUNTRY_SHORT_NAME = "short_name";

    public static final String TABLE_ITEM_CATEGORIES = "item_categories";
    public static final String COLUMN_ITEM_CATEGORIES_ID = "id";
    public static final String COLUMN_ITEM_CATEGORIES_CATEGORY_CLASS_NAME = "category_class_name";
    public static final String COLUMN_ITEM_CATEGORIES_CATEGORY = "category";

    public static final String TABLE_SHOPS = "shops";
    public static final String COLUMN_SHOPS_ID = "id";
    public static final String COLUMN_SHOPS_SHOP_NAME = "shop_name";
    public static final String COLUMN_SHOPS_SHOP_URL = "shop_url";
    public static final String COLUMN_SHOPS_LOGO = "logo";

    public static final String TABLE_ITEMS_IN_SHOPS = "items_in_shops";
    public static final String COLUMN_ITEMS_IN_SHOPS_ID = "id";
    public static final String COLUMN_ITEMS_IN_SHOPS_SHOP_ID = "shop_id";
    public static final String COLUMN_ITEMS_IN_SHOPS_ITEM_ID = "item_id";
    public static final String COLUMN_ITEMS_IN_SHOPS_GROSS_WEIGHT = "gross_weight";
    public static final String COLUMN_ITEMS_IN_SHOPS_ITEM_URL = "items_url";

    public static final String TABLE_ITEMS_CURRENT_STATES = "items_current_states";
    public static final String COLUMN_ITEMS_CURRENT_STATES_ID = "id";
    public static final String COLUMN_ITEMS_CURRENT_STATES_ITEM_IN_SHOP_ID = "item_in_shop_id";
    public static final String COLUMN_ITEMS_CURRENT_STATES_PRICE = "price";
    public static final String COLUMN_ITEMS_CURRENT_STATES_AVAILABILITY = "availability";
    public static final String COLUMN_ITEMS_CURRENT_STATES_UPDATE_DATE = "update_date";
}
