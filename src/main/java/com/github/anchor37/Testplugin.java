package com.github.anchor37;

import org.bukkit.plugin.java.JavaPlugin;

public class Testplugin extends JavaPlugin {
	@Override
	public void onEnable() {
		getLogger().info("onEnableメソッドが呼び出されたよ！！");
	}

	@Override
	public void onDisable() {
		getLogger().info("onDisableメソッドが呼び出されたよ！！");
	}
}
