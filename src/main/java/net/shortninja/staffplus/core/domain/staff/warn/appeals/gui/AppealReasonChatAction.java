package net.shortninja.staffplus.core.domain.staff.warn.appeals.gui;

import be.garagepoort.mcioc.IocContainer;
import net.shortninja.staffplus.core.common.config.Messages;
import net.shortninja.staffplus.core.common.gui.IAction;
import net.shortninja.staffplus.core.common.utils.MessageCoordinator;
import net.shortninja.staffplus.core.domain.staff.warn.appeals.AppealService;
import net.shortninja.staffplus.core.domain.staff.warn.warnings.Warning;
import net.shortninja.staffplus.core.session.PlayerSession;
import net.shortninja.staffplus.core.session.SessionManagerImpl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AppealReasonChatAction implements IAction {
    private static final String CANCEL = "cancel";
    private final Messages messages = IocContainer.get(Messages.class);
    private final MessageCoordinator messageCoordinator = IocContainer.get(MessageCoordinator.class);
    private final SessionManagerImpl sessionManager = IocContainer.get(SessionManagerImpl.class);
    private final AppealService appealService = IocContainer.get(AppealService.class);

    private final Warning warning;

    public AppealReasonChatAction(Warning warning) {
        this.warning = warning;
    }

    @Override
    public void click(Player player, ItemStack item, int slot) {
        messageCoordinator.send(player, "&1=====================================================", messages.prefixGeneral);
        messageCoordinator.send(player, "&6         You have chosen to appeal this warning", messages.prefixGeneral);
        messageCoordinator.send(player, "&6            Type your appeal reason in chat", messages.prefixGeneral);
        messageCoordinator.send(player, "&6         Type \"cancel\" to cancel appealing ", messages.prefixGeneral);
        messageCoordinator.send(player, "&1=====================================================", messages.prefixGeneral);

        PlayerSession playerSession = sessionManager.get(player.getUniqueId());

        playerSession.setChatAction((player1, input) -> {
            if (input.equalsIgnoreCase(CANCEL)) {
                messageCoordinator.send(player, "&CYou have cancelled your appeal", messages.prefixWarnings);
                return;
            }

            appealService.addAppeal(player, warning, input);
        });
    }

    @Override
    public boolean shouldClose(Player player) {
        return true;
    }
}
