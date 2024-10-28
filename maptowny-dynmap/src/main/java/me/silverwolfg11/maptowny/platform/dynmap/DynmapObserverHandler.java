/*
 * Copyright (c) 2024 Silverwolfg11
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

package me.silverwolfg11.maptowny.platform.dynmap;

import me.silverwolfg11.maptowny.platform.MapPlatformObserver;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class DynmapObserverHandler {
    private final CopyOnWriteArrayList<MapPlatformObserver> observers = new CopyOnWriteArrayList<>();
    private final List<MapPlatformObserver> unsetupObservers = new ArrayList<>();

    private final DynmapCommonAPIListener dynmapListener;
    private final AtomicReference<DynmapCommonAPI> dynmapApiRef = new AtomicReference<>();

    public DynmapObserverHandler() {
        dynmapListener = new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
                DynmapObserverHandler.this.onDynmapEnabled(dynmapCommonAPI);
            }

            @Override
            public void apiDisabled(DynmapCommonAPI api) {
                DynmapObserverHandler.this.onDynmapDisabled();
            }
        };

        // Listener runs immediately if dynmap api is enabled.
        DynmapCommonAPIListener.register(dynmapListener);
    }

    public boolean registerObserver(MapPlatformObserver observer) {
        if (observers.contains(observer)) {
            return false;
        }

        synchronized (unsetupObservers) {
            if (unsetupObservers.contains(observer)) {
                return false;
            }
        }

        setupObserver(observer);
        return true;
    }

    public boolean unregisterObserver(MapPlatformObserver observer) {
        if (observers.remove(observer)) {
            return true;
        }

        synchronized (unsetupObservers) {
            if (unsetupObservers.remove(observer)) {
                return true;
            }
        }

        return false;
    }

    public void disableObservers() {
        DynmapCommonAPIListener.unregister(dynmapListener);
    }

    private void setupObserver(MapPlatformObserver observer) {
        if (dynmapApiRef.get() != null) {
            observer.onObserverSetup();
            observers.add(observer);
        }
        else {
            synchronized (unsetupObservers) {
                unsetupObservers.add(observer);
            }
        }
    }

    private void onDynmapEnabled(DynmapCommonAPI api) {
        dynmapApiRef.set(api);

        for (MapPlatformObserver observer : observers) {
            observer.onPlatformEnabled();
        }

        List<MapPlatformObserver> tmpObservers;
        synchronized (unsetupObservers) {
            tmpObservers = new ArrayList<>(unsetupObservers);
            unsetupObservers.clear();
        }

        // Setup first-time observers
        for (MapPlatformObserver observer : tmpObservers) {
            observer.onObserverSetup();
        }

        observers.addAll(tmpObservers);
    }

    private void onDynmapDisabled() {
        dynmapApiRef.set(null);

        for (MapPlatformObserver observer : observers) {
            observer.onPlatformDisabled();
        }
    }
}
