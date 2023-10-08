package searchengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@ConfigurationProperties(prefix = "user-agent-settings")
public class UserAgentsCfg {
    private List<String> users;
    private String referrer;
    private final Random random = new Random();

    public String getRandomUser() {
        return users.get(random.nextInt(users.size()));
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getReferrer() {
        return referrer;
    }
}
