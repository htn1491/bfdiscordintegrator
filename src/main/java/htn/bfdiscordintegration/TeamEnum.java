package htn.bfdiscordintegration;

import discord4j.rest.util.Color;

public enum TeamEnum {
    GLOBAL(0, "Global", Color.GRAY),
    RED(1, "RED", Color.RED),
    BLUE(2, "BLUE", Color.BLUE),
    SPEC(3, "SPEC", Color.ORANGE),
    UNKOWN(-1, "Unknown", Color.GRAY);
    
    private final int CODE;
    private final String PRINTVALUE;
    private final Color DISCORDCOLOR;
    
    private TeamEnum(int code, String printValue, Color discordColor) {
        this.CODE = code;
        this.PRINTVALUE = printValue;
        this.DISCORDCOLOR = discordColor;
    }
    
    public static TeamEnum findByCode(int code) {
        for(TeamEnum val : TeamEnum.values()) {
            if(val.CODE == code) {
                return val;
            }
        }
        return TeamEnum.UNKOWN;
    }

    public String getPrintValue() {
        return PRINTVALUE;
    }
    
    public Color getDiscordColor() {
        return DISCORDCOLOR;
    }
}
