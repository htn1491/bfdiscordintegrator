package htn.bfdiscordintegration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.apache.commons.io.input.Tailer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TailerService {

    private static final Logger log = LogManager.getLogger(TailerService.class);

    @Value("${eventlog_file_path}")
    private String eventlogFilePath;

    @Value("${admin_help_prefix}")
    private String adminHelpPrefix;

    @Value("${chatlogs_export_location}")
    private String chatlogExportLocation;
    
    @Value("${move_after_read_locations}")
    private String moveAfterReadLocations;
    
    @Value("${delete_after_read_and_move}")
    private Boolean deleteAfterReadAndMove;
    
    @Value("${publish_round_stats}")
    private Boolean publishRoundStats;

    private String currentFileName = "";

    private Thread tailerThread = null;

    @Autowired
    private DiscordIntegratorService discordIntegratorService;

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
                    if (StringUtils.hasText(currentFileName)) {
                        log.info("Compare with current file " + currentFileName);
                        Pattern p = Pattern.compile("^ev_.*-(\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d).*$");
                        Matcher matcherCurrent = p.matcher(currentFileName);
                        matcherCurrent.matches();
                        Matcher matcherNew = p.matcher(event.context().toString());
                        matcherNew.matches();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
                        try {
                            Date dateNew = sdf.parse(matcherNew.group(1));
                            Date dateCurrent = sdf.parse(matcherCurrent.group(1));
                            if (dateNew.before(dateCurrent)) {
                                //Created eventlog is older, so ignore it
                                log.info("Ignoring this file, because the timestamp detected is lower than current file");
                                continue;
                            }
                        } catch (IndexOutOfBoundsException | ParseException e) {
                            //Nothing to do
                        }
                    }
                    if (tailerThread != null) {
                        log.info("Stop reading of current file");
                        tailerThread.interrupt();
                        if(StringUtils.hasText(moveAfterReadLocations)) {
                            String[] locations = moveAfterReadLocations.split(",");
                            String fullOldFilepath = (eventlogFilePath.endsWith("/") ? eventlogFilePath + currentFileName : eventlogFilePath + "/" + currentFileName);
                            for(String location : locations) {
                                if(!location.endsWith("/")) {
                                    location = location+"/";
                                }
                                log.info("Copying old file to " + location + currentFileName);
                                Files.copy(Paths.get(fullOldFilepath), Paths.get(location + currentFileName), StandardCopyOption.REPLACE_EXISTING);
                            }
                            if(deleteAfterReadAndMove) {
                                log.info("Deleting file " + fullOldFilepath);
                                Files.delete(Paths.get(fullOldFilepath));
                            }
                        }
//                        log.info("Publish end round to discord");
//                        discordIntegratorService.publishEndRound();
                    }
                    String fullFilepath = (eventlogFilePath.endsWith("/") ? eventlogFilePath + event.context().toString() : eventlogFilePath + "/" + event.context().toString());
                    log.info("Start reading of file " + fullFilepath);
                    currentFileName = event.context().toString();
//                    Tailer tailer = new Tailer(new File(fullFilepath), new TailerThread(event.context().toString(), discordIntegratorService, adminHelpPrefix, chatlogExportLocation), 500);
                    tailerThread = new CustomTailerThread(fullFilepath, event.context().toString(), discordIntegratorService, adminHelpPrefix, chatlogExportLocation, publishRoundStats);
                    tailerThread.start();
                }
            }
            key.reset();
        }
    }

}
