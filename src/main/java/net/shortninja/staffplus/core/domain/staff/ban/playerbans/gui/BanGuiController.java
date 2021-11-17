package net.shortninja.staffplus.core.domain.staff.ban.playerbans.gui;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.gui.AsyncGui;
import be.garagepoort.mcioc.gui.CurrentAction;
import be.garagepoort.mcioc.gui.GuiAction;
import be.garagepoort.mcioc.gui.GuiController;
import be.garagepoort.mcioc.gui.GuiParam;
import be.garagepoort.mcioc.gui.model.TubingGui;
import net.shortninja.staffplus.core.application.config.Messages;
import net.shortninja.staffplus.core.application.session.OnlinePlayerSession;
import net.shortninja.staffplus.core.application.session.OnlineSessionsManager;
import net.shortninja.staffplus.core.common.exceptions.PlayerNotFoundException;
import net.shortninja.staffplus.core.domain.player.PlayerManager;
import net.shortninja.staffplus.core.domain.staff.ban.playerbans.Ban;
import net.shortninja.staffplus.core.domain.staff.ban.playerbans.BanService;
import net.shortninja.staffplus.core.domain.staff.ban.playerbans.gui.views.BannedPlayersViewBuilder;
import net.shortninja.staffplus.core.domain.staff.ban.playerbans.gui.views.ManageBannedPlayerViewBuilder;
import net.shortninja.staffplus.core.domain.staff.ban.playerbans.gui.views.PlayerBanHistoryViewBuilder;
import net.shortninja.staffplusplus.session.SppPlayer;
import org.bukkit.entity.Player;

import static be.garagepoort.mcioc.gui.AsyncGui.async;

@IocBean
@GuiController
public class BanGuiController {

    private static final String CANCEL = "cancel";

    private final BannedPlayersViewBuilder bannedPlayersViewBuilder;
    private final ManageBannedPlayerViewBuilder manageBannedPlayerViewBuilder;
    private final PlayerBanHistoryViewBuilder playerBanHistoryViewBuilder;
    private final Messages messages;
    private final BanService banService;
    private final OnlineSessionsManager sessionManager;
    private final PlayerManager playerManager;

    public BanGuiController(BannedPlayersViewBuilder bannedPlayersViewBuilder,
                            ManageBannedPlayerViewBuilder manageBannedPlayerViewBuilder,
                            PlayerBanHistoryViewBuilder playerBanHistoryViewBuilder,
                            Messages messages,
                            BanService banService,
                            OnlineSessionsManager sessionManager,
                            PlayerManager playerManager) {
        this.bannedPlayersViewBuilder = bannedPlayersViewBuilder;
        this.manageBannedPlayerViewBuilder = manageBannedPlayerViewBuilder;
        this.playerBanHistoryViewBuilder = playerBanHistoryViewBuilder;
        this.messages = messages;
        this.banService = banService;
        this.sessionManager = sessionManager;
        this.playerManager = playerManager;
    }

    @GuiAction("manage-bans/view/overview")
    public AsyncGui<TubingGui> getBannedPlayersOverview(@GuiParam(value = "page", defaultValue = "0") int page,
                                                        @CurrentAction String currentAction,
                                                        @GuiParam("backAction") String backAction) {
        return async(() -> bannedPlayersViewBuilder.buildGui(page, currentAction, backAction));
    }

    @GuiAction("manage-bans/view/detail")
    public AsyncGui<TubingGui> getBanDetailView(@GuiParam("banId") int banId, @GuiParam("backAction") String backAction, @CurrentAction String currentAction) {
        return async(() -> {
            Ban ban = banService.getById(banId);
            return manageBannedPlayerViewBuilder.buildGui(ban, backAction, currentAction);
        });
    }

    @GuiAction("manage-bans/view/history")
    public AsyncGui<TubingGui> getBansPlayersHistory(@GuiParam(value = "page", defaultValue = "0") int page,
                                                     @CurrentAction String currentAction,
                                                     @GuiParam("targetPlayerName") String targetPlayerName,
                                                     @GuiParam("backAction") String backAction) {
        SppPlayer target = playerManager.getOnOrOfflinePlayer(targetPlayerName).orElseThrow(() -> new PlayerNotFoundException(targetPlayerName));
        return async(() -> playerBanHistoryViewBuilder.buildGui(target, page, currentAction, backAction));
    }


    @GuiAction("manage-bans/unban")
    public void unban(Player player, @GuiParam("banId") int banId) {
        messages.send(player, "&1=====================================================", messages.prefixGeneral);
        messages.send(player, "&6         You have chosen to unban this player", messages.prefixGeneral);
        messages.send(player, "&6Type your reason for unbanning this player in chat", messages.prefixGeneral);
        messages.send(player, "&6        Type \"cancel\" to cancel the unban ", messages.prefixGeneral);
        messages.send(player, "&1=====================================================", messages.prefixGeneral);

        OnlinePlayerSession playerSession = sessionManager.get(player);
        playerSession.setChatAction((player1, message) -> {
            if (message.equalsIgnoreCase(CANCEL)) {
                messages.send(player, "&CYou have cancelled unbanning this player", messages.prefixReports);
                return;
            }
            banService.unban(player, banId, message);
        });
    }

}