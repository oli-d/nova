package ch.squaredesk.nova.service.admin;

import ch.squaredesk.nova.service.admin.messages.Info;
import ch.squaredesk.nova.service.admin.messages.SupportedCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdminInfoCenter {
    private final List<AdminCommandConfig> configs = new CopyOnWriteArrayList<>();
    private final AdminUrlCalculator urlCalculator;

    public AdminInfoCenter(AdminUrlCalculator urlCalculator) {
        this.urlCalculator = urlCalculator;
    }

    Info getInfo() {
        return new Info(
                configs.stream()
                        .map(config -> new SupportedCommand(urlCalculator.urlFor(config), config.parameterNames))
                        .toArray(SupportedCommand[]::new));
    }

    void register(AdminCommandConfig adminCommandConfig) {
        configs.add(adminCommandConfig);
    }

    List<AdminCommandConfig> getConfigs() {
        return new ArrayList<>(configs);
    }
}
