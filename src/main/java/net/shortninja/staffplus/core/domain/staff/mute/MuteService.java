package net.shortninja.staffplus.core.domain.staff.mute;

import be.garagepoort.mcioc.IocBean;
import be.garagepoort.mcioc.IocMultiProvider;
import net.shortninja.staffplus.core.application.config.Options;
import net.shortninja.staffplus.core.common.JavaUtils;
import net.shortninja.staffplus.core.common.exceptions.BusinessException;
import net.shortninja.staffplus.core.common.permissions.PermissionHandler;
import net.shortninja.staffplus.core.domain.staff.infractions.Infraction;
import net.shortninja.staffplus.core.domain.staff.infractions.InfractionInfo;
import net.shortninja.staffplus.core.domain.staff.infractions.InfractionProvider;
import net.shortninja.staffplus.core.domain.staff.infractions.InfractionType;
import net.shortninja.staffplus.core.domain.staff.mute.config.MuteConfiguration;
import net.shortninja.staffplus.core.domain.staff.mute.database.MuteRepository;
import net.shortninja.staffplusplus.mute.MuteEvent;
import net.shortninja.staffplusplus.mute.MuteExtensionEvent;
import net.shortninja.staffplusplus.mute.MuteReductionEvent;
import net.shortninja.staffplusplus.mute.UnmuteEvent;
import net.shortninja.staffplusplus.session.SppPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static net.shortninja.staffplus.core.common.Constants.CONSOLE_UUID;
import static net.shortninja.staffplus.core.common.utils.BukkitUtils.sendEvent;

@IocBean
@IocMultiProvider(InfractionProvider.class)
public class MuteService implements InfractionProvider, net.shortninja.staffplusplus.mute.MuteService {

    private static final String LIMIT = ".limit";
    private final PermissionHandler permission;
    private final MuteRepository muteRepository;
    private final Options options;
    private final MuteConfiguration muteConfiguration;

    public MuteService(PermissionHandler permission, MuteRepository muteRepository, Options options, MuteConfiguration muteConfiguration) {
        this.permission = permission;
        this.muteRepository = muteRepository;
        this.options = options;
        this.muteConfiguration = muteConfiguration;
    }

    public void permMute(CommandSender issuer, SppPlayer playerToMute, String reason, boolean softMute) {
        mute(issuer, playerToMute, reason, null, softMute);
    }

    public void tempMute(CommandSender issuer, SppPlayer playerToMute, Long durationInMillis, String reason, boolean softMute) {
        this.checkDurationPermission(issuer, durationInMillis);
        mute(issuer, playerToMute, reason, durationInMillis, softMute);
    }

    public Optional<Mute> getMuteByMutedUuid(UUID playerUuid) {
        return muteRepository.findActiveMute(playerUuid);
    }

    public Mute getById(int muteId) {
        return muteRepository.findActiveMute(muteId).orElseThrow(() -> new BusinessException("No mute found with this id"));
    }

    public List<Mute> getAllPaged(int offset, int amount) {
        return muteRepository.getActiveMutes(offset, amount);
    }

    public List<Mute> getAllActiveMutes(List<Player> players) {
        if (players.isEmpty()) {
            return Collections.emptyList();
        }
        return muteRepository.getAllActiveMutes(players.stream().map(p -> p.getUniqueId().toString()).collect(Collectors.toList()));
    }

    public void extendMute(CommandSender sender, SppPlayer player, long duration) {
        Mute mute = getMuteByMutedUuid(player.getId()).orElseThrow(() -> new BusinessException("&CThis player isn't muted"));
        if (mute.getEndDate() == null) {
            throw new BusinessException("The player is permanently muted. Cannot extend mute");
        }

        long newDuration = (mute.getEndTimestamp() - System.currentTimeMillis()) + duration;
        permission.validateDuration(sender, muteConfiguration.permissionTempmutePlayer + LIMIT, newDuration);
        permission.validateDuration(sender, muteConfiguration.permissionExtendMutePlayer + LIMIT, duration);

        muteRepository.setMuteDuration(mute.getId(), mute.getEndTimestamp() + duration);
        Mute updatedMute = getById(mute.getId());
        sendEvent(new MuteExtensionEvent(updatedMute, duration, sender));
    }

    public void reduceMute(CommandSender sender, SppPlayer player, long duration) {
        Mute mute = getMuteByMutedUuid(player.getId()).orElseThrow(() -> new BusinessException("&CThis player isn't muted"));
        if (mute.getEndDate() == null) {
            throw new BusinessException("The player is permanently muted. Cannot reduce mute");
        }

        permission.validateDuration(sender, muteConfiguration.permissionReduceMutePlayer + LIMIT, duration);

        muteRepository.setMuteDuration(mute.getId(), mute.getEndTimestamp() - duration);
        Mute updatedMute = getById(mute.getId());
        sendEvent(new MuteReductionEvent(updatedMute, duration, sender));
    }

    public void unmute(CommandSender issuer, SppPlayer playerToUnmute, String reason) {
        Mute mute = muteRepository.findActiveMute(playerToUnmute.getId())
            .orElseThrow(() -> new BusinessException("&CCannot unmute, this user is not muted"));

        mute.setUnmutedByName(issuer instanceof Player ? issuer.getName() : "Console");
        mute.setUnmutedByUuid(issuer instanceof Player ? ((Player) issuer).getUniqueId() : CONSOLE_UUID);
        mute.setUnmuteReason(reason);
        unmute(mute);
    }

    public void unmute(SppPlayer issuer, int muteId, String reason) {
        Mute mute = muteRepository.findActiveMute(muteId)
            .orElseThrow(() -> new BusinessException("&CCannot unmute, this user is not muted"));

        mute.setUnmutedByName(issuer.getUsername());
        mute.setUnmutedByUuid(issuer.getId());
        mute.setUnmuteReason(reason);
        unmute(mute);
    }

    public List<Mute> getAppealedMutes(int offset, int amount) {
        return muteRepository.getAppealedMutes(offset, amount);
    }

    private void mute(CommandSender issuer, SppPlayer playerToMute, String reason, Long durationInMillis, boolean softMute) {
        if (playerToMute.isOnline() && permission.has(playerToMute.getPlayer(), muteConfiguration.permissionMuteByPass)) {
            throw new BusinessException("&CThis player bypasses being muted");
        }

        muteRepository.findActiveMute(playerToMute.getId())
            .ifPresent((e) -> {
                throw new BusinessException("&CCannot mute this player, the player is already muted");
            });

        String issuerName = issuer instanceof Player ? issuer.getName() : "Console";
        UUID issuerUuid = issuer instanceof Player ? ((Player) issuer).getUniqueId() : CONSOLE_UUID;

        Long endDate = durationInMillis == null ? null : System.currentTimeMillis() + durationInMillis;
        Mute mute = new Mute(reason, endDate, issuerName, issuerUuid, playerToMute.getUsername(), playerToMute.getId(), softMute);
        mute.setId(muteRepository.addMute(mute));
        sendEvent(new MuteEvent(mute));
    }

    private void unmute(Mute mute) {
        muteRepository.update(mute);
        sendEvent(new UnmuteEvent(mute));
    }

    @Override
    public List<? extends Infraction> getInfractions(Player executor, UUID playerUUID) {
        if (!options.infractionsConfiguration.isShowMutes()) {
            return Collections.emptyList();
        }
        return muteRepository.getMutesForPlayer(playerUUID);
    }


    @Override
    public Optional<InfractionInfo> getInfractionsInfo() {
        if (!options.infractionsConfiguration.isShowMutes()) {
            return Optional.empty();
        }
        Map<UUID, List<String>> muteDurationByPlayer = muteRepository.getMuteDurationByPlayer().entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> Arrays.asList("&bTotal time muted: ", "&6" + JavaUtils.toHumanReadableDuration(e.getValue()))));


        for (UUID permMutedPlayer : muteRepository.getAllPermanentMutedPlayers()) {
            muteDurationByPlayer.put(permMutedPlayer, Collections.singletonList("&CPermanently muted"));
        }

        return Optional.of(new InfractionInfo(InfractionType.MUTE, muteRepository.getCountByPlayer(), muteDurationByPlayer));
    }

    @Override
    public InfractionType getType() {
        return InfractionType.MUTE;
    }

    @Override
    public long getTotalMuteCount() {
        return muteRepository.getTotalCount();
    }

    @Override
    public long getActiveMuteCount() {
        return muteRepository.getActiveCount();
    }

    private void checkDurationPermission(CommandSender player, long durationProvided) {
        permission.validateDuration(player, muteConfiguration.permissionTempmutePlayer + LIMIT, durationProvided);
    }

    public Optional<Mute> getLastMute(UUID playerUuid) {
        return muteRepository.getLastMute(playerUuid);
    }
}
