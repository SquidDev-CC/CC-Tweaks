package org.squiddev.cctweaks.core;

import net.minecraftforge.common.config.Configuration;
import org.squiddev.cctweaks.core.lua.socket.AddressMatcher;
import org.squiddev.configgen.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The main config class
 */
@org.squiddev.configgen.Config(languagePrefix = "gui.config.cctweaks.")
public final class Config {
	public static Configuration configuration;
	public static Set<String> turtleDisabledActions;
	public static Set<String> globalWhitelist;

	public static AddressMatcher socketWhitelist;
	public static AddressMatcher socketBlacklist;

	public static void init(File file) {
		ConfigLoader.init(file);
	}

	public static void sync() {
		ConfigLoader.sync();
	}

	@OnSync
	public static void onSync() {
		configuration = ConfigLoader.getConfiguration();

		// Handle generation of HashSets, etc...
		Set<String> disabledActions = turtleDisabledActions = new HashSet<String>();
		for (String action : Turtle.disabledActions) {
			disabledActions.add(action.toLowerCase());
		}

		globalWhitelist = new HashSet<String>(Arrays.asList(Computer.globalWhitelist));

		Computer.computerUpgradeCrafting &= Computer.computerUpgradeEnabled;

		Network.WirelessBridge.crafting &= Network.WirelessBridge.enabled;
		Network.WirelessBridge.turtleEnabled &= Network.WirelessBridge.enabled;

		socketWhitelist = new AddressMatcher(APIs.Socket.whitelist);
		socketBlacklist = new AddressMatcher(APIs.Socket.blacklist);
	}

	/**
	 * Computer tweaks and items.
	 */
	public static final class Computer {
		/**
		 * Enable upgrading computers.
		 */
		@DefaultBoolean(true)
		public static boolean computerUpgradeEnabled;

		/**
		 * Enable crafting the computer upgrade.
		 * Requires computerUpgradeEnabled.
		 */
		@DefaultBoolean(true)
		@RequiresRestart
		public static boolean computerUpgradeCrafting;

		/**
		 * Enable using the debug wand.
		 */
		@DefaultBoolean(true)
		public static boolean debugWandEnabled;

		/**
		 * Globals to whitelist (are not set to nil).
		 * This is NOT recommended for servers, use at your own risk.
		 */
		@RequiresRestart(mc = false, world = true)
		public static String[] globalWhitelist;

		/**
		 * Time in milliseconds before 'Too long without yielding' errors.
		 * You cannot shutdown/reboot the computer during this time.
		 * Use carefully.
		 */
		@DefaultInt(7000)
		@Range(min = 0)
		public static int computerThreadTimeout;

		/**
		 * Compile Lua bytecode to Java bytecode
		 */
		@DefaultBoolean(false)
		@RequiresRestart(mc = false, world = true)
		public static boolean luaJC;

		/**
		 * Verify LuaJC sources on generation.
		 * This will slow down compilation.
		 * If you have errors, please turn this and debug on and
		 * send it with the bug report.
		 */
		@DefaultBoolean(false)
		public static boolean luaJCVerify;
	}

	/**
	 * Turtle tweaks and items.
	 */
	public static final class Turtle {
		/**
		 * Amount of RF required for one refuel point
		 * Set to 0 to disable.
		 */
		@DefaultInt(100)
		@Range(min = 0)
		public static int fluxRefuelAmount;

		/**
		 * Amount of Eu required for one refuel point.
		 * Set to 0 to disable.
		 */
		@DefaultInt(25)
		@Range(min = 0)
		public static int euRefuelAmount;

		/**
		 * Fun actions for turtle names
		 */
		@DefaultBoolean(true)
		public static boolean funNames;

		/**
		 * Disabled turtle actions:
		 * (compare, compareTo, craft, detect, dig,
		 * drop, equip, inspect, move, place,
		 * refuel, select, suck, tool, turn).
		 */
		public static String[] disabledActions;

		/**
		 * Various tool host options
		 */
		public static class ToolHost {
			/**
			 * Enable the Tool Host
			 */
			@DefaultBoolean(true)
			public static boolean enabled;

			/**
			 * Enable crafting the Tool Host
			 */
			@DefaultBoolean(true)
			@RequiresRestart
			public static boolean crafting;

			/**
			 * Upgrade Id
			 */
			@DefaultInt(332)
			@RequiresRestart
			@Range(min = 0)
			public static int upgradeId;

			/**
			 * The dig speed factor for tool hosts.
			 * 20 is about normal player speed.
			 */
			@DefaultInt(10)
			@Range(min = 1)
			public static int digFactor;
		}
	}

	/**
	 * Additional network functionality.
	 */
	public static final class Network {
		/**
		 * The wireless bridge allows you to connect
		 * wired networks across dimensions.
		 */
		public static class WirelessBridge {
			/**
			 * Enable the wireless bridge
			 */
			@DefaultBoolean(true)
			@RequiresRestart(mc = false, world = true)
			public static boolean enabled;

			/**
			 * Enable the crafting of Wireless Bridges.
			 */
			@DefaultBoolean(true)
			@RequiresRestart
			public static boolean crafting;

			/**
			 * Enable the Wireless Bridge upgrade for turtles.
			 */
			@DefaultBoolean(true)
			@RequiresRestart
			public static boolean turtleEnabled;

			/**
			 * The turtle upgrade Id
			 */
			@DefaultInt(331)
			@Range(min = 1)
			@RequiresRestart
			public static int turtleId;

			/**
			 * Enable the Wireless Bridge upgrade for pocket computers.
			 * Requires Peripherals++
			 */
			@DefaultBoolean(true)
			@RequiresRestart
			public static boolean pocketEnabled;

			/**
			 * The pocket upgrade Id
			 * Requires Peripherals++
			 */
			@DefaultInt(331)
			@Range(min = 1)
			@RequiresRestart
			public static int pocketId;
		}

		/**
		 * Enable the crafting of full block modems.
		 *
		 * If you disable, existing ones will still function,
		 * and you can obtain them from creative.
		 */
		@DefaultBoolean(true)
		@RequiresRestart
		public static boolean fullBlockModemCrafting;
	}

	/**
	 * Integration with other mods.
	 */
	@RequiresRestart
	public static final class Integration {
		/**
		 * Allows pushing items from one inventory
		 * to another inventory on the network.
		 */
		@DefaultBoolean(true)
		public static boolean openPeripheralInventories;

		/**
		 * Enable ChickenBones Multipart
		 * (aka ForgeMultipart) integration.
		 */
		@DefaultBoolean(true)
		public static boolean cbMultipart;
	}

	/**
	 * Various tweaks that don't belong to anything
	 */
	public static final class Misc {
		/**
		 * The light level given off by normal monitors.
		 * Redstone torches are 7, normal torches are 14.
		 */
		@DefaultInt(7)
		@Range(min = 0, max = 15)
		public static int monitorLight;

		/**
		 * The light level given off by advanced monitors.
		 * Redstone torches are 7, normal torches are 14.
		 */
		@DefaultInt(10)
		@Range(min = 0, max = 15)
		public static int advancedMonitorLight;
	}

	/**
	 * Custom APIs for computers
	 */
	public static final class APIs {
		/**
		 * TCP connections from the socket API
		 */
		public static final class Socket {
			/**
			 * Enable TCP connections.
			 * When enabled, the socket API becomes available on
			 * all computers.
			 */
			@DefaultBoolean(true)
			@RequiresRestart(mc = false, world = true)
			public static boolean enabled;

			/**
			 * Blacklisted domain names.
			 *
			 * Entries are either domain names (www.example.com) or IP addresses in
			 * string format (10.0.0.3), optionally in CIDR notation to make it easier
			 * to define address ranges (1.0.0.0/8). Domains are resolved to their
			 * actual IP once on startup, future requests are resolved and compared
			 * to the resolved addresses.
			 */
			@DefaultString({"127.0.0.0/8", "10.0.0.0/8", "192.168.0.0/16", "172.16.0.0/12"})
			public static String[] blacklist;

			/**
			 * Whitelisted domain names.
			 * If something is mentioned in both the blacklist and whitelist then
			 * the blacklist takes priority.
			 */
			public static String[] whitelist;

			/**
			 * Maximum TCP connections a computer can have at any time
			 */
			@DefaultInt(4)
			@Range(min = 1)
			public static int maxTcpConnections;

			/**
			 * Number of threads to use for processing name lookups.
			 */
			@DefaultInt(4)
			@Range(min = 1)
			@RequiresRestart
			public static int threads;

			/**
			 * Maximum number of characters to read from a socket.
			 */
			@DefaultInt(2048)
			@Range(min = 1)
			public static int maxRead;
		}

		/**
		 * Basic data manipulation
		 */
		public static final class Data {
			/**
			 * If the data API is enabled
			 */
			@DefaultBoolean(true)
			@RequiresRestart(mc = false, world = true)
			public static boolean enabled;

			/**
			 * Maximum number of bytes to process.
			 * The default is 1MiB
			 */
			@DefaultInt(1048576)
			public static int limit;
		}
	}

	/**
	 * Only used when testing and developing the mod.
	 * Nothing to see here, move along...
	 */
	public static final class Testing {
		/**
		 * Show debug messages.
		 * If you hit a bug, enable this, rerun and send the log
		 */
		@DefaultBoolean(false)
		public static boolean debug;

		/**
		 * Enable debug blocks/items.
		 * Only use for testing.
		 */
		@DefaultBoolean(false)
		public static boolean debugItems;

		/**
		 * Throw exceptions on calling deprecated methods
		 *
		 * Only for development/testing
		 */
		@DefaultBoolean(false)
		public static boolean deprecatedWarnings;

		/**
		 * Controller validation occurs by default as a
		 * way of ensuring that your network has been
		 * correctly created.
		 *
		 * By enabling this it is easier to trace
		 * faults, though it may slow things down
		 * slightly
		 */
		@DefaultBoolean(false)
		public static boolean extendedControllerValidation;
	}
}
