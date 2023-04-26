package htn.bfdiscordintegration;

import java.io.IOException;
import javax.annotation.PostConstruct;
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

    @PostConstruct
    public void tailAttackFile() throws IOException {
        if(!StringUtils.hasText(attackLogFilepath)) {
            log.info("attack_log_filepath is not set. Skipping this function.");
            return;
        }
        
        AttackTailerThread thread = new AttackTailerThread(attackLogFilepath, discordIntegratorService);
        thread.start();
    }
}
