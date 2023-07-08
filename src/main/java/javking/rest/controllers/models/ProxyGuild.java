package javking.rest.controllers.models;

public class ProxyGuild {
    private final String id, name, icon;

    public ProxyGuild(String id, String name, String icon) {
        assert id != null;
        assert name != null;
        assert icon != null;

        this.id = id;
        this.name = name;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public long getIdLong() {
        return Long.parseUnsignedLong(id);
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }
}
