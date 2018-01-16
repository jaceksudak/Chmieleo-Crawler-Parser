package pl.chmieleo.model;

import javax.sql.DataSource;
import java.sql.*;
import java.text.DecimalFormat;

public class Hop extends Item {
    private static final int CATEGORY_ID;
    private static final String QUERY_CATEGORY_ID = "SELECT " +  DBStructure.COLUMN_ITEM_CATEGORIES_ID + " FROM " + DBStructure.TABLE_ITEM_CATEGORIES +
            " WHERE " + DBStructure.TABLE_ITEM_CATEGORIES  + "." + DBStructure.COLUMN_ITEM_CATEGORIES_CATEGORY_CLASS_NAME + " = ? ORDER BY " + DBStructure.COLUMN_ITEM_CATEGORIES_ID + " DESC LIMIT 1";

    static {
        DataSource dataSource = DataSourceManager.getDataSource();
        int id = 1;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(QUERY_CATEGORY_ID)) {
            ps.setString(1,Hop.class.getName());
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                id = rs.getInt(DBStructure.COLUMN_ITEM_CATEGORIES_ID);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            CATEGORY_ID = id;
        }
    }

    private int harvestYear;
    private HopForm hopForm;
    private Purpose purpose;
    private double alphaAcids;


    private Hop(HopBuilder hopBuilder) {
        super(hopBuilder);
        this.harvestYear = hopBuilder.harvestYear;
        this.hopForm = hopBuilder.hopForm;
        this.purpose = hopBuilder.purpose;
        this.alphaAcids = hopBuilder.alphaAcids;
    }

    public static class HopBuilder extends Item.Builder<HopBuilder> {
        private int harvestYear = 0;
        private HopForm hopForm = HopForm.MISSING;
        private Purpose purpose = Purpose.MISSING;
        private double alphaAcids = 0.0;

        public Hop build() {
            this.categoryId(CATEGORY_ID);
            this.name(createName());
            return new Hop(this);
        }

        private String createName() {
            DecimalFormat df = new DecimalFormat("#.#");
            String weight;
            if( getNetWeight()<500 ) {
                weight = getNetWeight() + "g";
            } else {
                weight = df.format((double)getNetWeight()/1000) + "kg";
            }
            return getVariety() + " " + weight + " - " + getCountry() + " " + getHarvestYear() + " - chmiel " + getHopForm().toName();
        }

        public HopBuilder harvestYear(int harvestYear) {
            this.harvestYear = harvestYear;
            return this;
        }

        public HopBuilder hopForm(Hop.HopForm hopForm) {
            this.hopForm = hopForm;
            return this;
        }

        public HopBuilder purpose(Hop.Purpose purpose) {
            this.purpose = purpose;
            return this;
        }

        public HopBuilder alphaAcids(double alphaAcids) {
            this.alphaAcids = alphaAcids;
            return this;
        }

        public int getHarvestYear() {
            return harvestYear;
        }

        public HopForm getHopForm() {
            return hopForm;
        }

        public Purpose getPurpose() {
            return purpose;
        }

        public double getAlphaAcids() {
            return alphaAcids;
        }
    }

    @Override
    public String toString() {
        return "Hop{" +
                super.toString() +
                "harvestYear=" + harvestYear +
                ", hopForm=" + hopForm +
                ", purpose=" + purpose +
                ", alphaAcids=" + alphaAcids +
                ", description=" + getDescription() +
                '}';
    }

    public int getHarvestYear() {
        return harvestYear;
    }

    public Hop.HopForm getHopForm() {
        return hopForm;
    }

    public Hop.Purpose getPurpose() {
        return purpose;
    }

    public double getAlphaAcids() {
        return alphaAcids;
    }

    public enum Purpose {
        AROMA,
        BITTER,
        BOTH,
        MISSING;

        @Override
        public String toString() {
            switch(this) {
                case AROMA:
                    return "aroma";
                case BITTER:
                    return "bitter";
                case BOTH:
                    return "both";
                case MISSING:
                    return "missing";
                default:
                    return null;
            }
        }
    }

    public enum HopForm {
        PELLET,
        WHOLELEAF,
        MISSING;

        public String toName() {
            switch (this) {
                case PELLET:
                    return "granulat";
                case WHOLELEAF:
                    return "szyszka";
                default:
                    return "";
            }
        }

        @Override
        public String toString() {
            switch(this) {
                case PELLET:
                    return "pellet";
                case WHOLELEAF:
                    return "whole";
                case MISSING:
                    return "missing";
                default:
                    return null;
            }
        }
    }
}
