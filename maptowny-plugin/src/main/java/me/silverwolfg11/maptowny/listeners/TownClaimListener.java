/*
 * Copyright (c) 2021 Silverwolfg11
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.silverwolfg11.maptowny.listeners;

import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import me.silverwolfg11.maptowny.MapTowny;
import me.silverwolfg11.maptowny.objects.TownRenderEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class TownClaimListener implements Listener {

    private final MapTowny plugin;

    public TownClaimListener(MapTowny plugin) {
        this.plugin = plugin;
    }

    // Render town as soon as it's claimed
    @EventHandler
    public void onNewTown(NewTownEvent event) {
        if (!event.getTown().hasHomeBlock())
            return;

        TownRenderEntry tre = plugin.getLayerManager().buildTownEntry(event.getTown());
        plugin.getScheduler().scheduleAsyncTask(() -> plugin.getLayerManager().renderTown(tre));
    }

    // Remove town as soon as it's deleted.
    @EventHandler
    public void onTownDelete(DeleteTownEvent event) {
        UUID townUUID = event.getTownUUID();
        plugin.getLayerManager().removeTownMarker(townUUID);
    }

}
