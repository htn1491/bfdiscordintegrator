/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package htn.bfdiscordintegration;

import java.io.File;
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

    @Value("${bvsmd_log_file}")
    private String logFile;

    @Value("${admin_help_prefix}")
    private String adminHelpPrefix;

    @Autowired
    private DiscordIntegratorService discordIntegratorService;

    private boolean initialLoadFinished = false;

    @PostConstruct
    public void tail() {
        Tailer tailer = new Tailer(new File(logFile), this, 500);
        tailer.run();
    }

    @Override
    public void endOfFileReached() {
        initialLoadFinished = true;
        super.endOfFileReached();
    }

    @Override
    public void fileRotated() {
        log.info("File rotated");
        super.fileRotated();
    }

    @Override
    public void fileNotFound() {
        log.info("File not found");
        super.fileNotFound();
    }

    @Override
    public void handle(String line) {
        if (initialLoadFinished) {
            log.debug("Received line: "+line);
            if (StringUtils.hasText(line)) {
                if (StringUtils.hasText(adminHelpPrefix) && line.trim().startsWith(adminHelpPrefix)) {
                    discordIntegratorService.publishDiscordAdminHelpMessage(line.trim().replace("!admin ", ""));
                } else {
                    discordIntegratorService.publishDiscordMessage(line.trim());
                }
            } else {
                log.trace("Ignore blank line");
            }
        }
    }

}
