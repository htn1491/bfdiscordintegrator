/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package htn.bfdiscordintegration;

/**
 *
 * @author Admin
 */
public enum TeamEnum {
    GLOBAL(0, "Global"),
    RED(1, "RED"),
    BLUE(2, "BLUE"),
    SPEC(3, "SPEC"),
    UNKOWN(-1, "Unknown");
    
    private final int CODE;
    private final String PRINTVALUE;
    
    private TeamEnum(int code, String printValue) {
        this.CODE = code;
        this.PRINTVALUE = printValue;
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
}
