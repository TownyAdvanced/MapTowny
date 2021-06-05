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
            if(town.isRuined())
                continue;
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
