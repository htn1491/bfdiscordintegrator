package htn.bfdiscordintegration;

import java.io.File;
import java.io.IOException;
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
                    }
                    String fullFilepath = (eventlogFilePath.endsWith("/") ? eventlogFilePath + event.context().toString() : eventlogFilePath + "/" + event.context().toString());
                    log.info("Start reading of file " + fullFilepath);
                    currentFileName = event.context().toString();
                    Tailer tailer = new Tailer(new File(fullFilepath), new TailerThread(event.context().toString(), discordIntegratorService, adminHelpPrefix, chatlogExportLocation), 500);
                    tailerThread = new Thread(tailer);
                    tailerThread.start();
                }
            }
            key.reset();
        }
    }

}
