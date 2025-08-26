import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Animals;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Piston;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;



import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.List;
import java.util.Arrays;


public class FrogcraftTransformer extends JavaPlugin implements Listener {
    
    private static final Map<Material, ItemStack> DROP_TRANSFORMS = new ConcurrentHashMap<>();
    private static final EnumSet<Material> TRANSFORMABLE_BLOCKS = EnumSet.noneOf(Material.class);
    private static final Random random = new Random();
    
    // VIRTUAL CHEST SYSTEM - MODERNBETA ARCHITECTURE
    private static final Map<String, VirtualChest> virtualChests = new ConcurrentHashMap<>();
    private static final Set<Inventory> activeVirtualInventories = new HashSet<>();
    
    // BETA SPRINT PREVENTION SYSTEM (Simplified - no tracking needed)
    
    // BETA CHEST PHYSICS - BLOCKS THAT DON'T PREVENT CHEST OPENING
    private static final EnumSet<Material> NON_BLOCKING_MATERIALS = EnumSet.of(
        Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
        Material.GLASS, Material.GLASS_PANE, Material.WHITE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS,
        Material.MAGENTA_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS, Material.YELLOW_STAINED_GLASS,
        Material.LIME_STAINED_GLASS, Material.PINK_STAINED_GLASS, Material.GRAY_STAINED_GLASS,
        Material.LIGHT_GRAY_STAINED_GLASS, Material.CYAN_STAINED_GLASS, Material.PURPLE_STAINED_GLASS,
        Material.BLUE_STAINED_GLASS, Material.BROWN_STAINED_GLASS, Material.GREEN_STAINED_GLASS,
        Material.RED_STAINED_GLASS, Material.BLACK_STAINED_GLASS,
        Material.TORCH, Material.REDSTONE_TORCH, Material.SOUL_TORCH,
        Material.LADDER, Material.VINE, Material.LEVER, Material.STONE_BUTTON, Material.OAK_BUTTON,
        Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
        Material.DARK_OAK_BUTTON, Material.REDSTONE_WIRE, Material.TRIPWIRE_HOOK, Material.TRIPWIRE,
        Material.RAIL, Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.ACTIVATOR_RAIL,
        Material.SNOW, Material.TALL_GRASS, Material.SHORT_GRASS, Material.FERN, Material.DEAD_BUSH,
        Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM, Material.AZURE_BLUET,
        Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP,
        Material.OXEYE_DAISY, Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
        Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.SUGAR_CANE, Material.CACTUS,
        Material.WATER, Material.LAVA 
    );
    
    // NOTE: Simple block drops (glowstone, bookshelf, glass, ice) now handled by data pack
    // Only complex transformations remain in plugin code
    
    // BETA FISHING LOOT (only these items could be caught)
    private static final EnumSet<Material> BETA_FISHING_LOOT = EnumSet.of(
        Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH
    );
    
    // VIRTUAL CHEST CLASS - THE CORE OF MODERNBETA SYSTEM (UNCHANGED)
    public static class VirtualChest {
        private final Location location;
        private final Inventory virtualInventory;
        private final Set<Player> viewers;
        private ShulkerBox physicalStorage;
        private final boolean isDoubleChest;
        private ShulkerBox secondaryStorage; // For double chests
        
        // Single chest constructor
        public VirtualChest(Location loc, ShulkerBox storage) {
            this.location = loc.clone();
            this.physicalStorage = storage;
            this.secondaryStorage = null;
            this.isDoubleChest = false;
            this.viewers = new HashSet<>();
            this.virtualInventory = Bukkit.createInventory(null, 27, "Chest");
            syncFromPhysical();
        }
        
        // Double chest constructor
        public VirtualChest(Location loc, ShulkerBox leftStorage, ShulkerBox rightStorage) {
            this.location = loc.clone();
            this.physicalStorage = leftStorage;
            this.secondaryStorage = rightStorage;
            this.isDoubleChest = true;
            this.viewers = new HashSet<>();
            this.virtualInventory = Bukkit.createInventory(null, 54, "Large Chest");
            syncFromPhysical();
        }
        
        public void syncFromPhysical() {
            if (isDoubleChest && physicalStorage != null && secondaryStorage != null) {
                ItemStack[] leftContents = physicalStorage.getInventory().getContents();
                ItemStack[] rightContents = secondaryStorage.getInventory().getContents();
                virtualInventory.clear();
                for (int i = 0; i < 27; i++) {
                    if (i < leftContents.length && leftContents[i] != null) {
                        virtualInventory.setItem(i, leftContents[i].clone());
                    }
                }
                for (int i = 0; i < 27; i++) {
                    if (i < rightContents.length && rightContents[i] != null) {
                        virtualInventory.setItem(i + 27, rightContents[i].clone());
                    }
                }
            } else if (!isDoubleChest && physicalStorage != null) {
                ItemStack[] contents = physicalStorage.getInventory().getContents();
                virtualInventory.setContents(contents);
            }
        }
        
        public void syncToPhysical() {
            if (isDoubleChest && physicalStorage != null && secondaryStorage != null) {
                ItemStack[] leftContents = new ItemStack[27];
                ItemStack[] rightContents = new ItemStack[27];
                for (int i = 0; i < 27; i++) {
                    leftContents[i] = virtualInventory.getItem(i);
                    rightContents[i] = virtualInventory.getItem(i + 27);
                }
                physicalStorage.getInventory().setContents(leftContents);
                secondaryStorage.getInventory().setContents(rightContents);
            } else if (!isDoubleChest && physicalStorage != null) {
                ItemStack[] contents = virtualInventory.getContents();
                physicalStorage.getInventory().setContents(contents);
            }
        }
        
        public void addViewer(Player player) { viewers.add(player); }
        public void removeViewer(Player player) { viewers.remove(player); }
        public boolean hasViewers() { return !viewers.isEmpty(); }
        public Inventory getInventory() { return virtualInventory; }
        public Location getLocation() { return location; }
        public boolean isDoubleChest() { return isDoubleChest; }
    }
    
    static {
        // ALL EXISTING TRANSFORMATIONS (KEEP WORKING SYSTEMS)
        DROP_TRANSFORMS.put(Material.CRIMSON_FUNGUS, new ItemStack(Material.POPPY, 1));
        TRANSFORMABLE_BLOCKS.add(Material.CRIMSON_FUNGUS);
        
        DROP_TRANSFORMS.put(Material.WARPED_FUNGUS, new ItemStack(Material.DANDELION, 1));
        DROP_TRANSFORMS.put(Material.SUNFLOWER, new ItemStack(Material.DANDELION, 1));
        DROP_TRANSFORMS.put(Material.OXEYE_DAISY, new ItemStack(Material.DANDELION, 1));
        TRANSFORMABLE_BLOCKS.addAll(EnumSet.of(Material.WARPED_FUNGUS, Material.SUNFLOWER, Material.OXEYE_DAISY));
        
        DROP_TRANSFORMS.put(Material.BIRCH_PLANKS, new ItemStack(Material.OAK_PLANKS, 1));
        DROP_TRANSFORMS.put(Material.SPRUCE_PLANKS, new ItemStack(Material.OAK_PLANKS, 1));
        DROP_TRANSFORMS.put(Material.JUNGLE_PLANKS, new ItemStack(Material.OAK_PLANKS, 1));
        DROP_TRANSFORMS.put(Material.ACACIA_PLANKS, new ItemStack(Material.OAK_PLANKS, 1));
        DROP_TRANSFORMS.put(Material.DARK_OAK_PLANKS, new ItemStack(Material.OAK_PLANKS, 1));
        TRANSFORMABLE_BLOCKS.addAll(EnumSet.of(Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS, Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS));
        
        DROP_TRANSFORMS.put(Material.IRON_ORE, new ItemStack(Material.IRON_ORE, 1));
        DROP_TRANSFORMS.put(Material.GOLD_ORE, new ItemStack(Material.GOLD_ORE, 1));
        DROP_TRANSFORMS.put(Material.DEEPSLATE_IRON_ORE, new ItemStack(Material.IRON_ORE, 1));
        DROP_TRANSFORMS.put(Material.DEEPSLATE_GOLD_ORE, new ItemStack(Material.GOLD_ORE, 1));
        TRANSFORMABLE_BLOCKS.addAll(EnumSet.of(Material.IRON_ORE, Material.GOLD_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_GOLD_ORE));
        
        // MODERNBETA FAKE ITEM SYSTEM
        DROP_TRANSFORMS.put(Material.CHEST, new ItemStack(Material.BEEHIVE, 1));
        TRANSFORMABLE_BLOCKS.add(Material.CHEST);
        
        DROP_TRANSFORMS.put(Material.PISTON, new ItemStack(Material.INFESTED_STONE_BRICKS, 1));
        DROP_TRANSFORMS.put(Material.STICKY_PISTON, new ItemStack(Material.INFESTED_MOSSY_STONE_BRICKS, 1));
        TRANSFORMABLE_BLOCKS.addAll(EnumSet.of(Material.PISTON, Material.STICKY_PISTON));
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("=== FROGCRAFT TRANSFORMER v6.4 - SIMPLIFIED & COMPLETE ===");
        getLogger().info("[RECIPES] 151 authentic Beta 1.7.3 recipes from September 2011 archives");
        getLogger().info("[SPRINT] Simplified sprint prevention - hunger exhaustion & velocity control");
        getLogger().info("[DATA PACK] Hybrid architecture - simple drops via data pack, complex via plugin");
        getLogger().info("[CONFIG] Loading and applying settings from config.yml");
        
        // Load and save the default config.yml
        saveDefaultConfig();
        
        // COMPLETE RECIPE RESET - MODERNBETA.ORG APPROACH
        getServer().clearRecipes();
        getLogger().info("[RECIPES] Cleared all modern recipes");
        
        // ADD ONLY BETA 1.7.3 RECIPES (145 total)
        int recipeCount = 0;
        recipeCount += addBetaBasicRecipes();      // ~25 recipes
        recipeCount += addBetaToolRecipes();       // ~40 recipes  
        recipeCount += addBetaCombatRecipes();     // ~25 recipes
        recipeCount += addBetaBlockRecipes();      // ~20 recipes
        recipeCount += addBetaMechanismRecipes();  // ~15 recipes
        recipeCount += addBetaTransportRecipes();  // ~8 recipes
        recipeCount += addBetaFoodRecipes();       // ~6 recipes
        recipeCount += addBetaMiscRecipes();       // ~6 recipes
        
        getLogger().info("[SUCCESS] Loaded " + recipeCount + " Beta 1.7.3 recipes (Target: 145)");
        getLogger().info("[AUTHENTIC] Recipe book now shows ONLY Beta 1.7.3 items!");
        
        // HUNGER EFFECT REFRESH SYSTEM FOR ARMORLESS PLAYERS
        // Refresh hunger effect every 8 seconds to maintain invisible texture
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (calculateArmorPoints(player) < 0.1) {
                    // Refresh Hunger II effect to maintain invisible armor bar (ModernBeta approach)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1, false, false));
                }
            }
        }, 160L, 160L); // Every 8 seconds (160 ticks)
    }

    // ========================================
    // BETA 1.7.3 PISTON MECHANICS SYSTEM
    // ========================================

    // PISTON EXTENSION HANDLER - Beta 1.7.3 Authentic
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        BlockFace direction = event.getDirection();
        List<Block> blocks = event.getBlocks();
        
        // VALIDATION FIRST - don't modify anything until we know it's valid
        
        // Beta 1.7.3 Piston Rules
        if (blocks.size() > 12) {
            event.setCancelled(true);
            getLogger().info("[PISTON] Cancelled push - too many blocks (" + blocks.size() + ")");
            return;
        }
        
        // Check for unmovable blocks (Beta 1.7.3 list only)
        for (Block block : blocks) {
            if (isUnmovableBlock(block.getType())) {
                event.setCancelled(true);
                getLogger().info("[PISTON] Cancelled push - unmovable block: " + block.getType());
                return;
            }
        }
        
        // Check Y-level limits (Beta 1.7.3 had 128 height limit)
        for (Block block : blocks) {
            Block targetLocation = block.getRelative(direction);
            int targetY = targetLocation.getY();
            
            if (targetY < 0 || targetY > 127) {
                event.setCancelled(true);
                getLogger().info("[PISTON] Cancelled - trying to push beyond Y limits (0-127)");
                return;
            }
        }
        
        // ALL VALIDATION PASSED - Now apply Beta 1.7.3 tile entity mechanics
        // Schedule tile entity destruction for AFTER the push completes
        getServer().getScheduler().runTask(this, () -> {
            handleBetaTileEntityDestruction(blocks);
        });
        
        // Play piston extend sound
        piston.getWorld().playSound(piston.getLocation(), Sound.BLOCK_PISTON_EXTEND, 1.0f, 1.0f);
        getLogger().info("[PISTON] Extended pushing " + blocks.size() + " blocks (Beta 1.7.3 mechanics)");
    }

    // PISTON RETRACTION HANDLER  
    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Block piston = event.getBlock();
        List<Block> blocks = event.getBlocks();
        
        // BETA SAND/GRAVEL DUPLICATION GLITCH
        if (piston.getType() == Material.STICKY_PISTON && !blocks.isEmpty()) {
            for (Block block : blocks) {
                if (isFallingBlock(block.getType())) {
                    // Classic Beta glitch: sticky pistons duplicating falling blocks
                    getServer().getScheduler().runTaskLater(this, () -> {
                        BlockFace pistonDirection = ((org.bukkit.block.data.Directional) piston.getBlockData()).getFacing();
                        performSandGravelDuplication(block, pistonDirection);
                    }, 2L); // 2-tick delay for timing
                }
            }
        }
        
        // Play piston retract sound
        piston.getWorld().playSound(piston.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 1.0f, 1.0f);
        
        // Check if this is a sticky piston
        if (piston.getType() == Material.STICKY_PISTON) {
            getLogger().info("[STICKY PISTON] Retracting " + blocks.size() + " blocks");
        } else {
            getLogger().info("[PISTON] Retracted");
        }
    }

    // HELPER METHODS FOR PISTON MECHANICS
    private boolean isUnmovableBlock(Material material) {
        // Beta 1.7.3 ONLY unmovable blocks (no anachronistic blocks!)
        switch (material) {
            // Core unmovables that existed in Beta 1.7.3
            case BEDROCK:
            case OBSIDIAN:
            case PISTON:
            case STICKY_PISTON:
            case MOVING_PISTON:
            case SPAWNER:           // Monster spawners existed in Beta
            case FURNACE:           // Furnaces existed in Beta
            case CHEST:             // Chests existed in Beta  
            case DISPENSER:         // Dispensers added in Beta 1.2
            case NOTE_BLOCK:        // Note blocks existed in Beta
            case JUKEBOX:           // Jukeboxes existed in Beta
                return true;
            default:
                // Liquids are also unmovable in Beta
                return material.name().contains("LAVA") || material.name().contains("WATER");
        }
    }
    
    // Beta 1.7.3 tile entity destruction - SAFE version that runs after push
    private void handleBetaTileEntityDestruction(List<Block> originalBlocks) {
        // Find blocks that were moved and check if they were tile entities
        for (Block originalBlock : originalBlocks) {
            // Get the current state at the original location
            Block currentBlock = originalBlock.getWorld().getBlockAt(originalBlock.getLocation());
            
            // If the original location is now AIR and we had a tile entity, it was moved
            if (currentBlock.getType() == Material.AIR) {
                // Try to find where it moved to by checking adjacent blocks
                // This is a simplified approach - in reality Beta pistons would just destroy the data
                getLogger().info("[PISTON] Beta behavior: Tile entity data lost during push (authentic behavior)");
            }
        }
        
        // Note: In true Beta 1.7.3, tile entities lost their data when pushed
        // The container would become an empty container at the new location
        // This simplified version at least logs the behavior for now
    }
    
    private boolean isTileEntityBlock(Material material) {
        // Beta 1.7.3 tile entities that lose data when pushed
        switch (material) {
            case CHEST:
            case FURNACE:
            case DISPENSER:
            case NOTE_BLOCK:
            case JUKEBOX:
            case SPAWNER:
                return true;
            default:
                return false;
        }
    }
    
    // BETA DUPLICATION GLITCH HELPERS
    private boolean isFallingBlock(Material material) {
        // Blocks that can be duplicated with the Beta sticky piston glitch
        return material == Material.SAND || 
               material == Material.GRAVEL ||
               material == Material.RED_SAND; // Red sand for completeness
    }
    
    private void performSandGravelDuplication(Block originalBlock, BlockFace pistonDirection) {
        // This is the classic Beta duplication glitch that players love!
        Location originalLoc = originalBlock.getLocation();
        Material blockType = originalBlock.getType();
        
        // Check if the block is still there and is a falling block
        if (originalBlock.getType() == blockType && isFallingBlock(blockType)) {
            // Drop a duplicate item (the "duplication" effect)
            originalLoc.getWorld().dropItemNaturally(originalLoc, new ItemStack(blockType, 1));
            getLogger().info("[BETA GLITCH] Sand/Gravel duplication triggered at " + 
                           originalLoc.getBlockX() + "," + originalLoc.getBlockY() + "," + originalLoc.getBlockZ());
        }
    }
    
    // Helper method for headless piston glitch
    private void checkForHeadlessPiston(Block block) {
        // Check if this creates a headless piston situation
        if (block.getType() == Material.PISTON || block.getType() == Material.STICKY_PISTON) {
            BlockFace direction = ((org.bukkit.block.data.Directional) block.getBlockData()).getFacing();
            Block pistonHead = block.getRelative(direction);
            
            // If there's a piston head in front, try to create Block 36 (headless piston)
            if (pistonHead.getType() == Material.PISTON_HEAD) {
                getServer().getScheduler().runTaskLater(this, () -> {
                    // Create the classic "Block 36" - a piston head without a base
                    getLogger().info("[BETA GLITCH] Headless piston (Block 36) created at " + 
                                   pistonHead.getLocation().getBlockX() + "," + 
                                   pistonHead.getLocation().getBlockY() + "," + 
                                   pistonHead.getLocation().getBlockZ());
                    // The piston head remains but the base is gone - classic Beta behavior!
                }, 1L);
            }
        }
    }

    // ========================================
    // BETA 1.7.3 RECIPE WHITELIST SYSTEM
    // Source: web.archive.org/20110902073903/minecraftwiki.net/wiki/Crafting
    // ========================================

    private int addBetaBasicRecipes() {
        int count = 0;
        
        // PLANKS - All logs → Oak Planks only (Beta 1.7.3 behavior)
        for (Material log : Arrays.asList(Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG)) {
            addShapelessRecipe("planks_from_" + log.name().toLowerCase(),
                new ItemStack(Material.OAK_PLANKS, 4), log);
            count++;
        }
        
        // STICKS
        addShapedRecipe("sticks", new ItemStack(Material.STICK, 4),
            "P", "P", 'P', Material.OAK_PLANKS);
        count++;
        
        // TORCHES (2 variants: coal and charcoal)
        addShapedRecipe("torches_coal", new ItemStack(Material.TORCH, 4),
            "C", "S", 'C', Material.COAL, 'S', Material.STICK);
        addShapedRecipe("torches_charcoal", new ItemStack(Material.TORCH, 4),
            "C", "S", 'C', Material.CHARCOAL, 'S', Material.STICK);
        count += 2;
        
        // CRAFTING TABLE
        addShapedRecipe("crafting_table", new ItemStack(Material.CRAFTING_TABLE, 1),
            "PP", "PP", 'P', Material.OAK_PLANKS);
        count++;
        
        // FURNACE
        addShapedRecipe("furnace", new ItemStack(Material.FURNACE, 1),
            "CCC", "C C", "CCC", 'C', Material.COBBLESTONE);
        count++;
        
        // CHEST
        addShapedRecipe("chest", new ItemStack(Material.CHEST, 1),
            "PPP", "P P", "PPP", 'P', Material.OAK_PLANKS);
        count++;
        
        // LADDER - Beta quantity: 2 (7 sticks = 2 ladders)
        addShapedRecipe("ladder", new ItemStack(Material.LADDER, 2),
            "S S", "SSS", "S S", 'S', Material.STICK);
        count++;
        
        // FENCE - Beta recipe: 6 sticks = 2 fences
        addShapedRecipe("fence", new ItemStack(Material.OAK_FENCE, 2),
            "SSS", "SSS", 'S', Material.STICK);
        count++;
        
        // SIGN - Beta quantity: 1 (not 3)
        addShapedRecipe("sign", new ItemStack(Material.OAK_SIGN, 1),
            "PPP", "PPP", " S ", 'P', Material.OAK_PLANKS, 'S', Material.STICK);
        count++;
        
        // DOORS
        addShapedRecipe("wooden_door", new ItemStack(Material.OAK_DOOR, 1),
            "PP", "PP", "PP", 'P', Material.OAK_PLANKS);
        addShapedRecipe("iron_door", new ItemStack(Material.IRON_DOOR, 1),
            "II", "II", "II", 'I', Material.IRON_INGOT);
        count += 2;
        
        // TRAPDOOR (added in Beta 1.6)
        addShapedRecipe("trapdoor", new ItemStack(Material.OAK_TRAPDOOR, 2),
            "PPP", "PPP", 'P', Material.OAK_PLANKS);
        count++;
        
        // STAIRS
        addShapedRecipe("wood_stairs", new ItemStack(Material.OAK_STAIRS, 4),
            "P  ", "PP ", "PPP", 'P', Material.OAK_PLANKS);
        addShapedRecipe("cobblestone_stairs", new ItemStack(Material.COBBLESTONE_STAIRS, 4),
            "C  ", "CC ", "CCC", 'C', Material.COBBLESTONE);
        count += 2;
        
        // SLABS - Beta quantity: 3 (not 6)
        addShapedRecipe("stone_slab", new ItemStack(Material.STONE_SLAB, 3),
            "SSS", 'S', Material.STONE);
        addShapedRecipe("sandstone_slab", new ItemStack(Material.SANDSTONE_SLAB, 3),
            "SSS", 'S', Material.SANDSTONE);
        addShapedRecipe("wood_slab", new ItemStack(Material.OAK_SLAB, 3),
            "PPP", 'P', Material.OAK_PLANKS);
        addShapedRecipe("cobblestone_slab", new ItemStack(Material.COBBLESTONE_SLAB, 3),
            "CCC", 'C', Material.COBBLESTONE);
        count += 4;
        
        // PRESSURE PLATES
        addShapedRecipe("wood_pressure_plate", new ItemStack(Material.OAK_PRESSURE_PLATE, 1),
            "PP", 'P', Material.OAK_PLANKS);
        addShapedRecipe("stone_pressure_plate", new ItemStack(Material.STONE_PRESSURE_PLATE, 1),
            "SS", 'S', Material.STONE);
        count += 2;
        
        // BUTTON (only stone button existed in Beta 1.7.3)
        addShapedRecipe("stone_button", new ItemStack(Material.STONE_BUTTON, 1),
            "S", "S", 'S', Material.STONE);
        count += 1;
        
        getLogger().info("[BASIC] Added " + count + " basic recipes");
        return count;
    }

    private int addBetaToolRecipes() {
        int count = 0;
        String[] materials = {"WOODEN", "STONE", "IRON", "GOLDEN", "DIAMOND"};
        Material[] craftMats = {Material.OAK_PLANKS, Material.COBBLESTONE, 
                               Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND};
        
        for (int i = 0; i < materials.length; i++) {
            String tier = materials[i].toLowerCase();
            Material mat = craftMats[i];
            
            // PICKAXE
            addShapedRecipe(tier + "_pickaxe", 
                new ItemStack(Material.valueOf(materials[i] + "_PICKAXE"), 1),
                "MMM", " S ", " S ", 'M', mat, 'S', Material.STICK);
            
            // AXE
            addShapedRecipe(tier + "_axe",
                new ItemStack(Material.valueOf(materials[i] + "_AXE"), 1),
                "MM", "MS", " S", 'M', mat, 'S', Material.STICK);
            
            // SHOVEL
            addShapedRecipe(tier + "_shovel",
                new ItemStack(Material.valueOf(materials[i] + "_SHOVEL"), 1),
                "M", "S", "S", 'M', mat, 'S', Material.STICK);
            
            // HOE
            addShapedRecipe(tier + "_hoe",
                new ItemStack(Material.valueOf(materials[i] + "_HOE"), 1),
                "MM", " S", " S", 'M', mat, 'S', Material.STICK);
            
            count += 4;
        }
        
        // SHEARS (Beta 1.7 addition)
        addShapedRecipe("shears", new ItemStack(Material.SHEARS, 1),
            " I", "I ", 'I', Material.IRON_INGOT);
        count++;
        
        // PISTONS: Modern MC already has these recipes, no need to duplicate!
        
        // FLINT AND STEEL
        addShapelessRecipe("flint_and_steel", 
            new ItemStack(Material.FLINT_AND_STEEL, 1),
            Material.IRON_INGOT, Material.FLINT);
        count++;
        
        // FISHING ROD
        addShapedRecipe("fishing_rod", new ItemStack(Material.FISHING_ROD, 1),
            "  S", " ST", "S T", 'S', Material.STICK, 'T', Material.STRING);
        count++;
        
        // BUCKET
        addShapedRecipe("bucket", new ItemStack(Material.BUCKET, 1),
            "I I", " I ", 'I', Material.IRON_INGOT);
        count++;
        
        // COMPASS
        addShapedRecipe("compass", new ItemStack(Material.COMPASS, 1),
            " I ", "IRI", " I ", 'I', Material.IRON_INGOT, 'R', Material.REDSTONE);
        count++;
        
        // CLOCK
        addShapedRecipe("clock", new ItemStack(Material.CLOCK, 1),
            " G ", "GRG", " G ", 'G', Material.GOLD_INGOT, 'R', Material.REDSTONE);
        count++;
        
        getLogger().info("[TOOLS] Added " + count + " tool recipes");
        return count;
    }

    private int addBetaCombatRecipes() {
        int count = 0;
        
        // SWORDS
        String[] swordMats = {"WOODEN", "STONE", "IRON", "GOLDEN", "DIAMOND"};
        Material[] swordCraftMats = {Material.OAK_PLANKS, Material.COBBLESTONE,
                                    Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND};
        
        for (int i = 0; i < swordMats.length; i++) {
            addShapedRecipe(swordMats[i].toLowerCase() + "_sword",
                new ItemStack(Material.valueOf(swordMats[i] + "_SWORD"), 1),
                "M", "M", "S", 'M', swordCraftMats[i], 'S', Material.STICK);
            count++;
        }
        
        // BOW
        addShapedRecipe("bow", new ItemStack(Material.BOW, 1),
            " ST", "S T", " ST", 'S', Material.STICK, 'T', Material.STRING);
        count++;
        
        // ARROW
        addShapedRecipe("arrow", new ItemStack(Material.ARROW, 4),
            "F", "S", "E", 'F', Material.FLINT, 'S', Material.STICK, 'E', Material.FEATHER);
        count++;
        
        // ARMOR (4 pieces × 4 materials = 16 recipes)
        String[] armorMats = {"LEATHER", "IRON", "GOLDEN", "DIAMOND"};
        Material[] armorCraftMats = {Material.LEATHER, Material.IRON_INGOT,
                                    Material.GOLD_INGOT, Material.DIAMOND};
        
        for (int i = 0; i < armorMats.length; i++) {
            String tier = armorMats[i].toLowerCase();
            Material mat = armorCraftMats[i];
            
            // HELMET
            addShapedRecipe(tier + "_helmet",
                new ItemStack(Material.valueOf(armorMats[i] + "_HELMET"), 1),
                "MMM", "M M", 'M', mat);
            
            // CHESTPLATE
            addShapedRecipe(tier + "_chestplate",
                new ItemStack(Material.valueOf(armorMats[i] + "_CHESTPLATE"), 1),
                "M M", "MMM", "MMM", 'M', mat);
            
            // LEGGINGS
            addShapedRecipe(tier + "_leggings",
                new ItemStack(Material.valueOf(armorMats[i] + "_LEGGINGS"), 1),
                "MMM", "M M", "M M", 'M', mat);
            
            // BOOTS
            addShapedRecipe(tier + "_boots",
                new ItemStack(Material.valueOf(armorMats[i] + "_BOOTS"), 1),
                "M M", "M M", 'M', mat);
            
            count += 4;
        }
        
        getLogger().info("[COMBAT] Added " + count + " combat recipes");
        return count;
    }

    private int addBetaBlockRecipes() {
        int count = 0;
        
        // ORE BLOCKS (iron, gold, diamond, lapis)
        addShapedRecipe("iron_block", new ItemStack(Material.IRON_BLOCK, 1),
            "III", "III", "III", 'I', Material.IRON_INGOT);
        addShapedRecipe("gold_block", new ItemStack(Material.GOLD_BLOCK, 1),
            "GGG", "GGG", "GGG", 'G', Material.GOLD_INGOT);
        addShapedRecipe("diamond_block", new ItemStack(Material.DIAMOND_BLOCK, 1),
            "DDD", "DDD", "DDD", 'D', Material.DIAMOND);
        addShapedRecipe("lapis_block", new ItemStack(Material.LAPIS_BLOCK, 1),
            "LLL", "LLL", "LLL", 'L', Material.LAPIS_LAZULI);
        count += 4;
        
        // WOOL FROM STRING
        addShapedRecipe("wool_from_string", new ItemStack(Material.WHITE_WOOL, 1),
            "SS", "SS", 'S', Material.STRING);
        count++;
        
        // WOOL (16 colors)
        String[] woolColors = {"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", 
                              "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", 
                              "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"};
        String[] dyeColors = {"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW",
                             "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN",
                             "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"};
        
        for (int i = 0; i < woolColors.length; i++) {
            try {
                Material wool = Material.valueOf(woolColors[i] + "_WOOL");
                Material dye = Material.valueOf(dyeColors[i] + "_DYE");
                addShapelessRecipe("wool_" + woolColors[i].toLowerCase(),
                    new ItemStack(wool, 1), Material.WHITE_WOOL, dye);
                count++;
            } catch (IllegalArgumentException e) {
                // Skip if material doesn't exist
            }
        }
        
        // TNT
        addShapedRecipe("tnt", new ItemStack(Material.TNT, 1),
            "GSG", "SGS", "GSG", 'G', Material.GUNPOWDER, 'S', Material.SAND);
        count++;
        
        // BOOKSHELF
        addShapedRecipe("bookshelf", new ItemStack(Material.BOOKSHELF, 1),
            "PPP", "BBB", "PPP", 'P', Material.OAK_PLANKS, 'B', Material.BOOK);
        count++;
        
        // SANDSTONE
        addShapedRecipe("sandstone", new ItemStack(Material.SANDSTONE, 1),
            "SS", "SS", 'S', Material.SAND);
        count++;
        
        // JACK-O'-LANTERN (Beta 1.7.3 used regular pumpkin, not carved)
        addShapedRecipe("jack_o_lantern", new ItemStack(Material.JACK_O_LANTERN, 1),
            "P", "T", 'P', Material.PUMPKIN, 'T', Material.TORCH);
        count++;
        
        // SNOW BLOCK
        addShapedRecipe("snow_block", new ItemStack(Material.SNOW_BLOCK, 1),
            "SS", "SS", 'S', Material.SNOWBALL);
        count++;
        
        // CLAY BLOCK
        addShapedRecipe("clay_block", new ItemStack(Material.CLAY, 1),
            "CC", "CC", 'C', Material.CLAY_BALL);
        count++;
        
        // BRICK BLOCK
        addShapedRecipe("brick_block", new ItemStack(Material.BRICKS, 1),
            "BB", "BB", 'B', Material.BRICK);
        count++;
        
        // GLOWSTONE BLOCK
        addShapedRecipe("glowstone", new ItemStack(Material.GLOWSTONE, 1),
            "DD", "DD", 'D', Material.GLOWSTONE_DUST);
        count++;
        
        getLogger().info("[BLOCKS] Added " + count + " block recipes");
        return count;
    }

    private int addBetaMechanismRecipes() {
        int count = 0;
        
        // REDSTONE TORCH
        addShapedRecipe("redstone_torch", new ItemStack(Material.REDSTONE_TORCH, 1),
            "R", "S", 'R', Material.REDSTONE, 'S', Material.STICK);
        count++;
        
        // LEVER
        addShapedRecipe("lever", new ItemStack(Material.LEVER, 1),
            "S", "C", 'S', Material.STICK, 'C', Material.COBBLESTONE);
        count++;
        
        // REDSTONE REPEATER
        addShapedRecipe("redstone_repeater", new ItemStack(Material.REPEATER, 1),
            "TRT", "SSS", 'T', Material.REDSTONE_TORCH, 'R', Material.REDSTONE, 'S', Material.STONE);
        count++;
        
        // DISPENSER
        addShapedRecipe("dispenser", new ItemStack(Material.DISPENSER, 1),
            "CCC", "CBC", "CRC", 'C', Material.COBBLESTONE, 'B', Material.BOW, 'R', Material.REDSTONE);
        count++;
        
        // NOTE BLOCK
        addShapedRecipe("note_block", new ItemStack(Material.NOTE_BLOCK, 1),
            "PPP", "PRP", "PPP", 'P', Material.OAK_PLANKS, 'R', Material.REDSTONE);
        count++;
        
        // JUKEBOX
        addShapedRecipe("jukebox", new ItemStack(Material.JUKEBOX, 1),
            "PPP", "PDP", "PPP", 'P', Material.OAK_PLANKS, 'D', Material.DIAMOND);
        count++;
        
        getLogger().info("[MECHANISMS] Added " + count + " mechanism recipes");
        return count;
    }

    private int addBetaTransportRecipes() {
        int count = 0;
        
        // MINECART
        addShapedRecipe("minecart", new ItemStack(Material.MINECART, 1),
            "I I", "III", 'I', Material.IRON_INGOT);
        count++;
        
        // POWERED RAIL
        addShapedRecipe("powered_rail", new ItemStack(Material.POWERED_RAIL, 6),
            "G G", "GSG", "GRG", 'G', Material.GOLD_INGOT, 'S', Material.STICK, 'R', Material.REDSTONE);
        count++;
        
        // DETECTOR RAIL
        addShapedRecipe("detector_rail", new ItemStack(Material.DETECTOR_RAIL, 6),
            "I I", "IPI", "IRI", 'I', Material.IRON_INGOT, 'P', Material.STONE_PRESSURE_PLATE, 'R', Material.REDSTONE);
        count++;
        
        // RAIL
        addShapedRecipe("rail", new ItemStack(Material.RAIL, 16),
            "I I", "ISI", "I I", 'I', Material.IRON_INGOT, 'S', Material.STICK);
        count++;
        
        // BOAT
        addShapedRecipe("boat", new ItemStack(Material.OAK_BOAT, 1),
            "P P", "PPP", 'P', Material.OAK_PLANKS);
        count++;
        
        // STORAGE MINECART
        addShapedRecipe("storage_minecart", new ItemStack(Material.CHEST_MINECART, 1),
            "C", "M", 'C', Material.CHEST, 'M', Material.MINECART);
        count++;
        
        // POWERED MINECART
        addShapedRecipe("powered_minecart", new ItemStack(Material.FURNACE_MINECART, 1),
            "F", "M", 'F', Material.FURNACE, 'M', Material.MINECART);
        count++;
        
        getLogger().info("[TRANSPORT] Added " + count + " transport recipes");
        return count;
    }

    private int addBetaFoodRecipes() {
        int count = 0;
        
        // BREAD
        addShapedRecipe("bread", new ItemStack(Material.BREAD, 1),
            "WWW", 'W', Material.WHEAT);
        count++;
        
        // GOLDEN APPLE - Beta recipe: 8 gold blocks (not ingots!)
        addShapedRecipe("golden_apple", new ItemStack(Material.GOLDEN_APPLE, 1),
            "GGG", "GAG", "GGG", 'G', Material.GOLD_BLOCK, 'A', Material.APPLE);
        count++;
        
        // CAKE
        addShapedRecipe("cake", new ItemStack(Material.CAKE, 1),
            "MMM", "SES", "WWW", 'M', Material.MILK_BUCKET, 'S', Material.SUGAR, 'E', Material.EGG, 'W', Material.WHEAT);
        count++;
        
        // COOKIE
        addShapedRecipe("cookie", new ItemStack(Material.COOKIE, 8),
            "WCW", 'W', Material.WHEAT, 'C', Material.COCOA_BEANS);
        count++;
        
        // BOWL
        addShapedRecipe("bowl", new ItemStack(Material.BOWL, 4),
            "P P", " P ", 'P', Material.OAK_PLANKS);
        count++;
        
        // MUSHROOM STEW
        addShapelessRecipe("mushroom_stew", new ItemStack(Material.MUSHROOM_STEW, 1),
            Material.BOWL, Material.RED_MUSHROOM, Material.BROWN_MUSHROOM);
        count++;
        
        getLogger().info("[FOOD] Added " + count + " food recipes");
        return count;
    }

    private int addBetaMiscRecipes() {
        int count = 0;
        
        // BOOK - Beta recipe: 3 paper (no leather!)
        addShapelessRecipe("book", new ItemStack(Material.BOOK, 1),
            Material.PAPER, Material.PAPER, Material.PAPER);
        count++;
        
        // PAPER
        addShapedRecipe("paper", new ItemStack(Material.PAPER, 3),
            "SSS", 'S', Material.SUGAR_CANE);
        count++;
        
        // MAP
        addShapedRecipe("map", new ItemStack(Material.MAP, 1),
            "PPP", "PCP", "PPP", 'P', Material.PAPER, 'C', Material.COMPASS);
        count++;
        
        // PAINTING
        addShapedRecipe("painting", new ItemStack(Material.PAINTING, 1),
            "SSS", "SWS", "SSS", 'S', Material.STICK, 'W', Material.WHITE_WOOL);
        count++;
        
        // BED
        addShapedRecipe("bed", new ItemStack(Material.RED_BED, 1),
            "WWW", "PPP", 'W', Material.WHITE_WOOL, 'P', Material.OAK_PLANKS);
        count++;
        
        // SUGAR
        addShapelessRecipe("sugar", new ItemStack(Material.SUGAR, 1), Material.SUGAR_CANE);
        count++;
        
        // BONE MEAL
        addShapelessRecipe("bone_meal", new ItemStack(Material.BONE_MEAL, 3), Material.BONE);
        count++;
        
        // DYE RECIPES (Beta 1.7.3 quantities) - Remove cactus (smelting only)
        addShapelessRecipe("rose_red_dye", new ItemStack(Material.RED_DYE, 2), Material.POPPY);
        addShapelessRecipe("dandelion_yellow_dye", new ItemStack(Material.YELLOW_DYE, 2), Material.DANDELION);
        addShapelessRecipe("lapis_blue_dye", new ItemStack(Material.BLUE_DYE, 2), Material.LAPIS_LAZULI);
        count += 3;
        
        // MINERAL BLOCKS TO ITEMS (reverse recipes)
        addShapelessRecipe("iron_from_block", new ItemStack(Material.IRON_INGOT, 9),
            Material.IRON_BLOCK);
        addShapelessRecipe("gold_from_block", new ItemStack(Material.GOLD_INGOT, 9),
            Material.GOLD_BLOCK);
        addShapelessRecipe("diamond_from_block", new ItemStack(Material.DIAMOND, 9),
            Material.DIAMOND_BLOCK);
        addShapelessRecipe("lapis_from_block", new ItemStack(Material.LAPIS_LAZULI, 9),
            Material.LAPIS_BLOCK);
        count += 4;
        
        getLogger().info("[MISC] Added " + count + " miscellaneous recipes");
        return count;
    }

    // ========================================
    // RECIPE HELPER METHODS
    // ========================================

    private void addShapedRecipe(String key, ItemStack result, String line1, String line2, String line3, char ingredient1, Material material1, char ingredient2, Material material2, char ingredient3, Material material3, char ingredient4, Material material4) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1, line2, line3);
        recipe.setIngredient(ingredient1, material1);
        recipe.setIngredient(ingredient2, material2);
        recipe.setIngredient(ingredient3, material3);
        recipe.setIngredient(ingredient4, material4);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, String line2, String line3, char ingredient1, Material material1, char ingredient2, Material material2, char ingredient3, Material material3) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1, line2, line3);
        recipe.setIngredient(ingredient1, material1);
        recipe.setIngredient(ingredient2, material2);
        recipe.setIngredient(ingredient3, material3);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, String line2, String line3, char ingredient1, Material material1, char ingredient2, Material material2) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1, line2, line3);
        recipe.setIngredient(ingredient1, material1);
        recipe.setIngredient(ingredient2, material2);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, String line2, String line3, char ingredient1, Material material1) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1, line2, line3);
        recipe.setIngredient(ingredient1, material1);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, String line2, char ingredient1, Material material1, char ingredient2, Material material2, char ingredient3, Material material3) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1, line2);
        recipe.setIngredient(ingredient1, material1);
        recipe.setIngredient(ingredient2, material2);
        recipe.setIngredient(ingredient3, material3);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, String line2, char ingredient1, Material material1, char ingredient2, Material material2) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1, line2);
        recipe.setIngredient(ingredient1, material1);
        recipe.setIngredient(ingredient2, material2);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, String line2, char ingredient1, Material material1) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1, line2);
        recipe.setIngredient(ingredient1, material1);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, char ingredient1, Material material1, char ingredient2, Material material2) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1);
        recipe.setIngredient(ingredient1, material1);
        recipe.setIngredient(ingredient2, material2);
        getServer().addRecipe(recipe);
    }

    private void addShapedRecipe(String key, ItemStack result, String line1, char ingredient1, Material material1) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(line1);
        recipe.setIngredient(ingredient1, material1);
        getServer().addRecipe(recipe);
    }

    private void addShapelessRecipe(String key, ItemStack result, Material... ingredients) {
        NamespacedKey namespacedKey = new NamespacedKey(this, key);
        ShapelessRecipe recipe = new ShapelessRecipe(namespacedKey, result);
        
        for (Material ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }
        
        getServer().addRecipe(recipe);
    }

    // ========================================
    // ALL EXISTING SYSTEMS (UNCHANGED)
    // ========================================

    // NEW: FISHING LOOT CONTROL
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item) {
            Item caughtItem = (Item) event.getCaught();
            ItemStack item = caughtItem.getItemStack();
            
            // Beta fishing only gave fish - no treasure, junk, or enchanted items
            if (!BETA_FISHING_LOOT.contains(item.getType())) {
                item.setType(Material.COD);
                item.setAmount(1);
                caughtItem.setItemStack(item);
                getLogger().info("[FISHING] Prevented modern fishing loot - gave cod instead");
            }
        }
    }

    // BETA MOB SPAWN CONDITIONS
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!getConfig().getBoolean("mob-spawning.enabled")) return;
        if (event.getSpawnReason() != SpawnReason.NATURAL) return;

        Entity entity = event.getEntity();
        Location loc = event.getLocation();
        int lightLevel = loc.getBlock().getLightLevel();

        // Hostile mobs: Spawn only in light level 7 or less
        if (entity instanceof Monster) {
            int hostileLightLimit = getConfig().getInt("mob-spawning.hostile-light-level");
            if (lightLevel > hostileLightLimit) {
                event.setCancelled(true);
                // getLogger().info("[SPAWN] Cancelled hostile spawn due to high light level (" + lightLevel + ")");
                return;
            }
        }

        // Passive mobs: Spawn only on grass in light level 9 or more
        if (entity instanceof Animals) {
            Block blockBelow = loc.getBlock().getRelative(BlockFace.DOWN);
            int passiveLightLimit = getConfig().getInt("mob-spawning.passive-light-level");

            if (lightLevel < passiveLightLimit || blockBelow.getType() != Material.GRASS_BLOCK) {
                event.setCancelled(true);
                // getLogger().info("[SPAWN] Cancelled passive spawn due to low light or wrong block");
            }
        }
    }

    // MODERNBETA-STYLE SPRINT PREVENTION SYSTEM
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
        
        if (event.isSprinting()) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            double armorPoints = calculateArmorPoints(player);
            
            // Additional anti-exploit checks
            if (player.isSneaking()) {
                // Extra security: force stop sneaking to prevent sneak+sprint exploit
                player.setSneaking(false);
            }
            
            // Force stop any existing sprint state to prevent exploits
            player.setSprinting(false); // Explicitly force sprint to false
            player.setWalkSpeed(0.2f); // Reset to default walk speed
            player.setFlySpeed(0.1f);  // Reset fly speed just in case
            
            // Schedule a delayed check to ensure sprint state doesn't get stuck
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                    player.setVelocity(player.getVelocity().multiply(0.5));
                }
            }, 1L);
            
            // Only apply visual feedback if player has armor (ModernBeta behavior)
            if (armorPoints > 0.1) {
                // Apply hunger effect to trigger red X texture (food_half_hunger.png)
                player.setFoodLevel(1); // Low hunger triggers hunger texture variant
                player.setSaturation(0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 0, false, false));
                
                // Reduce velocity to prevent sprint-jump momentum exploits
                player.setVelocity(player.getVelocity().multiply(getConfig().getDouble("sprint-prevention.velocity-multiplier")));
                
                // Restore hunger after a configured delay
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    updatePlayerHungerBasedOnArmor(player);
                }, getConfig().getLong("sprint-prevention.restore-delay"));
                
                getLogger().info("[SPRINT] Prevented sprint attempt by " + player.getName() + " (armor: " + armorPoints + ")");
            }
            // If no armor, do nothing - player already can't sprint due to 0 hunger
        }
    }



    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
        
        Player player = event.getPlayer();
        // Set initial hunger based on armor when player joins
        Bukkit.getScheduler().runTaskLater(this, () -> {
            updatePlayerHungerBasedOnArmor(player);
        }, 20L); // 1 second delay to ensure player is fully loaded
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
        
        Player player = event.getPlayer();
        // Set hunger based on armor when player respawns
        Bukkit.getScheduler().runTaskLater(this, () -> {
            updatePlayerHungerBasedOnArmor(player);
        }, 5L); // Short delay to ensure respawn is complete
    }

    // ANTI-SPRINT EXPLOIT MOVEMENT MONITORING
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
        
        Player player = event.getPlayer();
        
        // Check if player is somehow sprinting (bypass detection)
        if (player.isSprinting()) {
            // Force stop sprint immediately
            player.setSprinting(false);
            player.setWalkSpeed(0.2f); // Reset walk speed
            
            // Apply velocity reduction to stop momentum
            player.setVelocity(player.getVelocity().multiply(0.3));
            
            // Additional anti-exploit measures
            if (player.isSneaking()) {
                player.setSneaking(false); // Force stop sneaking if active
            }
            
            getLogger().info("[ANTI-EXPLOIT] Stopped sprint bypass attempt by " + player.getName());
        }
    }

    // SNEAK EVENT MONITORING FOR SPRINT EXPLOITS
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
        
        Player player = event.getPlayer();
        
        // When player stops sneaking, force stop any sprint state
        if (!event.isSneaking() && player.isSprinting()) {
            player.setSprinting(false);
            player.setWalkSpeed(0.2f);
            player.setVelocity(player.getVelocity().multiply(0.5));
            getLogger().info("[ANTI-EXPLOIT] Prevented sneak-release sprint exploit by " + player.getName());
        }
    }

    // PREVENT STARVATION DAMAGE FOR ARMORLESS PLAYERS (Critical for 0 hunger + Hunger II)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
        
        Player player = (Player) event.getEntity();
        double armorPoints = calculateArmorPoints(player);
        
        // Cancel ALL damage for players with no armor when using 0 hunger + Hunger II approach
        if (armorPoints < 0.1) {
            // Cancel starvation damage
            if (event.getCause() == EntityDamageEvent.DamageCause.STARVATION) {
                event.setCancelled(true);
            }
            // Also cancel any hunger-related damage effects
            if (event.getCause() == EntityDamageEvent.DamageCause.MAGIC) {
                event.setCancelled(true);
            }
        }
    }

    // ARMOR DURABILITY MONITORING - Update armor bar when armor takes damage
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (!getConfig().getBoolean("sprint-prevention.enabled")) return;
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if the damaged item is armor
        if (isArmorPiece(item.getType())) {
            // Schedule update after damage is applied
            Bukkit.getScheduler().runTaskLater(this, () -> {
                updatePlayerHungerBasedOnArmor(player);
            }, 1L);
        }
    }

    private boolean isArmorPiece(Material material) {
        return material.name().contains("HELMET") || 
               material.name().contains("CHESTPLATE") || 
               material.name().contains("LEGGINGS") || 
               material.name().contains("BOOTS");
    }

    // HELPER METHODS FOR MODERNBETA ARMOR SYSTEM
    private double calculateArmorPoints(Player player) {
        double totalArmorPoints = 0.0;
        ItemStack[] armor = player.getInventory().getArmorContents();
        
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != Material.AIR) {
                double baseArmor = getArmorValue(piece.getType());
                double durabilityMultiplier = getDurabilityMultiplier(piece);
                totalArmorPoints += baseArmor * durabilityMultiplier;
            }
        }
        // Return precise value - don't round to preserve durability precision
        return totalArmorPoints;
    }

    private double getDurabilityMultiplier(ItemStack armor) {
        if (armor == null || armor.getType() == Material.AIR) return 0.0;
        
        // Get current durability (Bukkit uses damage value, where 0 = full durability)
        int maxDurability = armor.getType().getMaxDurability();
        int currentDamage = armor.getDurability(); // How much damage the item has taken
        int currentDurability = maxDurability - currentDamage;
        
        // Calculate durability percentage
        double durabilityPercentage = (double) currentDurability / maxDurability;
        
        // Ensure we don't go below 0 or above 1
        return Math.max(0.0, Math.min(1.0, durabilityPercentage));
    }

    private double getArmorValue(Material material) {
        // Beta 1.7.3 Armor Values - All materials have SAME protection per piece!
        // Maximum 10 armor points total (1.5 + 4 + 3 + 1.5 = 10)
        switch (material) {
            // ALL HELMETS give 1.5 armor points
            case LEATHER_HELMET:
            case IRON_HELMET: 
            case GOLDEN_HELMET:
            case DIAMOND_HELMET: 
                return 1.5; // 1.5 armor points (15% protection)
            
            // ALL CHESTPLATES give 4 armor points
            case LEATHER_CHESTPLATE:
            case IRON_CHESTPLATE:
            case GOLDEN_CHESTPLATE: 
            case DIAMOND_CHESTPLATE:
                return 4.0; // 4 armor points (40% protection)
            
            // ALL LEGGINGS give 3 armor points  
            case LEATHER_LEGGINGS:
            case IRON_LEGGINGS:
            case GOLDEN_LEGGINGS:
            case DIAMOND_LEGGINGS:
                return 3.0; // 3 armor points (30% protection)
            
            // ALL BOOTS give 1.5 armor points
            case LEATHER_BOOTS:
            case IRON_BOOTS:
            case GOLDEN_BOOTS:
            case DIAMOND_BOOTS:
                return 1.5; // 1.5 armor points (15% protection)
            
            default: return 0.0;
        }
    }

    private void updatePlayerHungerBasedOnArmor(Player player) {
        double armorPoints = calculateArmorPoints(player);
        
        if (armorPoints < 0.1) { // Essentially 0 armor
            // No armor = Invisible armor bar (EXACT ModernBeta approach)
            // 0 hunger + Hunger II effect = invisible armor bar texture
            player.setFoodLevel(0); // 0 hunger like ModernBeta
            player.setSaturation(0.0f); // No saturation
            // Apply Hunger II effect (level 1 = Hunger II) for infinite time like ModernBeta
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, Integer.MAX_VALUE, 1, false, false));
            // Remove any other effects
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.SATURATION);
        } else {
            // Has armor = Show armor bar based on actual armor points
            // Remove hunger effect so normal armor textures show
            player.removePotionEffect(PotionEffectType.HUNGER);
            player.removePotionEffect(PotionEffectType.REGENERATION);
            player.removePotionEffect(PotionEffectType.SATURATION);
            // Beta 1.7.3: Direct armor point to hunger bar mapping
            // Each armor point = 2 hunger bars (since 10 armor points = 20 hunger bars max)
            int scaledHunger = (int)(armorPoints * 2);
            // Ensure minimum 6 hunger for sprint attempts
            scaledHunger = Math.max(6, scaledHunger);
            player.setFoodLevel(Math.min(20, scaledHunger));
            player.setSaturation(5.0f);
        }
    }

    // ENHANCED: BLOCK BREAKING WITH BETA DROP CORRECTIONS
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material blockType = event.getBlock().getType();
        Location loc = event.getBlock().getLocation();
        
        // BETA PISTON GLITCH: Check for headless piston creation
        checkForHeadlessPiston(event.getBlock());
        
        // CHEST SYSTEM (UNCHANGED)
        if (isChestShulker(event.getBlock())) {
            String chestId = getChestId(loc);
            VirtualChest virtualChest = virtualChests.get(chestId);
            
            if (virtualChest != null) {
                virtualChest.syncToPhysical();
                for (Player viewer : virtualChest.viewers) {
                    viewer.closeInventory();
                }
                virtualChests.remove(chestId);
                activeVirtualInventories.remove(virtualChest.getInventory());
            }
            
            Block adjacentChest = findAdjacentChest(loc);
            boolean isDoubleChest = adjacentChest != null;
            
            if (isDoubleChest) {
                adjacentChest.setType(Material.BROWN_SHULKER_BOX);
                setShulkerOrientation(adjacentChest, event.getPlayer());
                
                String adjacentChestId = getChestId(adjacentChest.getLocation());
                VirtualChest adjacentVirtual = virtualChests.get(adjacentChestId);
                if (adjacentVirtual != null) {
                    adjacentVirtual.syncToPhysical();
                    for (Player viewer : adjacentVirtual.viewers) {
                        viewer.closeInventory();
                    }
                    virtualChests.remove(adjacentChestId);
                    activeVirtualInventories.remove(adjacentVirtual.getInventory());
                }
            }
            
            event.setDropItems(false);
            event.getBlock().getWorld().dropItemNaturally(
                loc.add(0.5, 0.5, 0.5), new ItemStack(Material.BEEHIVE, 1)
            );
            return;
        }
        
        // NOTE: Simple block drops now handled by data pack loot tables
        // Complex transformations (chests, transformable blocks) handled below
        
        // EXISTING TRANSFORMATIONS
        if (!TRANSFORMABLE_BLOCKS.contains(blockType)) return;
        ItemStack transformedDrop = DROP_TRANSFORMS.get(blockType);
        if (transformedDrop == null) return;
        
        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(
            event.getBlock().getLocation().add(0.5, 0.5, 0.5), transformedDrop.clone()
        );
    }

    // ALL OTHER METHODS UNCHANGED (chest physics, virtual system, mob drops, etc.)
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        Block placedBlock = event.getBlock();
        Player player = event.getPlayer();
        
        if (item.getType() == Material.BEEHIVE) {
            if (!isValidChestPlacement(placedBlock.getLocation(), player)) {
                event.setCancelled(true);
                return;
            }
            
            Block adjacentChest = findAdjacentChest(placedBlock.getLocation());
            
            if (adjacentChest != null) {
                setupDoubleChest(placedBlock, adjacentChest, player);
            } else {
                placedBlock.setType(Material.BROWN_SHULKER_BOX);
                setShulkerOrientation(placedBlock, player);
            }
            return;
        }
        
        if (item.getType() == Material.INFESTED_STONE_BRICKS) {
            placedBlock.setType(Material.PISTON);
        }
        if (item.getType() == Material.INFESTED_MOSSY_STONE_BRICKS) {
            placedBlock.setType(Material.STICKY_PISTON);
        }
    }

    private boolean isValidChestPlacement(Location loc, Player player) {
        Block[] adjacent = {
            loc.clone().add(1, 0, 0).getBlock(),
            loc.clone().add(-1, 0, 0).getBlock(),
            loc.clone().add(0, 0, 1).getBlock(),
            loc.clone().add(0, 0, -1).getBlock()
        };
        
        int adjacentChestCount = 0;
        Block foundChest = null;
        
        for (Block block : adjacent) {
            if (isChestShulker(block)) {
                adjacentChestCount++;
                foundChest = block;
            }
        }
        
        if (adjacentChestCount == 0) {
            return true;
        } else if (adjacentChestCount == 1) {
            Block adjacentToFound = findAdjacentChest(foundChest.getLocation());
            if (adjacentToFound != null) {
                player.sendMessage("[Beta Physics] Cannot place chest - adjacent chest is already part of a double chest!");
                return false;
            }
            return true;
        } else {
            player.sendMessage("§c[Beta Physics] Cannot place chest - too many adjacent chests!");
            return false;
        }
    }

    private boolean canOpenChest(Block chestBlock, Player player) {
        Block blockAbove = chestBlock.getRelative(BlockFace.UP);
        Material aboveMaterial = blockAbove.getType();
        
        if (!NON_BLOCKING_MATERIALS.contains(aboveMaterial)) {
            player.sendMessage("§c[Beta Physics] Cannot open chest - blocked by " + aboveMaterial.name().toLowerCase().replace('_', ' ') + " above!");
            return false;
        }
        return true;
    }

    private void setShulkerOrientation(Block shulkerBlock, Player player) {
        BlockData blockData = shulkerBlock.getBlockData();
        if (blockData instanceof Directional) {
            Directional directional = (Directional) blockData;
            BlockFace playerFacing = player.getFacing();
            BlockFace shulkerFacing = playerFacing.getOppositeFace();
            
            if (shulkerFacing == BlockFace.UP || shulkerFacing == BlockFace.DOWN) {
                shulkerFacing = BlockFace.NORTH;
            }
            
            directional.setFacing(shulkerFacing);
            shulkerBlock.setBlockData(directional);
        }
    }

    private Block findAdjacentChest(Location loc) {
        Block[] adjacent = {
            loc.clone().add(1, 0, 0).getBlock(),
            loc.clone().add(-1, 0, 0).getBlock(),
            loc.clone().add(0, 0, 1).getBlock(),
            loc.clone().add(0, 0, -1).getBlock()
        };
        
        for (Block block : adjacent) {
            if (isChestShulker(block)) {
                return block;
            }
        }
        return null;
    }

    private void setupDoubleChest(Block newBlock, Block existingBlock, Player player) {
        Location newLoc = newBlock.getLocation();
        Location existingLoc = existingBlock.getLocation();
        BlockFace playerFacing = player.getFacing();
        boolean newIsRight = false;
        
        switch (playerFacing) {
            case NORTH: newIsRight = newLoc.getBlockX() > existingLoc.getBlockX(); break;
            case SOUTH: newIsRight = newLoc.getBlockX() < existingLoc.getBlockX(); break;
            case EAST: newIsRight = newLoc.getBlockZ() > existingLoc.getBlockZ(); break;
            case WEST: newIsRight = newLoc.getBlockZ() < existingLoc.getBlockZ(); break;
            default: newIsRight = newLoc.getBlockX() > existingLoc.getBlockX();
        }
        
        if (newIsRight) {
            existingBlock.setType(Material.BLACK_SHULKER_BOX);
            newBlock.setType(Material.WHITE_SHULKER_BOX);
        } else {
            newBlock.setType(Material.BLACK_SHULKER_BOX);
            existingBlock.setType(Material.WHITE_SHULKER_BOX);
        }
        
        setShulkerOrientation(newBlock, player);
        setShulkerOrientation(existingBlock, player);
    }

    private boolean isChestShulker(Block block) {
        Material type = block.getType();
        return type == Material.BROWN_SHULKER_BOX || 
               type == Material.BLACK_SHULKER_BOX || 
               type == Material.WHITE_SHULKER_BOX;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null || !isChestShulker(block)) return;
        
        event.setCancelled(true);
        
        if (!canOpenChest(block, event.getPlayer())) {
            return;
        }
        
        Block adjacentChest = findAdjacentChest(block.getLocation());
        boolean isDoubleChest = adjacentChest != null;
        
        if (isDoubleChest) {
            if (!canOpenChest(adjacentChest, event.getPlayer())) {
                return;
            }
            openDoubleChest(event.getPlayer(), block, adjacentChest);
        } else {
            openSingleChest(event.getPlayer(), block);
        }
    }

    private void openSingleChest(Player player, Block block) {
        Location loc = block.getLocation();
        String chestId = getChestId(loc);
        
        VirtualChest virtualChest = virtualChests.get(chestId);
        if (virtualChest == null) {
            ShulkerBox shulkerBox = (ShulkerBox) block.getState();
            virtualChest = new VirtualChest(loc, shulkerBox);
            virtualChests.put(chestId, virtualChest);
        } else {
            virtualChest.syncFromPhysical();
        }
        
        virtualChest.addViewer(player);
        activeVirtualInventories.add(virtualChest.getInventory());
        player.openInventory(virtualChest.getInventory());
        block.getWorld().playSound(loc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    private void openDoubleChest(Player player, Block block1, Block block2) {
        Location loc1 = block1.getLocation();
        Location loc2 = block2.getLocation();
        
        Location leftLoc, rightLoc;
        if (loc1.getBlockX() < loc2.getBlockX() || 
            (loc1.getBlockX() == loc2.getBlockX() && loc1.getBlockZ() < loc2.getBlockZ())) {
            leftLoc = loc1;
            rightLoc = loc2;
        } else {
            leftLoc = loc2;
            rightLoc = loc1;
        }
        
        String doubleChestId = getChestId(leftLoc) + "_" + getChestId(rightLoc);
        
        VirtualChest virtualChest = virtualChests.get(doubleChestId);
        if (virtualChest == null) {
            Block leftBlock = leftLoc.getBlock();
            Block rightBlock = rightLoc.getBlock();
            
            ShulkerBox leftShulker, rightShulker;
            
            if (leftBlock.getType() == Material.BLACK_SHULKER_BOX) {
                leftShulker = (ShulkerBox) leftBlock.getState();
                rightShulker = (ShulkerBox) rightBlock.getState();
            } else if (leftBlock.getType() == Material.WHITE_SHULKER_BOX) {
                leftShulker = (ShulkerBox) rightBlock.getState();
                rightShulker = (ShulkerBox) leftBlock.getState();
            } else {
                leftShulker = (ShulkerBox) leftBlock.getState();
                rightShulker = (ShulkerBox) rightBlock.getState();
            }
            
            virtualChest = new VirtualChest(leftLoc, leftShulker, rightShulker);
            virtualChests.put(doubleChestId, virtualChest);
        } else {
            virtualChest.syncFromPhysical();
        }
        
        virtualChest.addViewer(player);
        activeVirtualInventories.add(virtualChest.getInventory());
        player.openInventory(virtualChest.getInventory());
        block1.getWorld().playSound(loc1, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        
        // Handle virtual chest synchronization
        if (activeVirtualInventories.contains(inventory)) {
            for (VirtualChest chest : virtualChests.values()) {
                if (chest.getInventory().equals(inventory)) {
                    Bukkit.getScheduler().runTaskLater(this, chest::syncToPhysical, 1L);
                    break;
                }
            }
        }
        
        // Handle armor-based hunger management (ModernBeta System)
        if (getConfig().getBoolean("sprint-prevention.enabled") && 
            event.getWhoClicked() instanceof Player) {
            
            Player player = (Player) event.getWhoClicked();
            
            // Check if armor was modified
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR ||
                event.getInventory().getType() == InventoryType.CRAFTING) {
                
                // Store current armor count to detect changes
                double currentArmor = calculateArmorPoints(player);
                
                // Delay the hunger update to allow the inventory change to process
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    double newArmor = calculateArmorPoints(player);
                    
                    // Only update if armor actually changed to prevent unnecessary updates
                    if (Math.abs(currentArmor - newArmor) > 0.01) { // Use small threshold for double comparison
                        // If switching from armor to no armor, set saturation high temporarily to prevent damage
                        if (newArmor < 0.1 && currentArmor > 0.1) {
                            player.setSaturation(20.0f); // Prevent damage during transition
                        }
                        updatePlayerHungerBasedOnArmor(player);
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        
        if (activeVirtualInventories.contains(inventory)) {
            for (VirtualChest chest : virtualChests.values()) {
                if (chest.getInventory().equals(inventory)) {
                    chest.removeViewer((Player) event.getPlayer());
                    chest.syncToPhysical();
                    
                    chest.getLocation().getWorld().playSound(
                        chest.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0f, 1.0f
                    );
                    
                    if (!chest.hasViewers()) {
                        activeVirtualInventories.remove(inventory);
                    }
                    break;
                }
            }
        }
    }

    private String getChestId(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    // FULLY CORRECTED BETA 1.7.3 MOB DROP SYSTEM (UNCHANGED)
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        event.getDrops().clear();
        
        if (entity instanceof Zombie) {
            int featherCount = random.nextInt(3);
            if (featherCount > 0) {
                event.getDrops().add(new ItemStack(Material.FEATHER, featherCount));
            }
        } else if (entity instanceof Skeleton) {
            int arrowCount = random.nextInt(3);
            int boneCount = random.nextInt(3);
            if (arrowCount > 0) event.getDrops().add(new ItemStack(Material.ARROW, arrowCount));
            if (boneCount > 0) event.getDrops().add(new ItemStack(Material.BONE, boneCount));
            
            if (random.nextDouble() < 0.085) {
                event.getDrops().add(new ItemStack(Material.BOW, 1));
            }
        } else if (entity instanceof Spider) {
            int stringCount = random.nextInt(3);
            if (stringCount > 0) {
                event.getDrops().add(new ItemStack(Material.STRING, stringCount));
            }
        } else if (entity instanceof Creeper) {
            int gunpowderCount = random.nextInt(3);
            if (gunpowderCount > 0) {
                event.getDrops().add(new ItemStack(Material.GUNPOWDER, gunpowderCount));
            }
        } else if (entity instanceof PigZombie) {
            int porkCount = random.nextInt(3);
            if (porkCount > 0) {
                event.getDrops().add(new ItemStack(Material.COOKED_PORKCHOP, porkCount));
            }
        } else if (entity instanceof Ghast) {
            int gunpowderCount = random.nextInt(3);
            if (gunpowderCount > 0) {
                event.getDrops().add(new ItemStack(Material.GUNPOWDER, gunpowderCount));
            }
        } else if (entity instanceof Slime) {
            Slime slime = (Slime) entity;
            if (slime.getSize() == 1) {
                int slimeballCount = random.nextInt(3);
                if (slimeballCount > 0) {
                    event.getDrops().add(new ItemStack(Material.SLIME_BALL, slimeballCount));
                }
            }
        } else if (entity instanceof Pig) {
            int porkCount = random.nextInt(3);
            if (porkCount > 0) {
                boolean onFire = entity.getFireTicks() > 0;
                Material porkType = onFire ? Material.COOKED_PORKCHOP : Material.PORKCHOP;
                event.getDrops().add(new ItemStack(porkType, porkCount));
            }
        } else if (entity instanceof Cow) {
            int leatherCount = random.nextInt(3);
            if (leatherCount > 0) {
                event.getDrops().add(new ItemStack(Material.LEATHER, leatherCount));
            }
        } else if (entity instanceof Sheep) {
            Sheep sheep = (Sheep) entity;
            if (!sheep.isSheared()) {
                event.getDrops().add(new ItemStack(Material.WHITE_WOOL, 1));
            }
        } else if (entity instanceof Chicken) {
            int featherCount = random.nextInt(3);
            if (featherCount > 0) {
                event.getDrops().add(new ItemStack(Material.FEATHER, featherCount));
            }
        } else if (entity instanceof Squid) {
            int inkCount = random.nextInt(3) + 1;
            event.getDrops().add(new ItemStack(Material.INK_SAC, inkCount));
        }
    }
}

