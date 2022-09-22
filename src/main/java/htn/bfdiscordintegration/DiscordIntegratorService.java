package htn.bfdiscordintegration;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
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
    
    public void publishEndRound() {
        
        EmbedCreateSpec spec = EmbedCreateSpec.builder()
                .color(Color.DARK_GRAY)
                .description("--- ROUND HAS ENDED ---")
                .build();
        gatewayDiscordClient.getChannelById(Snowflake.of(chatChannelID))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(spec))
                .subscribe();
    }

    public void publishDiscordMessage(final String msg, final ChatModel chatModel) {
        if(hideRmCommandsInDiscord != null && hideRmCommandsInDiscord && msg.contains(": !")) {
            log.info("Ignore message with RM command in discord publisher: "+msg);
            return;
        }
        gatewayDiscordClient.getChannelById(Snowflake.of(chatChannelID))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(createDiscordFormattedMessage(msg, chatModel)))
                .subscribe();
    }

    public void publishDiscordAdminHelpMessage(final String msg, final ChatModel chatModel) {
        if (StringUtils.hasText(adminChannelId)) {
                gatewayDiscordClient.getChannelById(Snowflake.of(adminChannelId))
                    .ofType(MessageChannel.class)
                    .flatMap(channel -> channel.createMessage((StringUtils.hasText(adminHelpMentionId) ? "<@&"+adminHelpMentionId+"> " : "")+msg))
                    .subscribe();
        } else {
            log.info("Admin-Help message dropped, because no admin_channel_id is set: " + msg);
        }
    }
}
