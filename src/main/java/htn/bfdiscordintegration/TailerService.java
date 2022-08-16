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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 *
 * @author Robert
 */
@Service
public class TailerService extends TailerListenerAdapter {

    private static final Logger log = LogManager.getLogger(TailerService.class);

    @Value("${eventlog_file_path}")
    private String eventlogFilePath;

    @Value("${admin_help_prefix}")
    private String adminHelpPrefix;

    @Value("${chatlogs_export_location}")
    private String chatlogExportLocation;

    @Autowired
    private DiscordIntegratorService discordIntegratorService;

    @Autowired
    private EventlogMapper eventlogMapper;

    private boolean fileNotFoundPrinted = false;

    private String currentFileName = "";

    private Thread tailerThread = null;

    private String elementCache = "";
    private boolean collectingElement = false;

    @PostConstruct
    public void tail() throws IOException, InterruptedException {

        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(eventlogFilePath);

        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        WatchKey key;
        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.context().toString().startsWith("ev_") && event.context().toString().endsWith(".xml")) {
                    log.info("Detected new eventlog file " + event.context().toString());
                    //extracting time from filename like ev_15567-20220816_1323.xml
                    Pattern p = Pattern.compile("^ev_.*-(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d).*$");
                    Matcher matcherCurrent = p.matcher(currentFileName);
                    Matcher matcherNew = p.matcher(event.context().toString());
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
                    try {
                        Date dateCurrent = sdf.parse(matcherCurrent.group(1));
                        Date dateNew = sdf.parse(matcherNew.group(1));
                        if(dateNew.before(dateCurrent)) {
                            //Created eventlog is older, so ignore it
                            log.info("Ignoring this file, because the timestamp detected is lower than current file");
                            continue;
                        }
                    } catch(IndexOutOfBoundsException | ParseException e) {
                        //Nothing to do
                    }
                    if (tailerThread != null) {
                        log.info("Stop reading of current file");
                        tailerThread.interrupt();
                    }
                    String fullFilepath = (eventlogFilePath.endsWith("/") ? eventlogFilePath + event.context().toString() : eventlogFilePath + "/" + event.context().toString());
                    log.info("Start reading of file " + fullFilepath);
                    currentFileName = event.context().toString();
                    Tailer tailer = new Tailer(new File(fullFilepath), this, 500);
                    tailerThread = new Thread(tailer);
                    tailerThread.start();
                }
            }
            key.reset();
        }
    }

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
            log.info("File not found");
        }
        super.fileNotFound();
    }

    @Override
    public void handle(String line) {
        fileNotFoundPrinted = false;
        log.trace("Received line: " + line);
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
            log.trace("Ignore blank line");
        }
    }

    private void handleDiscordMessage(final ChatModel chatModel) {
        if (StringUtils.hasText(adminHelpPrefix) && chatModel.getText().trim().startsWith(adminHelpPrefix)) {
            discordIntegratorService.publishDiscordAdminHelpMessage("INGAME ADMIN CALL: " + formatMessage(chatModel).replace(adminHelpPrefix, ""));
        } else {
            discordIntegratorService.publishDiscordMessage(formatMessage(chatModel));
        }
    }

    private String formatMessage(final ChatModel chatModel) {
        return "[" + TeamEnum.findByCode(chatModel.getTeam()).getPrintValue() + "] " + (chatModel.getPlayerModel() == null ? "unknown" : chatModel.getPlayerModel().getName()) + ": " + chatModel.getText();
    }

    private void handlePersistMessage(final ChatModel chatModel) {
        if (!StringUtils.hasText(chatlogExportLocation)) {
            return;
        }
        String targetFilePath = chatlogExportLocation + currentFileName + ".chatlog";
        try {

            File output = new File(targetFilePath);
            FileOutputStream fos = new FileOutputStream(output, true);

            log.trace("Writing to chatlog file " + targetFilePath);
            try ( BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos))) {
                log.debug("Write line: " + chatModel.getFormattedTimestamp() + " : # " + formatMessage(chatModel));
                bw.write(chatModel.getFormattedTimestamp() + " : # " + formatMessage(chatModel));
                bw.newLine();
            }
            log.trace("Write completed");
        } catch (IOException e) {
            log.warn("Error writing chatlog to new file " + targetFilePath + "! Is it writable?");
        }
    }

}
