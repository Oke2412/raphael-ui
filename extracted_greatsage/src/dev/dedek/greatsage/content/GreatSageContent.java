/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.MobCategory
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.DeferredRegister$Items
 */
package dev.dedek.greatsage.content;

import dev.dedek.greatsage.content.VenuzdonoaEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GreatSageContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems((String)"greatsage");
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create((ResourceKey)Registries.ENTITY_TYPE, (String)"greatsage");
    public static final Supplier<Item> VENUZDONOA_ITEM = ITEMS.register("venuzdonoa", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<EntityType<?>, EntityType<VenuzdonoaEntity>> VENUZDONOA = ENTITIES.register("venuzdonoa", () -> EntityType.Builder.of(VenuzdonoaEntity::new, (MobCategory)MobCategory.MISC).sized(0.6f, 3.0f).clientTrackingRange(12).updateInterval(20).fireImmune().noSummon().build("venuzdonoa"));

    private GreatSageContent() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
    }
}

