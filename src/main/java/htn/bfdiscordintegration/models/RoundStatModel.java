package htn.bfdiscordintegration.models;

import htn.bfdiscordintegration.models.enums.TeamEnum;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Robert
 */
public class RoundStatModel {
    private TeamEnum winningTeam;
    private int redTickets;
    private int blueTickets;
    private final List<PlayerStatModel> playerModels = new ArrayList<>();

    public TeamEnum getWinningTeam() {
        return winningTeam;
    }

    public void setWinningTeam(TeamEnum winningTeam) {
        this.winningTeam = winningTeam;
    }

    public int getRedTickets() {
        return redTickets;
    }

    public void setRedTickets(int redTickets) {
        this.redTickets = redTickets;
    }

    public int getBlueTickets() {
        return blueTickets;
    }

    public void setBlueTickets(int blueTickets) {
        this.blueTickets = blueTickets;
    }

    public List<PlayerStatModel> getPlayerModels() {
        return playerModels;
    }

    @Override
    public String toString() {
        return "RoundStatModel{" + "winningTeam=" + winningTeam + ", redTickets=" + redTickets + ", blueTickets=" + blueTickets + ", playerModels=" + playerModels + '}';
    }
    
}
