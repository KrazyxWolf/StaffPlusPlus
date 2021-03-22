package net.shortninja.staffplus.core.domain.staff.staffchat;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.shortninja.staffplus.core.StaffPlus;
import net.shortninja.staffplus.core.common.Constants;
import be.garagepoort.mcioc.IocBean;
import net.shortninja.staffplus.core.common.config.Messages;
import net.shortninja.staffplus.core.common.config.Options;
import net.shortninja.staffplus.core.common.utils.MessageCoordinator;
import net.shortninja.staffplusplus.staffmode.chat.StaffChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import static net.shortninja.staffplus.core.common.utils.BukkitUtils.sendEvent;

@IocBean
public class StaffChatServiceImpl implements net.shortninja.staffplusplus.staffmode.chat.StaffChatService {

    private Messages messages;
    private Options options;
    private final StaffChatMessageFormatter staffChatMessageFormatter;
    private final MessageCoordinator message;

    public StaffChatServiceImpl(Messages messages, Options options, StaffChatMessageFormatter staffChatMessageFormatter, MessageCoordinator message) {
        this.messages = messages;
        this.options = options;
        this.staffChatMessageFormatter = staffChatMessageFormatter;
        this.message = message;
    }

    void handleBungeeMessage(String message) {
        sendMessageToStaff(message);
    }

    public void sendMessage(CommandSender sender, String message) {
        String formattedMessage = staffChatMessageFormatter.formatMessage(sender, message);

        sendBungeeMessage(sender, formattedMessage);
        sendMessageToStaff(formattedMessage);

        if (sender instanceof Player) {
            sendEvent(new StaffChatEvent((Player) sender, options.serverName, message));
        }
    }

    public boolean hasHandle(String message) {
        return message.startsWith(options.staffChatConfiguration.getHandle()) && !options.staffChatConfiguration.getHandle().isEmpty();
    }

    @Override
    public void sendMessage(String senderName, String message) {
        String formattedMessage = staffChatMessageFormatter.formatMessage(senderName, message);
        sendMessageToStaff(formattedMessage);
    }

    @Override
    public void sendMessage(String message) {
        if (!messages.prefixStaffChat.isEmpty()) {
            message = messages.prefixStaffChat + " " + message;
        }

        sendMessageToStaff(message);
    }

    private void sendMessageToStaff(String formattedMessage) {
        Bukkit.getOnlinePlayers().stream()
            .filter(player -> player.hasPermission(options.staffChatConfiguration.getPermissionStaffChat()))
            .forEach(player -> message.send(player, formattedMessage, messages.prefixStaffChat));
    }

    private void sendBungeeMessage(CommandSender sender, String message) {
        if (!options.staffChatConfiguration.isBungeeEnabled()) {
            // Bungee network not enabled
            return;
        }

        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
            if (onlinePlayers.iterator().hasNext()) {
                player = onlinePlayers.iterator().next();
            }
        }
        if (player != null) {
            try {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Forward");
                out.writeUTF("ALL");
                out.writeUTF("StaffPlusPlusChat");
                ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
                DataOutputStream msgout = new DataOutputStream(msgbytes);
                msgout.writeUTF(message);

                out.writeShort(msgbytes.toByteArray().length);
                out.write(msgbytes.toByteArray());

                player.sendPluginMessage(StaffPlus.get(), Constants.BUNGEE_CORD_CHANNEL, out.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
