/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package htn.bfdiscordintegration;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.MessageData;
import java.time.Instant;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author Robert
 */
//@Service
public class DiscordIntegratorService {

    private static final Logger log = LogManager.getLogger(DiscordIntegratorService.class);

    @Value("${discord_bot_token}")
    private String token;

    @Value("${chat_channel_id}")
    private String chatChannelID;

    @Value("${admin_help_channel_id}")
    private String adminChannelId;

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

    public void publishDiscordMessage(final String msg) {
        gatewayDiscordClient.getChannelById(Snowflake.of(chatChannelID))
                .ofType(MessageChannel.class)
                .flatMap(channel -> channel.createMessage(msg))
                .subscribe();
    }

    public void publishDiscordAdminHelpMessage(final String msg) {
        if (StringUtils.hasText(adminChannelId)) {
            gatewayDiscordClient.getChannelById(Snowflake.of(adminChannelId))
                    .ofType(MessageChannel.class)
                    .flatMap(channel -> channel.createMessage(msg))
                    .subscribe();
        } else {
            log.info("Admin-Help message dropped, because no admin_channel_id is set: " + msg);
        }
    }
}
