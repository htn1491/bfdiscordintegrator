package htn.bfdiscordintegration;

import htn.bfdiscordintegration.models.ChatModel;
import htn.bfdiscordintegration.models.RoundStatModel;
import htn.bfdiscordintegration.models.enums.TeamEnum;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Optional;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

public class CustomTailerThread extends Thread {

    private static final Logger log = LogManager.getLogger(CustomTailerThread.class);

    private final String adminHelpPrefix;

    private final String chatlogExportLocation;

    private final DiscordIntegratorService discordIntegratorService;

    private final EventlogMapper eventlogMapper;

    private String elementCache = "";
    private boolean collectingElement = false;

    private final String fullFilepath;
    private final String filename;
    private final Boolean publishRoundStats;
    
    private String handledMapName = null;

    public CustomTailerThread(final String fullFilepath, final String filename, final DiscordIntegratorService discordIntegratorService, final String adminHelpPrefix, final String chatlogExportLocation, final Boolean publishRoundStats) {
        this.fullFilepath = fullFilepath;
        this.filename = filename;
        this.eventlogMapper = new EventlogMapper();
        this.discordIntegratorService = discordIntegratorService;
        this.adminHelpPrefix = adminHelpPrefix;
        this.chatlogExportLocation = chatlogExportLocation;
        this.publishRoundStats = publishRoundStats;
    }

    @Override
    public void run() {
        try ( BufferedReader br = new BufferedReader(new FileReader(fullFilepath))) {
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    Thread.sleep(500);
                } else {
                    handle(line);
                }
            }
        } catch (FileNotFoundException e) {
            log.warn("File not found", e);
        } catch (Exception e) {
            log.info("Tailer thread canceled: ", e);
        }

    }

    private void handle(String line) {
        log.trace("Received line: " + line);
        if (StringUtils.hasText(line)) {
            //New bf:log begin?
            if (line.startsWith("<bf:log ")) {
                log.debug("New start of log file detected");
                handledMapName = null;
                eventlogMapper.reset();
                eventlogMapper.handleBeginTimestamp(line);
                return;
            }

            if (line.startsWith("<bf:event ")) {
                collectingElement = true;
                elementCache = "";
            }

            if (publishRoundStats) {
                if (line.startsWith("<bf:server")) {
                    collectingElement = true;
                    elementCache = "";
                }

                if (line.startsWith("<bf:roundstats ")) {
                    collectingElement = true;
                    elementCache = "";
                }
            }

            if (collectingElement) {
                elementCache += line;
            }

            //Server ended, try to parse the server
            if (publishRoundStats && line.equals("</bf:server>")) {
                handledMapName = eventlogMapper.extractMapName(elementCache);
                log.trace("Clearing element cache");
                elementCache = "";
                collectingElement = false;
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

            //Roundstats ended, try to parse the roundstats
            if (publishRoundStats && line.equals("</bf:roundstats>")) {
                Optional<RoundStatModel> roundStatModelOpt = eventlogMapper.handleRoundStats(elementCache);
                if (roundStatModelOpt.isPresent()) {
                    RoundStatModel roundStatModel = roundStatModelOpt.get();
                    roundStatModel.setMapName(handledMapName);
                    log.info(roundStatModel);
                    handleDiscordMessage(roundStatModel);
                }
                log.trace("Clearing element cache");
                elementCache = "";
                collectingElement = false;
            }

        } else {
            log.trace("Ignore blank line");
        }
    }

    private void handleDiscordMessage(final RoundStatModel roundStatModel) {
        discordIntegratorService.publishRoundStats(roundStatModel);
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
        if (includeTeam) {
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
