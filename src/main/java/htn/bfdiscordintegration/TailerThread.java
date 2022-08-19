/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package htn.bfdiscordintegration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Optional;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

/**
 *
 * @author Robert
 */
public class TailerThread extends TailerListenerAdapter {
    
    private static final Logger log = LogManager.getLogger(TailerThread.class);

    @Value("${admin_help_prefix}")
    private String adminHelpPrefix;

    @Value("${chatlogs_export_location}")
    private String chatlogExportLocation;

    @Autowired
    private DiscordIntegratorService discordIntegratorService;

    @Autowired
    private EventlogMapper eventlogMapper;

    private boolean fileNotFoundPrinted = false;

    private String elementCache = "";
    private boolean collectingElement = false;
    
    private final String filename;

    @Override
    public void endOfFileReached() {
        super.endOfFileReached();
    }

    @Override
    public void fileRotated() {
        super.fileRotated();
    }

    @Override
    public void fileNotFound() {
        if (!fileNotFoundPrinted) {
            fileNotFoundPrinted = true;
            log.info("Tailer: File not found");
        }
        super.fileNotFound();
    }

    public TailerThread(final String filename) {
        this.filename = filename;
    }

    @Override
    public void handle(String line) {
        fileNotFoundPrinted = false;
        log.debug("Received line: " + line);
        if (StringUtils.hasText(line)) {
            //New bf:log begin?
            if (line.startsWith("<bf:log engine")) {
                eventlogMapper.reset();
                eventlogMapper.handleBeginTimestamp(line);
                return;
            }
            if (line.startsWith("<bf:event ")) {
                collectingElement = true;
                elementCache = "";
            }

            if (collectingElement) {
                elementCache += line;
            }

            //Event ended, try to parse the event
            if (line.equals("</bf:event>")) {
                Optional<ChatModel> chatModelOpt = eventlogMapper.handleBfEvent(elementCache);
                if (chatModelOpt.isPresent()) {
                    ChatModel chatModel = chatModelOpt.get();
                    log.info(chatModel);
                    handleDiscordMessage(chatModel);
                    handlePersistMessage(chatModel);
                }
                elementCache = "";
                collectingElement = false;
            }

        } else {
            log.debug("Ignore blank line");
        }
    }

    private void handleDiscordMessage(final ChatModel chatModel) {
        if (StringUtils.hasText(adminHelpPrefix) && chatModel.getText().trim().startsWith(adminHelpPrefix)) {
            discordIntegratorService.publishDiscordAdminHelpMessage(TeamEnum.findByCode(chatModel.getTeam()).formatDiscordValue("INGAME ADMIN CALL: " + formatMessage(chatModel).replace(adminHelpPrefix, "")));
        } else {
            discordIntegratorService.publishDiscordMessage(TeamEnum.findByCode(chatModel.getTeam()).formatDiscordValue(formatMessage(chatModel)));
        }
    }

    private String formatMessage(final ChatModel chatModel) {
        return "[" + TeamEnum.findByCode(chatModel.getTeam()).getPrintValue() + "] " + (chatModel.getPlayerModel() == null ? "unknown" : chatModel.getPlayerModel().getName()) + ": " + chatModel.getText();
    }

    private void handlePersistMessage(final ChatModel chatModel) {
        if (!StringUtils.hasText(chatlogExportLocation)) {
            return;
        }
        String targetFilePath = chatlogExportLocation + filename + ".chatlog";
        try {

            File output = new File(targetFilePath);
            FileOutputStream fos = new FileOutputStream(output, true);

            log.debug("Writing to chatlog file " + targetFilePath);
            try ( BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos))) {
                log.debug("Write line: " + chatModel.getFormattedTimestamp() + " : # " + formatMessage(chatModel));
                bw.write(chatModel.getFormattedTimestamp() + " : # " + formatMessage(chatModel));
                bw.newLine();
            }
            log.debug("Write completed");
        } catch (IOException e) {
            log.warn("Error writing chatlog to new file " + targetFilePath + "! Is it writable?");
        }
    }
}
