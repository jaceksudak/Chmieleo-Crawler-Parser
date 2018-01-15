package pl.chmieleo.model;

import java.io.File;

public class Shop {
    private int id;
    private String name;
    private String URL;
    private File image;

    public Shop(int id, String name, String URL, File image) {
        this.id = id;
        this.name = name;
        this.URL = URL;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getURL() {
        return URL;
    }

    public File getImage() {
        return image;
    }
}
