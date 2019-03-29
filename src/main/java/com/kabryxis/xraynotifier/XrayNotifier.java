package com.kabryxis.xraynotifier;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class XrayNotifier extends JavaPlugin implements Listener {
	
	private final Map<UUID, Set<Block>> trackedBlocks = new HashMap<>();
	
	private File logFile;
	private FileWriter fileWriter;
	private long duration;
	private int amount;
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		logFile = new File(getDataFolder(), "current.txt");
		if(!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		try {
			fileWriter = new FileWriter(logFile);
		} catch(IOException e) {
			e.printStackTrace();
		}
		reloadConfig();
		duration = getConfig().getLong("interval", 300);
		amount = getConfig().getInt("amount", 15);
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable() {
		try {
			fileWriter.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		if(logFile.exists()) logFile.renameTo(new File(getDataFolder(), new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".txt"));
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if(block.getType() == Material.DIAMOND_ORE) {
			Player player = event.getPlayer();
			Set<Block> playerTrackedBlocks = trackedBlocks.computeIfAbsent(player.getUniqueId(), u ->
					Collections.newSetFromMap(new SelfExpiringHashMap<>(duration * 1000)));
			playerTrackedBlocks.add(block);
			if(playerTrackedBlocks.size() >= amount) {
				try {
					Location blockLoc = block.getLocation();
					String message = player.getDisplayName() + " has mined " + playerTrackedBlocks.size() + " diamond blocks in " +
							amount + " seconds (block location: " + blockLoc.getWorld().getName() + "," + blockLoc.getBlockX() +
							"," + blockLoc.getBlockY() + "," + blockLoc.getBlockZ() + ").";
					getLogger().info(message);
					fileWriter.write(message + '\n');
					fileWriter.flush();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
