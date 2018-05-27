package de.Linus122.customoregen;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Events implements Listener {
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onFromTo(BlockFromToEvent event) {
		if (Main.disabledWorlds.contains(event.getBlock().getLocation().getWorld().getName())) {
			return;
		}
		
		int id = event.getBlock().getTypeId();
		if ((id >= 8) && (id <= 11) && id != 9) {
			Block b = event.getToBlock();
			int toid = b.getTypeId();
			if ((toid == 0) && (generatesCobble(id, b))) {
				OfflinePlayer p = Main.getOwner(b.getLocation());
				if (p == null)
					return;
				GeneratorConfig gc = Main.getGeneratorConfigForPlayer(p);
				if (gc == null)
					return;
				if (getObject(gc) == null)
					return;
				GeneratorItem winning = getObject(gc);
				if (Material.getMaterial(winning.name) == null)
					return;

				if (Material.getMaterial(winning.name).equals(Material.COBBLESTONE) && winning.damage == 0) {
					return;
				}
				event.setCancelled(true);
				b.setType(Material.getMaterial(winning.name));
				// <Block>.setData(...) is deprecated, but there is no
				// alternative to it. #spigot
				b.setData(winning.damage, true);
			}
		}

	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Main.getGeneratorConfigForPlayer(e.getPlayer());
	}

	public GeneratorItem getObject(GeneratorConfig gc) {

		Random random = new Random();
		double d = random.nextDouble() * 100;
		for (GeneratorItem key : gc.itemList) {
			if ((d -= key.chance) < 0)
				return key;
		}
		return new GeneratorItem("COBBLESTONE", (byte) 0, 0); // DEFAULT
	}

	private final BlockFace[] faces = { BlockFace.SELF, BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST,
			BlockFace.SOUTH, BlockFace.WEST };

	public boolean generatesCobble(int id, Block b) {
		int mirrorID1 = (id == 8) || (id == 9) ? 10 : 8;
		int mirrorID2 = (id == 8) || (id == 9) ? 11 : 9;
		for (BlockFace face : this.faces) {
			Block r = b.getRelative(face, 1);
			if ((r.getTypeId() == mirrorID1) || (r.getTypeId() == mirrorID2)) {
				return true;
			}
		}
		return false;
	}
}
