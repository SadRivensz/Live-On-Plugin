package com.liveon;

import com.liveon.LiveOnPlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LiveOnPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(LiveOnPlugin.class);
        RuneLite.main(args);
    }
}
