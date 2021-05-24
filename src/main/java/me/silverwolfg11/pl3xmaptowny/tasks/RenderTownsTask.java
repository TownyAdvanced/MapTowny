package me.silverwolfg11.pl3xmaptowny.tasks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Town;
import me.silverwolfg11.pl3xmaptowny.Pl3xMapTowny;
import me.silverwolfg11.pl3xmaptowny.objects.TownRenderEntry;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Must be ran synchronously
// Uses TownyAPI
public class RenderTownsTask extends BukkitRunnable {

    private final Pl3xMapTowny plugin;

    public RenderTownsTask(Pl3xMapTowny plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        Set<UUID> renderedTowns = new HashSet<>(plugin.getLayerManager().getRenderedTowns());
        final List<TownRenderEntry> townsToRender = new ArrayList<>();

        for (Town town : TownyUniverse.getInstance().getTowns()) {
            townsToRender.add(plugin.getLayerManager().buildTownEntry(town));
            renderedTowns.remove(town.getUUID());
        }

        // Render towns async
        plugin.async(() -> townsToRender.forEach(tre -> plugin.getLayerManager().renderTown(tre)));

        // Remove deleted towns from map
        for (UUID renderedTown : renderedTowns) {
            plugin.getLayerManager().removeTownMarker(renderedTown);
        }
    }
}
