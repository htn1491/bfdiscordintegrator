package htn.bfdiscordintegration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Robert
 */
public class AttackTailerThread extends Thread {

    private static final Logger log = LogManager.getLogger(AttackTailerThread.class);

    private final String attackLogFilepath;
    private final DiscordIntegratorService discordIntegratorService;

    private boolean initialReadDone = false;

    public AttackTailerThread(String attackLogFilepath, DiscordIntegratorService discordIntegratorService) {
        this.attackLogFilepath = attackLogFilepath;
        this.discordIntegratorService = discordIntegratorService;
    }

    @Override
    public void run() {
        try {
            waitForFile();
            try ( BufferedReader br = new BufferedReader(new FileReader(attackLogFilepath))) {
                String line;
                while (true) {
                    line = br.readLine();
                    if (line == null) {
                        initialReadDone = true;
                        Thread.sleep(500);
                    } else {
                        if (initialReadDone) {
                            discordIntegratorService.publishDiscordAttackMessage(line);
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                log.warn("File not found", e);
            } catch (Exception e) {
                log.info("Tailer thread canceled: ", e);
            }
        } catch (InterruptedException e) {
            log.info("Thread interrupted", e);
        }
    }

    private void waitForFile() throws InterruptedException {
        File f = new File(attackLogFilepath);
        if (!f.exists() || f.isDirectory() || !f.canRead()) {
            log.warn("Attack Log Filepath is configured " + attackLogFilepath + ", but cannot be read. Try again in 1min!");
            Thread.sleep(60000);
            waitForFile();
        }
    }
}
