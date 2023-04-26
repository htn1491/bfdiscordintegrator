package htn.bfdiscordintegration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import javax.annotation.PreDestroy;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AttackLogTailerService {

    private static final Logger log = LogManager.getLogger(AttackLogTailerService.class);

    @Value("${attack_log_filepath}")
    private String attackLogFilepath;

    @Autowired
    private DiscordIntegratorService discordIntegratorService;

    private Tailer tailer = null;

    @PostConstruct
    public void tailAttackFIle() throws IOException {
        if (!StringUtils.hasText(attackLogFilepath)) {
            log.info("attack_log_filepath is not set. Skipping this function.");
            return;
        }
        File f = new File(attackLogFilepath);
        if (!f.exists() || f.isDirectory() || !f.canRead()) {
            log.warn("Attack Log Filepath " + attackLogFilepath + " cannot be read. Skipping!");
            return;
        }

        TailerListener listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                discordIntegratorService.publishDiscordAttackMessage(line);
            }
        };

        tailer = new Tailer(f, listener, 100, true);

        log.info("Tailer start");
        tailer.run();
        log.info("Tailer started");
    }

    @PreDestroy
    public void preDestroy() {
        if (tailer != null) {
            log.info("Stop tailing attack_log_filepath...");
            tailer.stop();
        }
    }
}
