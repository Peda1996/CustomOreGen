package xyz.spaceio.customoregen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import jdk.nashorn.internal.ir.Block;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.reflect.TypeToken;

import de.Linus122.SpaceIOMetrics.Metrics;
import sun.security.acl.WorldGroupImpl;
import xyz.spaceio.config.ConfigHandler;
import xyz.spaceio.config.JSONConfig;
import xyz.spaceio.hooks.HookASkyBlock;
import xyz.spaceio.hooks.HookAcidIsland;
import xyz.spaceio.hooks.HookBentoBox;
import xyz.spaceio.hooks.HookSkyblockEarth;
import xyz.spaceio.hooks.SkyblockAPIHook;
import xyz.spaceio.hooks.HookuSkyBlock;

public class CustomOreGen extends JavaPlugin {

	/*
	 * Configurations for all generators (defined in the config.yml)
	 */
	private List<GeneratorConfig> generatorConfigs = new ArrayList<GeneratorConfig>();

	/*
	 * Disabled world blacklist
	 */
	private List<String> disabledWorlds = new ArrayList<String>();

	public List<GeneratorConfig> getGeneratorConfigs() {
		return generatorConfigs;
	}

	public void setGeneratorConfigs(List<GeneratorConfig> generatorConfigs) {
		this.generatorConfigs = generatorConfigs;
	}

	/*
	 * Our logger
	 */
	private ConsoleCommandSender clogger;

	/*
	 * Cache for GeneratorConfig ID's for each player
	 */
	private HashMap<UUIDWorld, Integer> cachedOregenConfigs = new HashMap<>();
	private JSONConfig cachedOregenJsonConfig;

	/*
	 * API Hook for the corresponding SkyBlock plugin
	 */
	private SkyblockAPIHook skyblockAPI;

	/*
	 * Object that handles the loading process of the config.yml file
	 */
	private ConfigHandler configHandler = new ConfigHandler(this, "plugins/CustomOreGen/config.yml");;

	/*
	 * Prefix for the clogger
	 */
	private final String PREFIX = "§6[CustomOreGen] ";

	private WorldGuardPlatform worldGuardPlatform;

	@Override
	public void onEnable() {
		clogger = getServer().getConsoleSender();
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new Events(this), this);

		this.loadHook();

		Bukkit.getPluginCommand("customoregen").setExecutor(new Cmd(this));

		try {
			configHandler.loadConfig();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		cachedOregenJsonConfig = new JSONConfig(cachedOregenConfigs, new TypeToken<HashMap<UUID, Integer>>() {
		}.getType(), this);

		cachedOregenConfigs = (HashMap<UUIDWorld, Integer>) cachedOregenJsonConfig.get();

		if (cachedOregenConfigs == null) {
			cachedOregenConfigs = new HashMap<UUIDWorld, Integer>();
		}
		disabledWorlds = getConfig().getStringList("disabled-worlds");

		worldGuardPlatform = getWorldGuard();

		new Metrics(this);
	}


	private WorldGuardPlatform getWorldGuard() {
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");


		// WorldGuard may not be loaded
		if (plugin == null) {
			return null;
		}

		return WorldGuard.getInstance().getPlatform();
	}

	/**
	 * creates a new api hook instance for the used skyblock plugin
	 */
	private void loadHook() {
		if (Bukkit.getServer().getPluginManager().isPluginEnabled("ASkyBlock")) {
			skyblockAPI = new HookASkyBlock();
			sendConsole("&aUsing ASkyBlock as SkyBlock-Plugin");
		} else if (Bukkit.getServer().getPluginManager().isPluginEnabled("AcidIsland")) {
			skyblockAPI = new HookAcidIsland();
			sendConsole("&aUsing AcidIsland as SkyBlock-Plugin");
		} else if (Bukkit.getServer().getPluginManager().isPluginEnabled("uSkyBlock")) {
			skyblockAPI = new HookuSkyBlock();
			sendConsole("&aUsing uSkyBlock as SkyBlock-Plugin");
		} else if (Bukkit.getServer().getPluginManager().isPluginEnabled("BentoBox")) {
			skyblockAPI = new HookBentoBox();
			sendConsole("&aUsing BentoBox as SkyBlock-Plugin");
		} else if (Bukkit.getServer().getPluginManager().isPluginEnabled("SkyBlock")) {
			skyblockAPI = new HookSkyblockEarth();
			sendConsole("&aUsing SkyblockEarth as SkyBlock-Plugin");
		} else if (Bukkit.getServer().getPluginManager().isPluginEnabled("PlotSquared")) {
			//skyblockAPI = new HookPlotSquared();
			//sendConsole("&aUsing PlotSquared as SkyBlock-Plugin");
		}
	}

	@Override
	public void onDisable() {
		cachedOregenJsonConfig.saveToDisk(cachedOregenConfigs);
	}

	public List<World> getActiveWorlds() {
		return Arrays.stream(skyblockAPI.getSkyBlockWorldNames()).map(v -> Bukkit.getWorld(v))
				.collect(Collectors.toList());
	}

	public int getLevel(UUID uuid, String world) {
		return skyblockAPI.getIslandLevel(uuid, world);
	}

	public OfflinePlayer getOwner(Location loc) {
		if (skyblockAPI.getIslandOwner(loc) == null) {
			return null;
		}
		Optional<UUID> uuid = skyblockAPI.getIslandOwner(loc);
		if (!uuid.isPresent()) {
			return null;
		}
		OfflinePlayer p = Bukkit.getOfflinePlayer(uuid.get());

		return p;
	}

	public void reload() throws IOException {
		reloadConfig();
		configHandler.loadConfig();
	}

	public GeneratorConfig getWorldguardConfig(Location l){
		if(l== null || worldGuardPlatform == null) return null;

		//com.sk89q.worldedit.bukkit.
		Set<ProtectedRegion> regions = worldGuardPlatform.getRegionContainer().get(BukkitAdapter.adapt(l.getWorld())).getApplicableRegions(BlockVector3.at(l.getX(),l.getY(),l.getZ())).getRegions();
		if(regions.size() == 0)
			return null;

		GeneratorConfig gc = null;

		for(GeneratorConfig g : generatorConfigs){
			System.out.println("Worldguardconfig works?!!");
			if(g.enabledWorlds.contains(l.getWorld().getName())){
				for(String rg : g.worldguardRegions){
					for(ProtectedRegion r : regions){
						String regionId = r.getId();
						if(regionId == rg){
							gc = g;
						}
					}
				}

			}
		}

		return gc;

	}


	public GeneratorConfig getGeneratorConfigForPlayer(OfflinePlayer p, String world) {
		GeneratorConfig gc = null;
		//int id = 0;
		Map<String, Integer> worldId = new HashMap<>();

		if (p == null) {
			for(GeneratorConfig g : generatorConfigs){
				if(g.enabledWorlds.contains(world)){
					gc = g;
				}
			}
			//TODO THIS will cause Nullpointers..
			//cacheOreGen(p.getUniqueId(), id, world);
		} else {
			//get the level
			int islandLevel = getLevel(p.getUniqueId(), world);

			//if (p.isOnline()) {
				Player realP = p.getPlayer();
					int count = 0;
					for (GeneratorConfig gc2 : generatorConfigs) {
						if (gc2 == null) {
							continue;
						}
						count++;


						if ((p.isOnline() && (realP.hasPermission(gc2.permission)) || gc2.permission.length() == 0)
								&& islandLevel >= gc2.unlock_islandLevel) {
							// continue
							if ((gc2.enabledWorlds.contains(world) || gc2.enabledWorlds.isEmpty()) &&
									this.getActiveWorlds().contains(Bukkit.getWorld(world))) {
								gc = gc2;
							}

							//this is only needed for checking the owners permissions
							//TODO use perms of members
							for(String w: gc2.enabledWorlds){
								worldId.put(w,count);
							}

						}else{
							GeneratorConfig tmp = getCachedGeneratorConfig(p.getUniqueId(), world);
							if(tmp != null){
								gc = tmp;
							}
						}

					}
		}

		for (Map.Entry<String, Integer> pair : worldId.entrySet()) {
			cacheOreGen(p.getUniqueId(), pair.getKey(), pair.getValue()-1);
		}

		//if (id > 0) {
		//	cacheOreGen(p.getUniqueId(), world, id - 1);
		//}
		return gc;
	}

	public List<String> getDisabledWorlds() {
		return disabledWorlds;
	}

	public void setDisabledWorlds(List<String> disabledWorlds) {
		this.disabledWorlds = disabledWorlds;
	}

	public GeneratorConfig getCachedGeneratorConfig(UUID uuid, String world) {
		UUIDWorld worlduuid = new UUIDWorld(uuid, world);
		if (cachedOregenConfigs.containsKey(worlduuid)) {
			int id = cachedOregenConfigs.get(worlduuid);
			//System.out.println("GET: "+id + " WORLD: " + world);
			return generatorConfigs.get(id);
		}
		return null;
	}

	public void cacheOreGen(UUID uuid, String world, int configID) {
		//System.out.println("SET: " + configID + " " + world );
		cachedOregenConfigs.put(new UUIDWorld(uuid,world), configID);
	}

	public void sendConsole(String msg) {
		clogger.sendMessage(PREFIX + msg.replace("&", "§"));
	}

	private class UUIDWorld{

		UUIDWorld(UUID uuid, String world){
			this.uuid = uuid;
			this.world = world;
		}
		UUID uuid;
		String world;

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			UUIDWorld uuidWorld = (UUIDWorld) o;

			if (uuid != null ? !uuid.equals(uuidWorld.uuid) : uuidWorld.uuid != null) return false;
			return world != null ? world.equals(uuidWorld.world) : uuidWorld.world == null;
		}

		@Override
		public int hashCode() {
			int result = uuid != null ? uuid.hashCode() : 0;
			result = 31 * result + (world != null ? world.hashCode() : 0);
			return result;
		}
	}
}
