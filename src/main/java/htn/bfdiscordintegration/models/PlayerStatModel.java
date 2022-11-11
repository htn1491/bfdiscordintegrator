package htn.bfdiscordintegration.models;

import htn.bfdiscordintegration.models.enums.TeamEnum;

/**
 *
 * @author Robert
 */
public class PlayerStatModel {
    private String playerName;
    private TeamEnum team = TeamEnum.UNKOWN;
    private int score;
    private int kills;
    private int deaths;
    private boolean isAi;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public TeamEnum getTeam() {
        return team;
    }

    public void setTeam(TeamEnum team) {
        this.team = team;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public boolean isIsAi() {
        return isAi;
    }

    public void setIsAi(boolean isAi) {
        this.isAi = isAi;
    }

    @Override
    public String toString() {
        return "PlayerStatModel{" + "playerName=" + playerName + ", team=" + team + ", score=" + score + ", kills=" + kills + ", deaths=" + deaths + ", isAi=" + isAi + '}';
    }
}
