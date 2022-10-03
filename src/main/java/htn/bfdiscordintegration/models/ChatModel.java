package htn.bfdiscordintegration.models;

import htn.bfdiscordintegration.models.PlayerModel;

public class ChatModel {
    private String formattedTimestamp;
    private PlayerModel playerModel;
    private int team;
    private String text;

    public String getFormattedTimestamp() {
        return formattedTimestamp;
    }

    public void setFormattedTimestamp(String formattedTimestamp) {
        this.formattedTimestamp = formattedTimestamp;
    }

    public PlayerModel getPlayerModel() {
        return playerModel;
    }

    public void setPlayerModel(PlayerModel playerModel) {
        this.playerModel = playerModel;
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "ChatModel{" + "formattedTimestamp=" + formattedTimestamp + ", playerModel=" + playerModel + ", team=" + team + ", text=" + text + '}';
    }
}
