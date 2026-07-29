package com.etema.attributemodify.editor.client;

import com.etema.attributemodify.AttributeModify;
import com.etema.attributemodify.ItemAttributeDataManager;
import com.etema.attributemodify.service.AttributeInspectionContext;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Detailed, in-game attribute diagnostics for the held item. */
public final class ItemAttributeInspectCommand {
    private ItemAttributeInspectCommand() {
    }

    public static int inspect(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0;
        }

        ItemStack stack = minecraft.player.getItemInHand(hand);
        if (stack.isEmpty()) {
            send(Component.literal("[AttributeModify] No hay un objeto en esa mano.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        line("==================================================");
        send(Component.literal("AttributeModify: inspección profunda").withStyle(ChatFormatting.GOLD));
        line("Item: " + itemId);
        line("Clase: " + stack.getItem().getClass().getName());
        line("Mano: " + hand.name().toLowerCase(Locale.ROOT) + " | Cantidad: " + stack.getCount());
        line("NBT: " + (stack.hasTag() ? stack.getTag() : "<sin NBT>"));

        boolean foundAnything = false;
        ItemAttributeDataManager manager = ItemAttributeDataManager.getInstance();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> defaults = stack.getItem().getDefaultAttributeModifiers(slot);
            Multimap<Attribute, AttributeModifier> external =
                    AttributeInspectionContext.inspectExternalAttributes(() -> stack.getAttributeModifiers(slot));
            Multimap<Attribute, AttributeModifier> effective = stack.getAttributeModifiers(slot);
            List<ItemAttributeDataManager.AttributeEntry> rules = manager.getEntriesForSlot(stack.getItem(), slot);

            if (defaults.isEmpty() && external.isEmpty() && effective.isEmpty() && rules.isEmpty()) {
                continue;
            }

            foundAnything = true;
            send(Component.literal("Slot: " + slot.getName()).withStyle(ChatFormatting.AQUA));
            printModifiers("DEFAULT", defaults, null, null);
            printModifiers("EXTERNAL", external, defaults, null);
            printRules(rules, stack);
            printModifiers("EFFECTIVE", effective, defaults, external);
        }

        if (!foundAnything) {
            line("No se encontraron modificadores ni reglas en ningún slot.");
        }
        line("DEFAULT=objeto | EXTERNAL=eventos sin AttributeModify | EFFECTIVE=resultado final");
        line("El informe completo también fue escrito en latest.log.");
        line("==================================================");
        return 1;
    }

    private static void printModifiers(String section, Multimap<Attribute, AttributeModifier> modifiers,
            Multimap<Attribute, AttributeModifier> defaults,
            Multimap<Attribute, AttributeModifier> external) {
        if (modifiers.isEmpty()) {
            return;
        }

        line("  " + section + ":");
        for (var entry : modifiers.entries()) {
            Attribute attribute = entry.getKey();
            AttributeModifier modifier = entry.getValue();
            ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            String origin = "";
            if ("EXTERNAL".equals(section)) {
                origin = containsEquivalent(defaults == null ? List.of() : defaults.get(attribute), modifier)
                        ? " [default]" : " [inyectado por evento]";
            } else if ("EFFECTIVE".equals(section)) {
                if (containsEquivalent(defaults == null ? List.of() : defaults.get(attribute), modifier)) {
                    origin = " [default]";
                } else if (containsEquivalent(external == null ? List.of() : external.get(attribute), modifier)) {
                    origin = " [externo]";
                } else {
                    origin = " [AttributeModify/otro evento tardío]";
                }
            }

            line("    " + attributeId + " amount=" + format(modifier.getAmount())
                    + " operation=" + modifier.getOperation().name()
                    + " uuid=" + modifier.getId()
                    + " name=\"" + modifier.getName() + "\"" + origin);
        }
    }

    private static void printRules(List<ItemAttributeDataManager.AttributeEntry> rules, ItemStack stack) {
        if (rules.isEmpty()) {
            return;
        }

        line("  RULES:");
        for (ItemAttributeDataManager.AttributeEntry rule : rules) {
            ResourceLocation attributeId = ForgeRegistries.ATTRIBUTES.getKey(rule.attribute());
            AttributeModifier modifier = rule.modifier();
            String value = modifier == null ? "" : " amount=" + format(modifier.getAmount())
                    + " operation=" + modifier.getOperation().name();
            line("    " + attributeId + " action=" + rule.action().name()
                    + value + " matches=" + rule.matches(stack));
        }
    }

    private static boolean containsEquivalent(Collection<AttributeModifier> candidates,
            AttributeModifier modifier) {
        if (modifier == null) {
            return false;
        }
        for (AttributeModifier candidate : candidates) {
            if (candidate == modifier || (candidate.getId().equals(modifier.getId())
                    && candidate.getOperation() == modifier.getOperation()
                    && Double.compare(candidate.getAmount(), modifier.getAmount()) == 0
                    && candidate.getName().equals(modifier.getName()))) {
                return true;
            }
        }
        return false;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.8f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static void line(String text) {
        AttributeModify.LOGGER.info("[inspect] {}", text);
        send(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    private static void send(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(component, false);
        }
    }
}
