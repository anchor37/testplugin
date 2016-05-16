package com.github.anchor37;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Testplugin extends JavaPlugin implements Listener {

    /**
     * Plugin Logger
     */
    final private Logger log = this.getLogger();
    /**
     * Plugin Configartion
     */
    private FileConfiguration conf = this.getConfig();
    private List<String> worlds;
    private List<String> diableblocks;
    private int maxdistance = 32;

    public Testplugin() {
	// コンフィグファイルがなければ生成
	// Load Configration or Create config.yml and set default.
	if (conf == null) {
	    log.info("[" + this.getName()
		    + "] Do not exist config file. The plugin will create config file 'config.yml'.");
	    this.saveDefaultConfig();
	}

	// 動作可能ワールドの読み込み
	this.worlds = conf.getStringList("worlds");
	// 置換対象外ブロックリストの読み込み
	this.diableblocks = conf.getStringList("executable-blocks");
    }

    // プラグイン起動時
    public void onEnable() {
	log.info(this.getName() + " has been enabled!");
    }

    // プラグイン停止時
    public void onDisable() {
	log.info(this.getName() + " has been disebled!");
    }

    /**
     *
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	if (args.length <= 0) {
	    sender.sendMessage("[" + this.getName() + "] version " + this.getDescription().getVersion());
	    return true;
	}

	if (sender.isOp() && args[0].equalsIgnoreCase("reload")) {
	    this.reloadConfig();
	    this.worlds = conf.getStringList("worlds");
	    this.diableblocks = conf.getStringList("executable-blocks");
	    sender.sendMessage("[" + this.getName() + "] config has been reloaded.");
	    return true;
	}

	// コマンド使ったヒト
	// Command user
	Player player = getServer().getPlayer(sender.getName());
	// ワールド
	// world
	World world = player.getWorld();
	if (!worlds.contains(world)) {
	    sender.sendMessage("Don't use this command, in this map.");
	    return false;
	}
	// 座標点
	// Point
	Location biginfill = new Location(world, Integer.valueOf(args[0]), Integer.valueOf(args[1]),
		Integer.valueOf(args[2]));
	Location endfill = new Location(world, Integer.valueOf(args[3]), Integer.valueOf(args[4]),
		Integer.valueOf(args[5]));
	// XとZの距離をチェック
	// distance of X and Z.
	if (Math.abs(biginfill.getX()) + Math.abs(endfill.getX()) > maxdistance
		|| Math.abs(biginfill.getZ()) + Math.abs(endfill.getZ()) > maxdistance) {
	    sender.sendMessage("You specified coordinate points is too far distance! Respectively in the x and z to "
		    + maxdistance + ".");
	    return false;
	}
	// チャンク生成チェック
	if (!world.isChunkLoaded(biginfill.getChunk()) || !world.isChunkLoaded(endfill.getChunk())) {
	    sender.sendMessage("Don't use specified location.");
	    return false;
	}

	// 指定ブロックが所持品に含まれているか確認
	// Check at specified block in player inventory.
	PlayerInventory inventory = player.getInventory();
	List<String> items = getPlayerInventoryUniqeList(inventory);
	if (!items.contains(args[6])) {
	    sender.sendMessage("You don't have fill specified block(s).");
	    return false;
	}

	// 置換実行 空気ブロックしか置き換えません
	// execute replace. Replace at Material.AIR only.
	for (int y = (int) biginfill.getY(); y <= (int) endfill.getY(); y++) {
	    for (int x = (int) biginfill.getX(); x <= (int) endfill.getX(); x++) {
		for (int z = (int) biginfill.getX(); z <= (int) endfill.getZ(); z++) {
		    Location location = new Location(world, x, y, z);
		    Block block = location.getBlock();
		    if (!block.getType().equals(Material.AIR)) {
			block.setType(Material.valueOf(args[6]));
		    }
		}
	    }

	}
	return true;
    }

    /**
     *
     * @param sender
     * @param command
     * @param alias
     * @param args
     * @return tab complete string(s)
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
	// コマンド使ったヒト
	Player player = getServer().getPlayer(sender.getName());
	// 見てる先
	Location lookat = player.getEyeLocation();
	switch (args.length) {
	case 0:
	    return Collections.singletonList(String.valueOf(lookat.getBlockX()));
	case 1:
	    return Collections.singletonList(String.valueOf(lookat.getBlockY()));
	case 2:
	    return Collections.singletonList(String.valueOf(lookat.getBlockZ()));
	case 3:
	    return Collections.singletonList(String.valueOf(lookat.getBlockX()));
	case 4:
	    return Collections.singletonList(String.valueOf(lookat.getBlockY()));
	case 5:
	    return Collections.singletonList(String.valueOf(lookat.getBlockZ()));
	case 6:
	    // プレイヤーのインベントリ取得
	    // Get player inventory
	    PlayerInventory inventory = player.getInventory();
	    return getPlayerInventoryUniqeList(inventory);
	default:
	    break;
	}

	// JavaPlugin#onTabComplete()を呼び出す
	return super.onTabComplete(sender, command, alias, args);
    }

    /**
     * @return Excutable world list
     */
    public List<String> getProtectedWorlds() {
	return this.worlds;
    }

    /**
     * @return Deny replaceable blocks
     */
    public List<String> getDisableBlocks() {
	return this.diableblocks;
    }

    /**
     *
     * @param inventory
     * @return Uniqe list at plyer inventory
     */
    private List<String> getPlayerInventoryUniqeList(PlayerInventory inventory) {
	// 持ち物のリスト
	List<String> items = new ArrayList<String>();
	for (ItemStack itemStack : inventory.getContents()) {
	    Material material = itemStack.getType();
	    if (!items.contains(material) && !getDisableBlocks().contains(material)) {
		items.add(String.valueOf(material));
	    }
	}
	return items;
    }
}
