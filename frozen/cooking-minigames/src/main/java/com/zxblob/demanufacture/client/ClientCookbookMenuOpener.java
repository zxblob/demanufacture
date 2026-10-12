package com.zxblob.demanufacture.client;

import com.zxblob.demanufacture.client.screen.CookbookMenuScreen;

import net.minecraft.client.Minecraft;

public class ClientCookbookMenuOpener
{
    public static void open()
    {
        Minecraft.getInstance().setScreen(new CookbookMenuScreen());
    }
}
