package htn.bfdiscordintegration.models;

public class PlayerModel {
    private String name;
    private String currentName;
    private int playerId;
    private boolean isAi;
    private int team;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrentName() {
        return currentName;
    }

    public void setCurrentName(String currentName) {
        this.currentName = currentName;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public boolean isIsAi() {
        return isAi;
    }

    public void setIsAi(boolean isAi) {
        this.isAi = isAi;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    @Override
    public String toString() {
        return "PlayerModel{" + "name=" + name + ", playerId=" + playerId + ", isAi=" + isAi + ", team=" + team + '}';
    }
}
