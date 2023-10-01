package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
@ConfigurationProperties(prefix = "user-agent-settings")
public class UserAgentCfg {
    private List<String> users;
    private final Random random = new Random();

    public String getRandomUser() {
        return users.get(random.nextInt(users.size()));
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }
}
