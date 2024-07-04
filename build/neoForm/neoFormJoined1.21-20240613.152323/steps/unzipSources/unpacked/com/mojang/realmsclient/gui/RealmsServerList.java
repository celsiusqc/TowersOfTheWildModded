package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsServer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsServerList implements Iterable<RealmsServer> {
    private final Minecraft minecraft;
    private final Set<RealmsServer> removedServers = new HashSet<>();
    private List<RealmsServer> servers = List.of();

    public RealmsServerList(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void updateServersList(List<RealmsServer> pServers) {
        List<RealmsServer> list = new ArrayList<>(pServers);
        list.sort(new RealmsServer.McoServerComparator(this.minecraft.getUser().getName()));
        boolean flag = list.removeAll(this.removedServers);
        if (!flag) {
            this.removedServers.clear();
        }

        this.servers = list;
    }

    public void removeItem(RealmsServer pServer) {
        this.servers.remove(pServer);
        this.removedServers.add(pServer);
    }

    @Override
    public Iterator<RealmsServer> iterator() {
        return this.servers.iterator();
    }

    public boolean isEmpty() {
        return this.servers.isEmpty();
    }
}
