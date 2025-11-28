package com.zetsumei.nocoin.event;

import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.command.GachaCommand;
import com.zetsumei.nocoin.command.NocoinCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gestionnaire pour l'enregistrement des commandes.
 */
@Mod.EventBusSubscriber(modid = Nocoin.MODID)
public class CommandEventHandler {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NocoinCommand.register(event.getDispatcher());
        GachaCommand.register(event.getDispatcher());
    }
}
