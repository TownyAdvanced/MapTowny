/*
 * Copyright (c) 2022 Silverwolfg11
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

package me.silverwolfg11.maptowny.platform.bluemap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import me.silverwolfg11.maptowny.platform.bluemap.markerops.MarkerOp;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlueMapMarkerProcessor {

    private final ConcurrentLinkedQueue<MarkerOp> markerOpsQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean processingScheduled = new AtomicBoolean(false);
    private final AtomicBoolean processingRunning = new AtomicBoolean(false);
    private final AtomicBoolean enableScheduling = new AtomicBoolean(false);

    private final JavaPlugin plugin;
    private final BlueMapIconMapper iconMapper;

    public BlueMapMarkerProcessor(JavaPlugin plugin, BlueMapIconMapper iconMapper) {
        this.plugin = plugin;
        this.iconMapper = iconMapper;
    }

    // Enable scheduling the marker processing task
    // Before this is executed, no task should be scheduled because the API is not yet enabled.
    public void enableScheduling() {
        enableScheduling.set(true);
        checkAndSchedule();
    }

    // Disable scheduling marker processing tasks.
    // This is because the MapTowny plugin is disabling.
    public void disableScheduling() {
        enableScheduling.set(false);
        processQueue();
    }

    private void scheduleProcessing() {
        processingScheduled.set(true);
        // Schedule processing 5 ticks later
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::processQueue, 5L);
    }

    // Check if the processing task can be scheduled based on 3 conditions:
    // - Scheduling is enabled.
    // - No processing task has already been scheduled.
    // - No processing task is currently running.
    //
    // If all 3 conditions were met, then schedule a task.
    private synchronized void checkAndSchedule() {
        if (enableScheduling.get() && !processingScheduled.get() && !processingRunning.get()) {
            scheduleProcessing();
        }
    }

    public void queueMarkerOp(MarkerOp markerOp) {
        markerOpsQueue.add(markerOp);
        checkAndSchedule();
    }

    public void queueMarkerOps(Collection<MarkerOp> markerOps) {
        markerOpsQueue.addAll(markerOps);
        checkAndSchedule();
    }

    // Transfers all Queue items into a list
    // This will allow the processing task to work with a fixed number of items.
    private Collection<MarkerOp> segregateQueue() {
        List<MarkerOp> markerOps = new ArrayList<>(markerOpsQueue.size());
        while (!markerOpsQueue.isEmpty()) {
            MarkerOp markerOp = markerOpsQueue.poll();
            markerOps.add(markerOp);
        }

        return markerOps;
    }

    // Process all marker operations
    private void processQueue() {
        // Order matters here
        // Indicate that processing is running before removing that it is scheduled to prevent a gap in logic that
        // could potentially schedule another process.
        processingRunning.set(true);
        processingScheduled.set(false);

        Collection<MarkerOp> markerOps = segregateQueue();

        if (!markerOps.isEmpty()) {
            BlueMapAPI api = BlueMapAPI.getInstance().orElse(null);

            if (api == null)
                return;

            try {
                MarkerAPI markerAPI = api.getMarkerAPI();
                for (MarkerOp markerOp : markerOps) {
                    markerOp.run(api, markerAPI, iconMapper);
                }
                markerAPI.save();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Schedule a new processing task if there are new operations
        if (!markerOpsQueue.isEmpty() && enableScheduling.get()) {
            scheduleProcessing();
        }

        processingRunning.set(false);
    }

}
