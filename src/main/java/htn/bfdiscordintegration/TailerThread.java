package htn.bfdiscordintegration;

import htn.bfdiscordintegration.models.ChatModel;
import htn.bfdiscordintegration.models.enums.TeamEnum;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Optional;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

public class TailerThread extends TailerListenerAdapter {
    
    private static final Logger log = LogManager.getLogger(TailerThread.class);

    private final String adminHelpPrefix;

    private final String chatlogExportLocation;

    private final DiscordIntegratorService discordIntegratorService;

    private final EventlogMapper eventlogMapper;

    private boolean fileNotFoundPrinted = false;

    private String elementCache = "";
    private boolean collectingElement = false;
    
    private final String filename;
    
    public TailerThread(final String filename, final DiscordIntegratorService discordIntegratorService, final String adminHelpPrefix, final String chatlogExportLocation) {
        this.filename = filename;
        this.eventlogMapper = new EventlogMapper();
        this.discordIntegratorService = discordIntegratorService;
        this.adminHelpPrefix = adminHelpPrefix;
        this.chatlogExportLocation = chatlogExportLocation;
    }
    
    @Override
    public void endOfFileReached() {
        log.debug("Tailer: endOfFileReached");
        super.endOfFileReached();
    }

    @Override
    public void fileRotated() {
        log.debug("Tailer: fileRotated");
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

    @Override
    public void handle(String line) {
        fileNotFoundPrinted = false;
        log.debug("Received line: " + line);
        if (StringUtils.hasText(line)) {
            //New bf:log begin?
            if (line.startsWith("<bf:log engine")) {
                log.debug("New start of log file detected");
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
                log.trace("Clearing element cache");
                elementCache = "";
                collectingElement = false;
            }

        } else {
            log.trace("Ignore blank line");
        }
    }

    private void handleDiscordMessage(final ChatModel chatModel) {
        if (StringUtils.hasText(adminHelpPrefix) && chatModel.getText().trim().startsWith(adminHelpPrefix)) {
            discordIntegratorService.publishDiscordAdminHelpMessage("INGAME ADMIN CALL: " + formatMessage(chatModel, false).replace(adminHelpPrefix, ""), chatModel);
        } else {
            discordIntegratorService.publishDiscordMessage(formatMessage(chatModel, false), chatModel);
        }
    }

    private String formatMessage(final ChatModel chatModel, final boolean includeTeam) {
        String msg = "";
        if(includeTeam) {
            msg += "[" + TeamEnum.findByCode(chatModel.getTeam()).getPrintValue() + "] ";
        }
        msg += (chatModel.getPlayerModel() == null ? "unknown" : chatModel.getPlayerModel().getName()) + ": " + chatModel.getText();
        return msg;
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
                log.debug("Write line: " + chatModel.getFormattedTimestamp() + " : # " + formatMessage(chatModel, true));
                bw.write(chatModel.getFormattedTimestamp() + " : # " + formatMessage(chatModel, true));
                bw.newLine();
            }
            log.debug("Write completed");
        } catch (IOException e) {
            log.warn("Error writing chatlog to new file " + targetFilePath + "! Is it writable?");
        }
    }
}
