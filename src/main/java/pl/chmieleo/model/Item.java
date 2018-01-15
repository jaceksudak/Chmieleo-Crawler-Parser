package pl.chmieleo.model;


public abstract class Item {

    private boolean valid;
    private int categoryId;
    private String name;
    private String variety;
    private String country;
    private int netWeight;
    private String description;
    private String manufacturer;
    private String image;
    private double currentPrice;
    private boolean currentAvailability;
    private String uri;

                    public Item(int id, int categoryId, String name, String variety, String country, int netWeight, String description, String manufacturer, double currentPrice, boolean currentAvailability) {
                        this.categoryId = categoryId;
                        this.name = name;
                        this.variety = variety;
                        this.country = country;
                        this.netWeight = netWeight;
                        this.description = description;
                        this.manufacturer = manufacturer;
                        this.currentPrice = currentPrice;
                        this.currentAvailability = currentAvailability;
                    }

    Item(Builder builder) {
        this.valid = builder.valid;
        this.categoryId = builder.categoryId;
        this.name = builder.name;
        this.variety = builder.variety;
        this.country = builder.country;
        this.netWeight = builder.netWeight;
        this.description = builder.description;
        this.manufacturer = builder.manufacturer;
        this.image = builder.image;
        this.currentPrice = builder.currentPrice;
        this.currentAvailability = builder.currentAvailability;
        this.uri = builder.uri;
    }

    public abstract static class Builder <B> {
        private boolean valid = true;
        private int categoryId = 0;
        private String name = "missing";
        private String variety = "missing";
        private String country = "ND";
        private int netWeight = 0;
        private String description = "missing";
        private String manufacturer = "missing";
        private String image = "missing";
        private double currentPrice = 0.0;
        private boolean currentAvailability = false;
        private String uri = "";

        public B valid(boolean valid) {
            this.valid = valid;
            return (B)this;
        }

        public B categoryId(int categoryId) {
            this.categoryId = categoryId;
            return (B)this;
        }

        public B name(String name) {
            this.name = name;
            return (B)this;
        }

        public B variety(String variety) {
            this.variety = variety;
            return (B)this;
        }

        public B country(String country) {
            this.country = country;
            return (B)this;
        }

        public B netWeight(int netWeight) {
            this.netWeight = netWeight;
            return (B)this;
        }

        public B description(String description) {
            this.description = description;
            return (B)this;
        }

        public B manufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
            return (B)this;
        }

        public B image(String image) {
            this.image = image;
            return (B)this;
        }

        public B currentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
            return (B)this;
        }

        public B currentAvailability(boolean currentAvailability) {
            this.currentAvailability = currentAvailability;
            return (B)this;
        }

        public B uri(String uri) {
            this.uri = uri;
            return (B)this;
        }

        public boolean isValid() {
            return valid;
        }

        public int getCategoryId() {
            return categoryId;
        }

        public String getName() {
            return name;
        }

        public String getVariety() {
            return variety;
        }

        public String getCountry() {
            return country;
        }

        public int getNetWeight() {
            return netWeight;
        }

        public String getDescription() {
            return description;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public String getImage() {
            return image;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public boolean isCurrentAvailability() {
            return currentAvailability;
        }

        public String getUri() {
            return uri;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getVariety() {
        return variety;
    }

    public String getCountry() {
        return country;
    }

    public int getNetWeight() {
        return netWeight;
    }

    public String getDescription() {
        return description;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getImage() {
        return image;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public boolean isCurrentAvailability() {
        return currentAvailability;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "Item{" +
                "valid=" + valid +
                ", categoryId=" + categoryId +
                ", name='" + name + '\'' +
                ", variety='" + variety + '\'' +
                ", country='" + country + '\'' +
                ", netWeight=" + netWeight +
//                ", description='" + description + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", image=" + image +
                ", currentPrice=" + currentPrice +
                ", currentAvailability=" + currentAvailability +
                ", uri='" + uri + '\'' +
                '}';
    }
}
