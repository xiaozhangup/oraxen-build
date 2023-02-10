package io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.directional.DirectionalBlock;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.farmblock.FarmBlockTask;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.logstrip.LogStripListener;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.NoteBlock;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NoteBlockMechanicFactory extends MechanicFactory {

    public static final Map<Integer, NoteBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private static JsonObject variants;
    private static NoteBlockMechanicFactory instance;
    public final List<String> toolTypes;
    private boolean farmBlock;
    private static FarmBlockTask farmBlockTask;
    public final int farmBlockCheckDelay;
    public final boolean customSounds;

    public NoteBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants = new JsonObject();
        variants.add("instrument=harp,powered=false", getModelJson("block/note_block"));
        toolTypes = section.getStringList("tool_types");
        farmBlockCheckDelay = section.getInt("farmblock_check_delay");
        farmBlock = false;
        customSounds = OraxenPlugin.get().getConfigsManager().getMechanics().getConfigurationSection("custom_block_sounds").getBoolean("noteblock_and_block", true);

        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(), packFolder ->
                OraxenPlugin.get().getResourcePack().writeStringToVirtual(
                        "assets/minecraft/blockstates", "note_block.json", getBlockstateContent())
        );
        MechanicsManager.registerListeners(OraxenPlugin.get(),
                new NoteBlockMechanicListener(),
                new LogStripListener()
        );
        if (customSounds) MechanicsManager.registerListeners(OraxenPlugin.get(), new NoteBlockSoundListener());
    }

    public static String getInstrumentName(int id) {
        return switch (id / 25 % 384) {
            case 1 -> "basedrum";
            case 2 -> "snare";
            case 3 -> "hat";
            case 4 -> "bass";
            case 5 -> "flute";
            case 6 -> "bell";
            case 7 -> "guitar";
            case 8 -> "chime";
            case 9 -> "xylophone";
            case 10 -> "iron_xylophone";
            case 11 -> "cow_bell";
            case 12 -> "didgeridoo";
            case 13 -> "bit";
            case 14 -> "banjo";
            case 15 -> "pling";
            default -> "harp";
        };
    }

    public static JsonObject getModelJson(String modelName) {
        JsonObject content = new JsonObject();
        content.addProperty("model", modelName);

        return content;
    }

    public static JsonObject getDirectionalModelJson(String modelName, NoteBlockMechanic mechanic, NoteBlockMechanic parentMechanic) {
        String itemId = mechanic.getItemID();
        JsonObject content = new JsonObject();
        DirectionalBlock parent = parentMechanic.getDirectional();
        String subBlockModel = mechanic.getDirectional().getDirectionalModel(mechanic);
        content.addProperty("model", subBlockModel != null ? subBlockModel : modelName);
        // If subModel is specified and is different from parent we don't want to rotate it
        if (subBlockModel != null && !Objects.equals(subBlockModel, modelName)) return content;

        if (Objects.equals(parent.getYBlock(), itemId))
            return content;
        else if (Objects.equals(parent.getXBlock(), itemId)) {
            content.addProperty("x", 90);
            content.addProperty("z", 90);
        } else if (Objects.equals(parent.getZBlock(), itemId)) {
            content.addProperty("y", 90);
            content.addProperty("x", 90);
        } else if (Objects.equals(parent.getNorthBlock(), itemId))
            return content;
        else if (Objects.equals(parent.getEastBlock(), itemId)) {
            content.addProperty("y", 90);
        } else if (Objects.equals(parent.getSouthBlock(), itemId))
            content.addProperty("y", 180);
        else if (Objects.equals(parent.getWestBlock(), itemId)) {
            content.addProperty("z", 90);
            content.addProperty("y", 270);
        } else if (Objects.equals(parent.getUpBlock(), itemId))
            content.addProperty("y", 270);
        else if (Objects.equals(parent.getDownBlock(), itemId))
            content.addProperty("x", 180);

        return content;
    }

    public static String getBlockstateVariantName(int id) {
        id += 26;
        return getBlockstateVariantName(getInstrumentName(id), id % 25, id >= 400);
    }

    public static String getBlockstateVariantName(String instrument, int note, boolean powered) {
        return "instrument=" + instrument + ",note=" + note + ",powered=" + powered;
    }

    public static NoteBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static NoteBlockMechanicFactory getInstance() {
        return instance;
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("noteblock");
        NoteBlockMechanic noteBlockMechanic = (NoteBlockMechanic) mechanicFactory.getMechanic(itemId);
        block.setBlockData(createNoteBlockData(noteBlockMechanic.getCustomVariation()), false);
    }

    private String getBlockstateContent() {
        JsonObject noteblock = new JsonObject();
        noteblock.add("variants", variants);
        return noteblock.toString();
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        NoteBlockMechanic mechanic = new NoteBlockMechanic(this, itemMechanicConfiguration);
        DirectionalBlock directional = mechanic.getDirectional();
        String modelName = mechanic.getModel(itemMechanicConfiguration.getParent().getParent());

        if (mechanic.isDirectional() && !directional.isParentBlock()) {
            NoteBlockMechanic parentMechanic = directional.getParentMechanic();
            modelName = (parentMechanic.getModel(itemMechanicConfiguration.getParent().getParent()));
            variants.add(getBlockstateVariantName(mechanic.getCustomVariation()),
                    getDirectionalModelJson(modelName, mechanic, parentMechanic));
        } else {
            variants.add(getBlockstateVariantName(mechanic.getCustomVariation()),
                    getModelJson(modelName));
        }

        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    /**
     * Generate a NoteBlock blockdata from its id
     *
     * @param id The block id.
     */
    @SuppressWarnings("deprecation")
    public static NoteBlock createNoteBlockData(int id) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        id += 26;
        NoteBlock noteBlock = (NoteBlock) Bukkit.createBlockData(Material.NOTE_BLOCK);
        noteBlock.setInstrument(Instrument.getByType((byte) (id / 25 % 400)));
        noteBlock.setNote(new Note(id % 25));
        noteBlock.setPowered(id >= 400);
        return noteBlock;
    }

    /**
     * Generate a NoteBlock blockdata from an oraxen id
     *
     * @param itemID The id of an item implementing NoteBlockMechanic
     */
    public NoteBlock createNoteBlockData(String itemID) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        return createNoteBlockData(((NoteBlockMechanic) getInstance().getMechanic(itemID)).getCustomVariation());
    }

    public void registerFarmBlock() {
        if (farmBlock) return;
        if (farmBlockTask != null) farmBlockTask.cancel();

//        // Dont register if there is no farmblocks in configs
//        List<String> farmblockList = new ArrayList<>();
//        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
//            String id = OraxenItems.getIdByItem(itemBuilder.build());
//            NoteBlockMechanic mechanic = (NoteBlockMechanic) NoteBlockMechanicFactory.getInstance().getMechanic(id);
//            if (mechanic == null || !mechanic.hasDryout()) continue;
//            farmblockList.add(id);
//        }
//        if (farmblockList.isEmpty()) return;

        farmBlockTask = new FarmBlockTask(farmBlockCheckDelay);

        farmBlockTask.runTaskTimer(OraxenPlugin.get(), 0, farmBlockCheckDelay);
        farmBlock = true;
    }

}
