package com.github.anchor37;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Testplugin extends JavaPlugin {

    /**
     * Plugin Logger
     */
    final private Logger log = this.getLogger();
    /**
     * Plugin Configartion
     */
    private final String configFileName = "config.yml";
    private final File configFile = new File(this.getDataFolder(), this.configFileName);
    private final FileConfiguration conf = this.getConfig();
    private List<String> worlds;
    private List<String> disableblocks;
    private List<String> replaceableblocks;
    private int maxdistance = 16;

    // プラグイン起動時
    public void onEnable() {
        log.info("config file:" + configFile.toString());
        if (configFile.exists() == false) {
            log.info("[" + this.getName() + "] Do not exist config file. The plugin will create config file 'config.yml'.");
            this.saveDefaultConfig();
            try {
                conf.load(configFile);
            } catch (IOException | InvalidConfigurationException e) {
                // TODO 自動生成された catch ブロック
                e.printStackTrace();
            }
        }
        loadConfig();
        this.getCommand("xfill").setExecutor(this);
        log.info(this.getName() + " has been enabled!");
    }

    // プラグイン停止時
    public void onDisable() {
        log.info(this.getName() + " has been disebled!");
    }

    /**
     * @param sender
     * @param command
     * @param label
     * @param args
     * @return
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {
        if (!(sender instanceof Player)) {
            log.info("xfill command can use login player only.");
            return false;
        }
        Player player = getServer().getPlayer(sender.getName());
        World world = player.getWorld();
        if (!worlds.contains(world.getName())) {
            sender.sendMessage("Don't use this command, in this map.");
            return false;
        }

        if (command.getName().equalsIgnoreCase("xfill")) {
            // sender.sendMessage("[debug] run command.");
            switch (args.length) {
            case 7:
                // Point
                int minx;
                int maxx;
                int miny;
                int maxy;
                int minz;
                int maxz;
                Location biginfill = new Location(world, Integer.valueOf(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2]));
                Location endfill = new Location(world, Integer.valueOf(args[3]), Integer.valueOf(args[4]), Integer.valueOf(args[5]));
                if (biginfill.getBlockX() <= endfill.getBlockX()) {
                    minx = biginfill.getBlockX();
                    maxx = endfill.getBlockX();
                } else {
                    minx = endfill.getBlockX();
                    maxx = biginfill.getBlockX();
                }
                if (biginfill.getBlockY() <= endfill.getBlockY()) {
                    miny = biginfill.getBlockY();
                    maxy = endfill.getBlockY();
                } else {
                    miny = endfill.getBlockY();
                    maxy = biginfill.getBlockY();
                }
                if (biginfill.getBlockZ() <= endfill.getBlockZ()) {
                    minz = biginfill.getBlockZ();
                    maxz = endfill.getBlockZ();
                } else {
                    minz = endfill.getBlockZ();
                    maxz = biginfill.getBlockZ();
                }
                // Check at distance of X,Y,Z.
                if ((Math.abs(maxx - minx) > this.maxdistance) || (Math.abs(maxy - miny) > this.maxdistance) || (Math.abs(maxz - minz) > this.maxdistance)) {
                    sender.sendMessage("You specified coordinate points is too far distance! Respectively in the x and z to " + this.maxdistance + ".");
                    // sender.sendMessage("X distance:" + Math.abs(biginfill.getBlockX() - endfill.getBlockX()));
                    // sender.sendMessage("Z distance:" + Math.abs(biginfill.getBlockZ() - endfill.getBlockZ()));
                    return false;
                }
                // Chek at chank ganarated.
                if (!world.isChunkLoaded(biginfill.getChunk()) || !world.isChunkLoaded(endfill.getChunk())) {
                    sender.sendMessage("Don't use specified location.");
                    return false;
                }
                // Check at specified block in player inventory.
                PlayerInventory inventory = player.getInventory();
                List<String> items = getPlayerInventoryUniqeList(inventory);
                if (!items.contains(args[6])) {
                    sender.sendMessage("You don't have fill specified block(s).");
                    return false;
                }
                // log.info("execute replace:" + args[6]);
                invebtoryloop: {
                    for (ItemStack itemStack : inventory.getStorageContents()) {
                        if (itemStack == null) {
                            continue;
                        }
                        // log.info(itemStack.getTypeId() + ":" + itemStack.getDurability() + " " +
                        // itemStack.getAmount());
                        if ((itemStack.getTypeId() + ":" + itemStack.getDurability()).equalsIgnoreCase(args[6])) {
                            execute: {
                                // log.info(minx + ":" + miny + ":" + minz + " " + maxx + ":" + maxy + ":" + maxz);
                                for (int y = miny; y <= maxy; y++) {
                                    for (int x = minx; x <= maxx; x++) {
                                        for (int z = minz; z <= maxz; z++) {
                                            Location location = new Location(world, x, y, z);
                                            Block block = location.getBlock();
                                            // 置換許可ブロックのみ置き換え
                                            // Replace at allowed type only.
                                            // log.info("exec replace: " + location.toString());
                                            if (this.getReplaceableBlocks().contains(block.getTypeId() + ":" + block.getState().getRawData())) {
                                                // 置換を実行
                                                // Excute replace.
                                                block.setTypeIdAndData(itemStack.getTypeId(), itemStack.getData().getData(), true);
                                                // 個数0になったらスタックを消します
                                                // Remove zero amount itemstack.
                                                if (itemStack.getAmount() == 1) {
                                                    inventory.remove(itemStack);
                                                    break execute;
                                                } else {
                                                    itemStack.setAmount(itemStack.getAmount() - 1);
                                                }
                                            }
                                            // 指定範囲の最後のブロックの処理の場合全体のループから抜ける
                                            // Force exit, currnt location is end of area.
                                            if (y >= maxy && x >= maxx && z >= maxz) {
                                                break invebtoryloop;
                                            }
                                        }

                                    }
                                }
                            }
                        }

                    }
                }
                return true;
            case 1:
                if (sender.isOp() && args[0].equalsIgnoreCase("reload")) {
                    this.reloadConfig();
                    this.loadConfig();
                    return true;
                }

                if (args[0].equalsIgnoreCase("iteminfo")) {
                    ItemStack stack = player.getInventory().getItemInMainHand();

                    if (stack == null || stack.getType() == Material.AIR) {
                        sender.sendMessage("Nothing in your main hand.");
                    } else {
                        sender.sendMessage("MainHand [ID NAME(meta)]: " + stack.getTypeId() + " " + stack.getData().toString());
                        sender.sendMessage(stack.getTypeId() + ":" + stack.getDurability());
                    }
                    return true;
                }

                if (args[0].equalsIgnoreCase("lookinfo")) {
                    Block block = player.getTargetBlock((Set<Material>) null, 100);
                    if (block == null || block.getType() == Material.AIR) {
                        // log.info(block.getType().name());
                        sender.sendMessage("Nothing to target.");
                    } else {
                        sender.sendMessage("LookAt [ID(Meta)]: " + block.getType().getId() + ":" + block.getState().getRawData());
                    }
                    return true;
                }
            default:
                sender.sendMessage("[" + this.getName() + "] version " + this.getDescription().getVersion());
                return false;
            }
        }
        return false;
    }

    /**
     * @param sender
     * @param command
     * @param alias
     * @param args
     * @return tab complete string(s)
     */
    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {
        if (!(sender instanceof Player)) {
            return null;
        }
        Player player = getServer().getPlayer(sender.getName());
        // looking at
        Block block = player.getTargetBlock((Set<Material>) null, 100);
        Location lookat = block.getLocation();
        List<String> completionList = null;
        switch (args.length) {
        case 1:
            completionList = new ArrayList<String>();
            completionList.add(String.valueOf(lookat.getBlockX()));
            completionList.add("iteminfo");
            completionList.add("lookinfo");
            if (player.isOp()) {
                completionList.add("reload");
            }
            /*
             * if (args[0].length() > 0) { for (String comp : completionList) { if (comp.indexOf(args[0]) == -1) {
             * completionList.remove(comp); } } }
             */
            return completionList;
        case 2:
            return Collections.singletonList(String.valueOf(lookat.getBlockY()));
        case 3:
            return Collections.singletonList(String.valueOf(lookat.getBlockZ()));
        case 4:
            return Collections.singletonList(String.valueOf(lookat.getBlockX()));
        case 5:
            return Collections.singletonList(String.valueOf(lookat.getBlockY()));
        case 6:
            return Collections.singletonList(String.valueOf(lookat.getBlockZ()));
        case 7:
            PlayerInventory inventory = player.getInventory();
            return getPlayerInventoryUniqeList(inventory);
        default:
            break;
        }

        return super.onTabComplete(sender, command, alias, args);
    }

    /**
     * @return Excutable world list.
     */
    public List<String> getWorlds() {
        return this.worlds;
    }

    /**
     * @return Deny replaceable blocks. (in player inventory)
     */
    public List<String> getDisableBlocks() {
        return this.disableblocks;
    }

    /**
     * @return Allow replaceable blocks. (in world blocks) default Material.AIR only
     */
    public List<String> getReplaceableBlocks() {
        return this.replaceableblocks;
    }

    /**
     * @param inventory
     * @return Uniqe list at plyer inventory
     */
    private List<String> getPlayerInventoryUniqeList(
            PlayerInventory inventory) {
        // 持ち物のリスト
        List<String> items = new ArrayList<String>();
        try {
            for (ItemStack itemStack : inventory.getStorageContents()) {
                String itemId = String.valueOf(itemStack.getTypeId()) + ":" + String.valueOf(itemStack.getDurability());
                if (itemStack.getType().isBlock() && !items.contains(itemId) && !getDisableBlocks().contains(itemId)) {
                    // items.add(itemStack.getData().toString());
                    items.add(itemId);
                }
            }
        } catch (NullPointerException e) {
            // No item in inventory.
        }
        return items;
    }

    private void loadConfig() {
        this.worlds = conf.getStringList("worlds");
        this.disableblocks = conf.getStringList("disableblocks");
        // 置換許可ブロックリスト
        this.replaceableblocks = conf.getStringList("replaceableblocks");
        this.replaceableblocks.add(0, "0:0");
        this.maxdistance = conf.getInt("maxdistance");

        log.info("[" + this.getName() + "] config has been loaded.");
        log.info("worlds:" + this.getWorlds().toString());
        log.info("disablebloacks:" + this.getDisableBlocks().toString());
        log.info("replaceableblocks:" + this.getReplaceableBlocks().toString());
        log.info("maxdistance:" + this.maxdistance);
    }
}
