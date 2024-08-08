package de.guntram.mcmod.advancementinfo;

import de.guntram.mcmod.advancementinfo.accessors.AdvancementScreenAccessor;
import de.guntram.mcmod.advancementinfo.accessors.AdvancementWidgetAccessor;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.gui.screen.advancement.AdvancementTab;
import net.minecraft.client.gui.screen.advancement.AdvancementWidget;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.*;

public class AdvancementInfo implements ClientModInitializer {
    static final String MODID = "advancementinfo";
    static final Logger LOGGER = LogManager.getLogger();

    static public AdvancementWidget mouseOver, mouseClicked;
    static public List<AdvancementStep> cachedClickList;
    static public int cachedClickListLineCount;
    public static boolean showAll;
    public static ModConfig config;

    public static List<AdvancementStep> getSteps(AdvancementWidgetAccessor widget) {
        List<AdvancementStep> result = new ArrayList<>();
        addStep(result, widget.advancementInfo$getProgress(), widget.advancementInfo$getProgress().getUnobtainedCriteria(), false);
        addStep(result, widget.advancementInfo$getProgress(), widget.advancementInfo$getProgress().getObtainedCriteria(), true);
        return result;
    }

    private static void addStep(List<AdvancementStep> result, AdvancementProgress progress, Iterable<String> criteria, boolean obtained) {
        final String[] prefixes = new String[]{"item.minecraft", "block.minecraft", "entity.minecraft", "container", "effect.minecraft", "biome.minecraft"};
        // criteria is actually a List<> .. but play nice
        ArrayList<String> sorted = new ArrayList<>();
        for (String s : criteria) {
            sorted.add(s);
        }
        ArrayList<String> details = null;
        Collections.sort(sorted);
        for (String s : sorted) {
            String translation = null;
            String key = s;
            if (key.startsWith("minecraft:")) {
                key = key.substring(10);
            }
            if (key.startsWith("armor_trimmed_minecraft:")) {
                key = key.substring(24);
            }
            if (key.startsWith("textures/entity/")) {
                String entityAppearance = key.substring(16);
                int dotPos;
                if ((dotPos = entityAppearance.indexOf(".")) > 0) {
                    entityAppearance = entityAppearance.substring(0, dotPos);
                }
                translation = entityAppearance;
            }
            if (translation == null) {
                for (String prefix : prefixes) {
                    if (I18n.hasTranslation(prefix + "." + key)) {
                        translation = I18n.translate(prefix + "." + key);
                        break;
                    }
                }
            }
            if (translation == null) {
            /*
                As this helps only very few advancements which use potion effects, and
                we can't get the conditions the way we did any more, just ignore this
                and use the key, which we do with everything else anyway.

                CriterionConditions conditions = ((AdvancementProgressAccessor)(progress)).getCriterion(s).conditions();
                if (conditions != null) {
                    JsonObject o = conditions.toJson();
                    JsonElement maybeEffects = o.get("effects");
                    if (maybeEffects != null && maybeEffects instanceof JsonObject) {
                        JsonObject effects = (JsonObject) maybeEffects;
                        details = new ArrayList<>(effects.entrySet().size());
                        for (Map.Entry<String, JsonElement> entry: effects.entrySet()) {
                            details.add(I18n.translate("effect."+entry.getKey().replace(':', '.')));
                        }
                    }
                }
            */
                translation = key;
            }
            result.add(new AdvancementStep(translation, obtained, details));
        }
    }

    public static void setMatchingFrom(AdvancementsScreen screen, String text) {
        List<AdvancementStep> result = new ArrayList<>();
        ClientAdvancementManager advancementHandler = ((AdvancementScreenAccessor) screen).advancementInfo$getAdvancementHandler();
        Collection<PlacedAdvancement> all = advancementHandler.getManager().getAdvancements();
        int lineCount = 0;

        text = text.toLowerCase();
        for (PlacedAdvancement adv : all) {
            Identifier id = adv.getAdvancementEntry().id();
            if (id.getPath().startsWith("recipes/")) {
                continue;
            }
            Optional<AdvancementDisplay> display = adv.getAdvancement().display();
            if (display.isEmpty()) {
                LOGGER.debug("! {} Has no display", id);
                continue;
            }
            if (display.get().getTitle() == null) {
                LOGGER.debug("! {} Has no title", id);
                continue;
            }
            if (display.get().getDescription() == null) {
                LOGGER.debug("! {} Has no description", id);
                continue;
            }
            String title = display.get().getTitle().getString();
            String desc = display.get().getDescription().getString();
            LOGGER.debug("- {} {}: {} ", id, title, desc);
            if (title.toLowerCase().contains(text)
                || desc.toLowerCase().contains(text)) {
                ArrayList<String> details = new ArrayList<>();
                details.add(desc);
                AdvancementTab tab = ((AdvancementScreenAccessor) screen).advancementInfo$myGetTab(adv);
                if (tab == null) {
                    LOGGER.info("no tab found for advancement {} title {} description {}", id, title, desc);
                    continue;
                }
                details.add(tab.getTitle().getString());
                boolean done = ((AdvancementWidgetAccessor) (screen.getAdvancementWidget(adv))).advancementInfo$getProgress().isDone();
                result.add(new AdvancementStep(title, done, details));
                lineCount += 3;
            }
        }
        cachedClickList = result;
        cachedClickListLineCount = lineCount;
        mouseOver = null;
    }

    @Override
    public void onInitializeClient() {
        Configurator.setLevel(LOGGER.getName(), Level.ALL);
        showAll = false;
        if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            ConfigHolder<ModConfig> configHolder = AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
            configHolder.registerSaveListener((holder, modConfig) -> {
                modConfig.validate();
                return ActionResult.PASS;
            });
            config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        } else {
            config = new ModConfig();
        }

        LOGGER.info("AdvancementInfo initialized");
    }
}
