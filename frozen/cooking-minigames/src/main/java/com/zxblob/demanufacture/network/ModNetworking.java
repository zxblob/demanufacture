package com.zxblob.demanufacture.network;

import com.zxblob.demanufacture.Demanufacture;
import com.zxblob.demanufacture.client.ClientNetworkHandlers;
import com.zxblob.demanufacture.cooking.CookModeUtil;
import com.zxblob.demanufacture.cooking.CookingIngredientHandler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Demanufacture.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking
{
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TakeBasketItemPayload.TYPE, TakeBasketItemPayload.STREAM_CODEC, CookingIngredientHandler::handleTakeBasketItem);
        registrar.playToServer(SetPotLiquidPayload.TYPE, SetPotLiquidPayload.STREAM_CODEC, CookingIngredientHandler::handleSetPotLiquid);
        registrar.playToServer(FinishCookingPayload.TYPE, FinishCookingPayload.STREAM_CODEC, CookingIngredientHandler::handleFinishCooking);
        registrar.playToServer(TakeIngredientOutPayload.TYPE, TakeIngredientOutPayload.STREAM_CODEC, CookingIngredientHandler::handleTakeIngredientOut);
        registrar.playToServer(RuinPotPayload.TYPE, RuinPotPayload.STREAM_CODEC, CookingIngredientHandler::handleRuinPot);
        registrar.playToServer(ServePotPayload.TYPE, ServePotPayload.STREAM_CODEC, CookingIngredientHandler::handleServePot);
        registrar.playToServer(ToggleCookModePayload.TYPE, ToggleCookModePayload.STREAM_CODEC, CookModeUtil::handleToggleCookMode);
        registrar.playToClient(OpenStirringScreenPayload.TYPE, OpenStirringScreenPayload.STREAM_CODEC, ClientNetworkHandlers::handleOpenStirringScreen);
        registrar.playToClient(OpenScramblingScreenPayload.TYPE, OpenScramblingScreenPayload.STREAM_CODEC, ClientNetworkHandlers::handleOpenScramblingScreen);
    }
}
