package htn.bfdiscordintegration;

import htn.bfdiscordintegration.models.ChatModel;
import htn.bfdiscordintegration.models.enums.TeamEnum;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import htn.bfdiscordintegration.models.RoundStatModel;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscordIntegratorService {

    private static final Logger log = LogManager.getLogger(DiscordIntegratorService.class);

    @Value("${discord_bot_token}")
    private String token;

    @Value("${chat_channel_id}")
    private String chatChannelID;

    @Value("${public_chat_channel_id}")
    private String publicChatChannelID;

    @Value("${admin_help_channel_id}")
    private String adminChannelId;

    @Value("${admin_help_mention_id:}")
    private String adminHelpMentionId;

    @Value("${hide_rm_commands_in_discord:true}")
    private Boolean hideRmCommandsInDiscord;

    private GatewayDiscordClient gatewayDiscordClient;

    @PostConstruct
    public void init() {
        gatewayDiscordClient = DiscordClientBuilder.create(token)
                .build()
                .login()
                .block();

        gatewayDiscordClient
                .getEventDispatcher()
                .on(MessageCreateEvent.class)
                .map(mce -> {
                    return mce.getMessage();
                });
    }

    private EmbedCreateSpec createDiscordFormattedMessage(final String msg, final ChatModel chatModel) {
        return EmbedCreateSpec.builder()
                .color(TeamEnum.findByCode(chatModel.getTeam()).getDiscordColor())
                .description(msg)
                .build();
    }

    public void publishRoundStats(RoundStatModel roundStatModel) {
        EmbedCreateSpec.Builder specBuilder = EmbedCreateSpec.builder()
                .color(roundStatModel.getWinningTeam().getDiscordColor())
                .title(roundStatModel.getWinningTeam().equals(TeamEnum.GLOBAL) ? "No team has won" : roundStatModel.getWinningTeam().getPrintValue() + " team has won the round!")
                .description("The round statistics" + ((StringUtils.hasText(roundStatModel.getMapName())) ? " for " + roundStatModel.getMapName() : ""))
                .addField("Blue tickets left", "" + roundStatModel.getBlueTickets(), true)
                .addField("Red tickets left", "" + roundStatModel.getRedTickets(), true)
                .addField("BLUE TEAM", "\u200B", false);

        final StringBuilder playerNames = new StringBuilder();
        final StringBuilder scores = new StringBuilder();
        roundStatModel.getPlayerModels().stream().filter(pm -> pm.getTeam().equals(TeamEnum.BLUE)).sorted((o1, o2) -> {
            return o2.getScore() - o1.getScore();
        }).limit(5).forEach(pm -> {
            playerNames.append(pm.getPlayerName()).append(pm.isIsAi() ? " (BOT)" : "").append("\n");
            scores.append((pm.getScore() < 10) ? "0" : "").append(pm.getScore()).append(" / ")
                    .append((pm.getKills() < 10) ? "0" : "").append(pm.getKills()).append(" / ")
                    .append((pm.getDeaths() < 10) ? "0" : "").append(pm.getDeaths())
                    .append("\n");
        });
        specBuilder.addField("Player name", playerNames.toString(), true);
        specBuilder.addField("Score / Kills / Deaths", scores.toString(), true);
        
        specBuilder.addField("RED TEAM", "\u200B", false);
        
        //Reset variables
        playerNames.setLength(0);
        scores.setLength(0);
        roundStatModel.getPlayerModels().stream().filter(pm -> pm.getTeam().equals(TeamEnum.RED)).sorted((o1, o2) -> {
            return o2.getScore() - o1.getScore();
        }).limit(5).forEach(pm -> {
            playerNames.append(pm.getPlayerName()).append(pm.isIsAi() ? " (BOT)" : "").append("\n");
            scores.append((pm.getScore() < 10) ? "0" : "").append(pm.getScore()).append(" / ")
                    .append((pm.getKills() < 10) ? "0" : "").append(pm.getKills()).append(" / ")
                    .append((pm.getDeaths() < 10) ? "0" : "").append(pm.getDeaths())
                    .append("\n");
        });
        specBuilder.addField("Player name", playerNames.toString(), true);
        specBuilder.addField("Score / Kills / Deaths", scores.toString(), true);

        gatewayDiscordClient.getChannelById(Snowflake.of(chatChannelID))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(specBuilder.build()))
                .subscribe();
        
        gatewayDiscordClient.getChannelById(Snowflake.of(publicChatChannelID))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(specBuilder.build()))
                .subscribe();
    }

    public void publishEndRound() {

        EmbedCreateSpec spec = EmbedCreateSpec.builder()
                .color(Color.DARK_GRAY)
                .description("--- ROUND HAS ENDED ---")
                .build();
        gatewayDiscordClient.getChannelById(Snowflake.of(chatChannelID))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(spec))
                .subscribe();
        if (StringUtils.hasText(publicChatChannelID)) {
            gatewayDiscordClient.getChannelById(Snowflake.of(publicChatChannelID))
                    .ofType(MessageChannel.class)
                    .flatMap(channel -> channel.createMessage(spec))
                    .subscribe();
        }
    }

    public void publishDiscordMessage(final String msg, final ChatModel chatModel) {
        if (hideRmCommandsInDiscord != null && hideRmCommandsInDiscord && msg.contains(": !")) {
            log.info("Ignore message with RM command in discord publisher: " + msg);
            return;
        }
        gatewayDiscordClient.getChannelById(Snowflake.of(chatChannelID))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(createDiscordFormattedMessage(msg, chatModel)))
                .subscribe();

        if (StringUtils.hasText(publicChatChannelID)) {
            TeamEnum teamEnum = TeamEnum.findByCode(chatModel.getTeam());
            if (teamEnum == TeamEnum.GLOBAL || teamEnum == TeamEnum.UNKOWN) {
                gatewayDiscordClient.getChannelById(Snowflake.of(publicChatChannelID))
                        .ofType(MessageChannel.class)
                        .flatMap(channel -> channel.createMessage(createDiscordFormattedMessage(msg, chatModel)))
                        .subscribe();
            }
        }
    }

    public void publishDiscordAdminHelpMessage(final String msg, final ChatModel chatModel) {
        if (StringUtils.hasText(adminChannelId)) {
            gatewayDiscordClient.getChannelById(Snowflake.of(adminChannelId))
                    .ofType(MessageChannel.class)
                    .flatMap(channel -> channel.createMessage((StringUtils.hasText(adminHelpMentionId) ? "<@&" + adminHelpMentionId + "> " : "") + msg))
                    .subscribe();
        } else {
            log.info("Admin-Help message dropped, because no admin_channel_id is set: " + msg);
        }
    }
}
