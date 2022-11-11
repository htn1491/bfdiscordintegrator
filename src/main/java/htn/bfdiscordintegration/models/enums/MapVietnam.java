package htn.bfdiscordintegration.models.enums;

public enum MapVietnam {
    HUE("hue", "Hue"),
    LANDING_ZONE_ALBANY("landing_zone_albany", "Landing Zone Albany"),
    HO_CHI_MINH_TRAIL_ALT("ho_chi_minh_trail_alt", "Cambordian Incursion"),
    FALL_OF_SAIGON("fall_of_saigon", "Fall of Saigon"),
    LANG_VEI("lang_vei", "Lang Vei"),
    OPERATION_FLAMING_DART("operation_flaming_dart", "Operation Flaming Dart"),
    QUANG_TRI_ALT("quanq_tri_alt", "Quang Tri - 1972"),
    OPERATION_GAME_WARDEN("operation_game_warden", "Operation Game Warden"),
    KHE_SAHN("khe_sahn", "Siege of Khe Sahn"),
    HUE_ALT("hue_alt", "Reclaiming Hue"),
    OPERATION_HASTINGS("operation_hastings", "Operation Hastings"),
    OPERATION_CEDAR_FALLS("operation_cedar_falls", "Operation Cedar Falls"),
    QUANG_TRI("quang_tri", "Quang Tri - 1968"),
    HO_CHI_MINH_TRAIL("ho_chi_minh_trail", "Ho Chi Minh Trail"),
    OPERATION_IRVING("operation_irving", "Operation Irving"),
    IA_DRANG("ia_drang", "The Ia Drang Valley"),
    DEFENSE_OF_CON_THIEN("defense_of_con_thien", "Defense of Con Thien"),
    SAIGON68("saigon68", "Saigon 1968"),
    ;
    
    private final String MAPNAME;
    private final String PRINTNAME;
    
    private MapVietnam(String mapName, String printName) {
        this.MAPNAME = mapName;
        this.PRINTNAME = printName;
    }
    
    public static MapVietnam findByMapName(String mapName) {
        for(MapVietnam val : MapVietnam.values()) {
            if(val.MAPNAME.equals(mapName)) {
                return val;
            }
        }
        return null;
    }

    public String getMapName() {
        return MAPNAME;
    }

    public String getPrintName() {
        return PRINTNAME;
    }
}
