package xyz.spaceio.hooks;

		import java.lang.reflect.InvocationTargetException;
		import java.lang.reflect.Method;
		import java.util.Arrays;
		import java.util.Collection;
		import java.util.Optional;
		import java.util.UUID;
		import java.util.stream.Stream;

		import org.bukkit.Bukkit;
		import org.bukkit.Location;
		import org.bukkit.World;

		import world.bentobox.bentobox.BentoBox;
		import world.bentobox.bentobox.database.objects.Island;

public class HookBentoBox implements SkyblockAPIHook{

	private BentoBox api;

	public HookBentoBox() {
		api = (BentoBox) Bukkit.getPluginManager().getPlugin("BentoBox");
	}

	@Override
	public int getIslandLevel(UUID uuid, String onWorld) {
		int level[] = new int[]{0};

		// TODO: Access the API instead of using reflection
		//TODO HOTFIX!
		/*onWorld.replace("_nether","");
		onWorld.replace("_nether","_the_end");
*/

		api.getAddonsManager().getAddonByName("Level").ifPresent(addon -> {
			try {
				Method method = addon.getClass().getMethod("getIslandLevel", World.class, UUID.class);
				long rawLevel = (long) method.invoke(addon, Bukkit.getWorld(onWorld), uuid);
				level[0] = Math.toIntExact(rawLevel);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		return level[0];
	}

	@Override
	public Optional<UUID> getIslandOwner(Location loc) {
		Optional<Island> optIsland = api.getIslands().getIslandAt(loc);
		Optional<UUID> optional = Optional.empty();

		//TODO HOTFIX!
		/*String onWorld = loc.getWorld().getName();
		onWorld.replace("_nether","");
		onWorld.replace("_nether","_the_end");
		loc.setWorld(Bukkit.getWorld(onWorld));
*/
		if(optIsland.isPresent()) {
			optional = Optional.of(api.getIslands().getIslandAt(loc).get().getOwner());
		}
		return optional;
	}

	@Override
	public String[] getSkyBlockWorldNames() {
		return api.getIWM().getOverWorlds().stream().map(w -> w.getName()).map(w -> new String[]{w,w+"_nether",w+"_the_end"}).flatMap(l -> Stream.of(l)).toArray(String[]::new);
	}

}
