/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.client.Minecraft
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.client.event.RegisterClientCommandsEvent
 */
package dev.dedek.greatsage.client;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.dedek.greatsage.client.voice.VoiceInput;
import dev.dedek.greatsage.voice.VoiceCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid="greatsage", value={Dist.CLIENT})
public final class VoiceConsole {
    private VoiceConsole() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"sage").executes(context -> {
            ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.translatable((String)"greatsage.voice.console.usage", (Object[])new Object[]{VoiceCommand.count()}), false);
            return 1;
        })).then(Commands.argument((String)"request", (ArgumentType)StringArgumentType.greedyString()).executes(context -> {
            VoiceInput.submit(StringArgumentType.getString((CommandContext)context, (String)"request"), Minecraft.getInstance().player);
            return 1;
        })));
    }
}

