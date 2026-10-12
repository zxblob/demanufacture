package com.zxblob.demanufacture.cooking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * First pass at the real dish-output system: names a dish from what went into it, grades it from
 * how the cook actually went, and writes a throwaway Mad-Libs backstory. Word banks and tone here
 * are a first guess, not a locked-in design - they're plain string arrays specifically so they're
 * easy to rewrite wholesale without touching any of the surrounding logic.
 */
public class DishNaming
{
    private static final Random RANDOM = new Random();

    public enum Quality
    {
        UNDERCOOKED("Undercooked", "Eat at your own risk."),
        MEDIOCRE("Questionable", "It's... edible."),
        DECENT("Decent", "Not bad, honestly."),
        GOOD("Tasty", "Surprisingly delicious."),
        EXCELLENT("Masterful", "A genuine masterpiece.");

        public final String adjective;
        public final String remark;

        Quality(String adjective, String remark)
        {
            this.adjective = adjective;
            this.remark = remark;
        }
    }

    public static Quality gradeQuality(boolean anyRaw, double paceAtServe)
    {
        if (anyRaw)
            return Quality.UNDERCOOKED;
        if (paceAtServe < 40)
            return Quality.MEDIOCRE;
        if (paceAtServe < 70)
            return Quality.DECENT;
        if (paceAtServe < 90)
            return Quality.GOOD;
        return Quality.EXCELLENT;
    }

    private static String cookingMethod(ItemStack liquid)
    {
        if (liquid.isEmpty())
            return "Raw";
        if (LiquidItems.isWater(liquid))
            return "Boiled";
        if (LiquidItems.isOil(liquid))
            return "Fried";
        return "Cooked";
    }

    public static String generateName(List<ItemStack> ingredients, ItemStack liquid, Quality quality)
    {
        String method = cookingMethod(liquid);

        Set<String> distinctNames = new LinkedHashSet<>();
        for (ItemStack stack : ingredients)
            distinctNames.add(stack.getHoverName().getString());

        String ingredientPart;
        if (distinctNames.isEmpty())
            ingredientPart = "Mystery Dish";
        else
        {
            List<String> names = new ArrayList<>(distinctNames);
            if (names.size() <= 2)
                ingredientPart = String.join(" & ", names);
            else
                ingredientPart = names.get(0) + " & " + names.get(1) + " & More";
        }

        // Quality adjective is only worth saying for the extremes - a "Decent" dish doesn't need
        // to announce its own mediocrity in its own name.
        String prefix = quality == Quality.DECENT ? "" : quality.adjective + " ";
        return (prefix + method + " " + ingredientPart).trim();
    }

    private static final String[] SUBJECTS = {
            "a wandering chef", "your grandmother", "a suspicious goblin", "the village baker",
            "a retired pirate captain", "a sleep-deprived wizard", "a traveling merchant",
            "three overconfident apprentices"
    };
    private static final String[] ACTIONS = {
            "invented", "perfected", "accidentally discovered", "stole the recipe for",
            "swore a sacred oath over", "bet their house on"
    };
    private static final String[] OCCASIONS = {
            "during a thunderstorm", "on a dare", "to win a bet", "while the kitchen was on fire",
            "after three days without sleep", "under a full moon", "during a siege",
            "to impress a skeptical dragon"
    };

    public static List<Component> generateLore(Quality quality)
    {
        String subject = SUBJECTS[RANDOM.nextInt(SUBJECTS.length)];
        String action = ACTIONS[RANDOM.nextInt(ACTIONS.length)];
        String occasion = OCCASIONS[RANDOM.nextInt(OCCASIONS.length)];

        String backstory = "Legend says " + subject + " " + action + " this dish " + occasion + ".";
        return List.of(Component.literal(backstory), Component.literal(quality.remark));
    }
}
