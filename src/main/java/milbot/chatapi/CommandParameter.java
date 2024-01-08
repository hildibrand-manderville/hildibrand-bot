package milbot.chatapi;

public class CommandParameter {

    private String name;
    private String description;
    private Type type;
    private boolean required;

    public CommandParameter(String name, String description, Type type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public CommandParameter(String name, String description, Type type, boolean required) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public enum Type {
        BOOL, INT, STRING, USER, GROUP, ROOM
    }
}
