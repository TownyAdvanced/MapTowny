package me.silverwolfg11.pl3xmaptowny.listeners;

import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import me.silverwolfg11.pl3xmaptowny.Pl3xMapTowny;
import me.silverwolfg11.pl3xmaptowny.objects.TownRenderEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class TownClaimListener implements Listener {

    private final Pl3xMapTowny plugin;

    public TownClaimListener(Pl3xMapTowny plugin) {
        this.plugin = plugin;
    }

    // Render town as soon as it's claimed
    @EventHandler
    public void onNewTown(NewTownEvent event) {
        if (!event.getTown().hasHomeBlock())
            return;

        TownRenderEntry tre = plugin.getLayerManager().buildTownEntry(event.getTown());
        plugin.async(() -> plugin.getLayerManager().renderTown(tre));
    }

    // Remove town as soon as it's deleted.
    @EventHandler
    public void onTownDelete(DeleteTownEvent event) {
        UUID townUUID = event.getTownUUID();
        plugin.getLayerManager().removeTownMarker(townUUID);
    }

}
