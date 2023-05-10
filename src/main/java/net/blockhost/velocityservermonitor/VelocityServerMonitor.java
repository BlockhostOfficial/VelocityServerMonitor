package net.blockhost.velocityservermonitor;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import ninja.leaping.configurate.transformation.NodePath;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "velocityservermonitor",
        name = "VelocityServerMonitor",
        version = "1.0",
        description = "Server monitor for CrashTheServer",
        url = "https://blockhost.net",
        authors = {"qbasty"}
)
public class VelocityServerMonitor {

    @Inject
    private Logger logger;

    @Inject
    private ProxyServer proxyServer;

    @Inject
    private CommandManager commandManager;
    private Path serversFilePath;

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        serversFilePath = Paths.get("/root/lobby/plugins/CrashTheServer/servers.txt"); // specify the full path to the servers.txt file
        proxyServer.getScheduler().buildTask(this, this::checkForNewServers).repeat(5, TimeUnit.SECONDS).schedule();
    }

    private void checkForNewServers() {
        try {
            if (!Files.exists(serversFilePath)) {
                logger.warn("servers.txt file not found");
                return;
            }

            List<String> lines = Files.readAllLines(serversFilePath);
            for (String line : lines) {
                String[] parts = line.split(",");
                String serverName = parts[0];
                String privateIp = parts[1];
                int port = Integer.parseInt(parts[2]);
                if (parts.length < 3) {
                    logger.warn("Invalid server info in servers.txt: " + line);
                    continue;
                }

                // Register the server
                InetSocketAddress address = new InetSocketAddress(privateIp, port);
                ServerInfo serverInfo = new ServerInfo(serverName, address);
                proxyServer.registerServer(serverInfo);
                logger.warn("Registered a server.");

                // Execute the 'velocity reload' command
                //commandManager.executeAsync(proxyServer.getConsoleCommandSource(), "velocity reload");
            }

            // Clear the servers.txt file after processing
            Files.write(serversFilePath, new ArrayList<>(), StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            logger.error("Failed to read server info from file: " + e.getMessage());
            e.printStackTrace();
        }
    }

}