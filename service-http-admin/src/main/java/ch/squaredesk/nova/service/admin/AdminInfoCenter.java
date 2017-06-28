package ch.squaredesk.nova.service.admin;

import ch.squaredesk.nova.service.admin.messages.Info;
import ch.squaredesk.nova.service.admin.messages.SupportedCommand;
import ch.squaredesk.nova.tuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdminInfoCenter {
    private final List<Pair<AdminCommandConfig, SupportedCommand>> configs = new CopyOnWriteArrayList<>();
    private final AdminUrlCalculator urlCalculator;

    public AdminInfoCenter(AdminUrlCalculator urlCalculator) {
        this.urlCalculator = urlCalculator;
    }

    private SupportedCommand calcSupportedCommandFrom (AdminCommandConfig acc) {
        return new SupportedCommand(urlCalculator.urlFor(acc), acc.parameterNames);
    }

    void register(AdminCommandConfig adminCommandConfig) {
        configs.add(new Pair<>(adminCommandConfig, calcSupportedCommandFrom(adminCommandConfig)));
    }

    Info getInfo() {
        return new Info(
                configs.stream()
                        .map(config -> config._2)
                        .toArray(SupportedCommand[]::new));
    }

    List<Pair<AdminCommandConfig, SupportedCommand>> getConfigs() {
        return new ArrayList<>(configs);
    }
}
