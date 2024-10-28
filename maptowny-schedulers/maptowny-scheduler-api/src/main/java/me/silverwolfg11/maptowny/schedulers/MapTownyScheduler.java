/*
 * Copyright (c) 2023 Silverwolfg11
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

package me.silverwolfg11.maptowny.schedulers;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;

/**
 * Generic scheduler for the MapTowny plugin.
 * Assume all scheduling is done is on a global region.
 *
 * @since 3.0.0
 */
public abstract class MapTownyScheduler {
    final protected JavaPlugin plugin;
    final protected Executor syncExecutor;
    final protected Executor asyncExecutor;

    protected MapTownyScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.syncExecutor = this::scheduleTask;
        this.asyncExecutor = this::scheduleAsyncTask;
    }

    abstract public void cancelAllTasks();

    abstract public void scheduleTask(Runnable task);

    abstract public void scheduleAsyncTask(Runnable task);

    abstract public void scheduleRepeatingTask(Runnable task, long delay, long period);

    /**
     * Return an executor that will schedule tasks
     * on the default global thread.
     */
    public Executor getExecutor() {
        return syncExecutor;
    }

    /**
     * Return an executor that will schedule tasks
     * on an asynchronous Bukkit-managed thread.
     */
    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }
}
