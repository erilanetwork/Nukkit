package cn.nukkit.entity.item;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockLayer;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.entity.ItemDespawnEvent;
import cn.nukkit.event.entity.ItemSpawnEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.AddItemEntityPacket;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.EntityEventPacket;
import lombok.Getter;
import lombok.Setter;

/**
 * @author MagicDroidX
 */
public class EntityItem extends Entity {

    public static final int NETWORK_ID = 64;
    @Getter
    @Setter
    protected String owner;
    @Setter
    @Getter
    protected String thrower;
    @Getter
    protected Item item;
    @Getter
    @Setter
    protected int pickupDelay;
    private boolean mergeItems;
    protected boolean floatsInLava;
    public Player droppedBy;
    private int waterTicks;

    private boolean deadOnceAndForAll;

    public EntityItem(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);

        this.updateMode = 5;
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getLength() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }

    @Override
    public float getGravity() {
        return 0.04f;
    }

    @Override
    public float getDrag() {
        return 0.02f;
    }

    @Override
    protected float getBaseOffset() {
        return 0.125f;
    }

    @Override
    public boolean canCollide() {
        return false;
    }

    @Override
    protected void initEntity() {
        this.setMaxHealth(5);

        super.initEntity();

        if (namedTag.contains("Health")) {
            this.setHealth(namedTag.getShort("Health"));
        } else {
            this.setHealth(5);
        }

        if (this.namedTag.contains("Age")) {
            this.age = this.namedTag.getShort("Age");
        }

        if (this.namedTag.contains("PickupDelay")) {
            this.pickupDelay = this.namedTag.getShort("PickupDelay");
        }

        if (this.namedTag.contains("Owner")) {
            this.owner = this.namedTag.getString("Owner");
        }

        if (this.namedTag.contains("Thrower")) {
            this.thrower = this.namedTag.getString("Thrower");
        }

        if(this.namedTag.contains("Mergeable")) {
            this.mergeItems = this.namedTag.getBoolean("Mergeable");
        } else mergeItems = true;

        if (!this.namedTag.contains("Item")) {
            this.close();
            return;
        }

        try {
            this.item = NBTIO.getItemHelper(this.namedTag.getCompound("Item"));
        } catch (Exception ex) {
            server.getLogger().error("Item couldn't be loaded", ex);
            this.close();
            return;
        }

        int id = this.item.getId();
        if (id >= Item.NETHERITE_INGOT && id <= Item.NETHERITE_SCRAP) {
            this.fireProof = true; // Netherite items are fireproof
            this.floatsInLava = true;
        }

        this.server.getPluginManager().callEvent(new ItemSpawnEvent(this));
    }

    public boolean isLavaResistant() {
        return this.fireProof;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        DamageCause cause = source.getCause();
        if ((cause == DamageCause.VOID || cause == DamageCause.CONTACT || cause == DamageCause.FIRE_TICK
                || (cause == DamageCause.ENTITY_EXPLOSION || cause == DamageCause.BLOCK_EXPLOSION) && !this.isInsideOfWater()
                && (this.item == null || this.item.getId() != Item.NETHER_STAR)) && super.attack(source)) {
            if (this.item == null || this.isAlive() || this.deadOnceAndForAll) {
                return true;
            }
            this.deadOnceAndForAll = true;
            int id = this.item.getId();
            if (id != Item.SHULKER_BOX && id != Item.UNDYED_SHULKER_BOX) {
                return true;
            }
            CompoundTag nbt = this.item.getNamedTag();
            if (nbt == null) {
                return true;
            }
            ListTag<CompoundTag> items = nbt.getList("Items", CompoundTag.class);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.get(i);
                Item item = NBTIO.getItemHelper(itemTag);
                if (item.isNull()) {
                    continue;
                }
                this.level.dropItem(this, item);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;
        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }
        this.lastUpdate = currentTick;

        if (this.mergeItems && this.age % 60 == 0 && this.onGround && this.getItem() != null && this.isAlive()) {
            if (this.getItem().getCount() < this.getItem().getMaxStackSize()) {
                for (Entity entity : this.getLevel().getNearbyEntities(getBoundingBox().grow(1, 1, 1), this, false)) {
                    if (entity instanceof EntityItem) {
                        if (!entity.isAlive()) {
                            continue;
                        }
                        if (!((EntityItem) entity).mergeItems) continue;
                        Item closeItem = ((EntityItem) entity).getItem();
                        if (!closeItem.equals(getItem(), true, true)) {
                            continue;
                        }
                        if (!entity.isOnGround()) {
                            continue;
                        }
                        int newAmount = this.getItem().getCount() + closeItem.getCount();
                        if (newAmount > this.getItem().getMaxStackSize()) {
                            continue;
                        }
                        entity.close();
                        this.getItem().setCount(newAmount);
                        EntityEventPacket packet = new EntityEventPacket();
                        packet.eid = getId();
                        packet.data = newAmount;
                        packet.event = EntityEventPacket.MERGE_ITEMS;
                        Server.broadcastPacket(this.getViewers().values(), packet);
                    }
                }
            }
        }

        boolean hasUpdate = this.entityBaseTick(tickDiff);

        boolean lavaResistant = fireProof || item != null && this.isLavaResistant();

        if (!lavaResistant && (isInsideOfFire() || isInsideOfLava())) {
            this.kill();
        }

        if (this.isAlive()) {
            if (this.pickupDelay > 0 && this.pickupDelay < 32767) {
                this.pickupDelay -= tickDiff;
                if (this.pickupDelay < 0) {
                    this.pickupDelay = 0;
                }
            }

            int bid = this.level.getBlockIdAt((int) this.x, (int) this.boundingBox.getMaxY(), (int) this.z, BlockLayer.NORMAL);

            boolean inWaterBlock =
                    bid == BlockID.STILL_WATER || bid == BlockID.WATER
                            || (this.level.getBlockIdAt((int) this.x, (int) this.boundingBox.getMaxY(), (int) this.z, BlockLayer.WATERLOGGED) == BlockID.STILL_WATER
                            || this.level.getBlockIdAt((int) this.x, (int) this.boundingBox.getMaxY(), (int) this.z, BlockLayer.WATERLOGGED) == BlockID.WATER);

            boolean inLavaBlock =
                    this.level.getBlockIdAt((int) this.x, (int) this.boundingBox.getMaxY(), (int) this.z, BlockLayer.NORMAL) == BlockID.STILL_LAVA
                            || this.level.getBlockIdAt((int) this.x, (int) this.boundingBox.getMaxY(), (int) this.z, BlockLayer.NORMAL) == BlockID.LAVA
                            || this.level.getBlockIdAt((int) this.x, (int) this.boundingBox.getMaxY(), (int) this.z, BlockLayer.WATERLOGGED) == BlockID.STILL_LAVA
                            || this.level.getBlockIdAt((int) this.x, (int) this.boundingBox.getMaxY(), (int) this.z, BlockLayer.WATERLOGGED) == BlockID.LAVA;

            boolean inLiquid = inWaterBlock || (lavaResistant && inLavaBlock);

            if (inLiquid) {
                this.motionY = this.getGravity() - 0.06;
                applyLiquidFlow();
            } else if (this.isInsideOfWater() || (lavaResistant && this.isInsideOfLava())) {
                this.motionY = this.getGravity() - 0.06;
            } else {
                this.motionY -= this.getGravity();
            }

            if (this.checkObstruction(this.x, this.y, this.z)) {
                hasUpdate = true;
            }

            this.move(this.motionX, this.motionY, this.motionZ);

            if (!inLiquid) {
                double friction = 1 - this.getDrag();
                if (this.onGround && (Math.abs(this.motionX) > 0.00001 || Math.abs(this.motionZ) > 0.00001)) {
                    friction *= this.getLevel().getBlock(this.temporalVector.setComponents(
                            (int) Math.floor(this.x), (int) Math.floor(this.y - 1), (int) Math.floor(this.z)
                    )).getFrictionFactor();
                }
                this.motionX *= friction;
                this.motionY *= 1 - this.getDrag();
                this.motionZ *= friction;

                if (this.onGround) {
                    this.motionY *= -0.5;
                }
            }

            if (inWaterBlock) {
                if (this.waterTicks < 1000) this.waterTicks++;
            } else {
                this.waterTicks = 0;
            }

            this.updateMovement();

            if (this.age > 6000) {
                ItemDespawnEvent ev = new ItemDespawnEvent(this);
                this.server.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    this.age = 0;
                    respawnToAll();
                } else {
                    this.kill();
                    hasUpdate = true;
                }
            }
        }

        return hasUpdate || !this.onGround
                || Math.abs(this.motionX) > 0.00001
                || Math.abs(this.motionY) > 0.00001
                || Math.abs(this.motionZ) > 0.00001;
    }

    private void applyLiquidFlow() {
        Block liquidBlock = this.level.getBlock((int) this.x, (int) Math.floor(this.y), (int) this.z);
        if (!(liquidBlock instanceof BlockLiquid)) {
            return;
        }

        Vector3 flow = ((BlockLiquid) liquidBlock).getFlowVector();
        double len = Math.sqrt(flow.x * flow.x + flow.z * flow.z);
        if (len > 0) {
            flow = new Vector3(flow.x / len, 0, flow.z / len);
        }

        double accelWater = 0.0015;
        double accelIce = 0.041;
        double capWaterXZ = 0.035;
        double capIceXZ = 0.22;
        double dragXZ = 0.92;
        double dragY = 0.90;

        Block under = this.level.getBlock((int) this.x, (int) Math.floor(this.y) - 1, (int) this.z);
        boolean overIce = under.getId() == BlockID.ICE
                || under.getId() == BlockID.FROSTED_ICE
                || under.getId() == BlockID.PACKED_ICE
                || under.getId() == BlockID.BLUE_ICE;

        double accel = overIce ? accelIce : accelWater;
        this.motionX += flow.x * accel;
        this.motionZ += flow.z * accel;

        this.motionY -= this.getGravity() * 0.20;
        int surfaceY = (int) Math.floor(this.boundingBox.getMaxY());
        int scanY = surfaceY;
        for (int i = 0; i < 3; i++) {
            int idUp = this.level.getBlockIdAt((int) this.x, scanY + 1, (int) this.z, BlockLayer.NORMAL);
            int idUpWL = this.level.getBlockIdAt((int) this.x, scanY + 1, (int) this.z, BlockLayer.WATERLOGGED);
            boolean isWaterUp = idUp == BlockID.STILL_WATER || idUp == BlockID.WATER
                    || idUpWL == BlockID.STILL_WATER || idUpWL == BlockID.WATER;
            if (!isWaterUp) {
                surfaceY = scanY + 1;
                break;
            }
            scanY++;
        }

        double topY = this.boundingBox.getMaxY();
        double depth = surfaceY - topY;
        double targetDepth = 0.35;
        double hysteresis = 0.10;
        double k = 0.20;
        double buoyBase = 0.010;
        double buoyMaxExtra = 0.030;

        if (this.waterTicks < 15) {
            this.motionY -= 0.005;
        }

        double force = k * (depth - targetDepth);
        if (depth > targetDepth + hysteresis) {
            double buoyancy = buoyBase + Math.min((depth - targetDepth) * 0.06, buoyMaxExtra);
            this.motionY += force + buoyancy;
            this.motionY *= 0.92;
        } else if (depth < targetDepth - hysteresis) {
            this.motionY += force - 0.006;
            this.motionY *= 0.92;
        } else {
            this.motionY += force * 0.5 + buoyBase * 0.5;
            this.motionY *= 0.94;
        }

        this.motionX *= dragXZ;
        this.motionZ *= dragXZ;
        this.motionY *= dragY;

        double capXZ = overIce ? capIceXZ : capWaterXZ;
        if (this.motionX > capXZ) this.motionX = capXZ;
        if (this.motionX < -capXZ) this.motionX = -capXZ;
        if (this.motionZ > capXZ) this.motionZ = capXZ;
        if (this.motionZ < -capXZ) this.motionZ = -capXZ;

        if (this.motionY > 0.06) this.motionY = 0.06;
        if (this.motionY < -0.08) this.motionY = -0.08;
    }


    @Override
    public void saveNBT() {
        super.saveNBT();
        if (this.item != null) { // Yes, a item can be null... I don't know what causes this, but it can happen.
            this.namedTag.putCompound("Item", NBTIO.putItemHelper(this.item, -1));
            this.namedTag.putShort("Health", (int) this.getHealth());
            this.namedTag.putShort("Age", this.age);
            this.namedTag.putShort("PickupDelay", this.pickupDelay);
            if (this.owner != null) {
                this.namedTag.putString("Owner", this.owner);
            }

            if (this.thrower != null) {
                this.namedTag.putString("Thrower", this.thrower);
            }
        }
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.getNameTag() : (this.item.hasCustomName() ? this.item.getCustomName() : this.item.getName());
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public DataPacket createAddEntityPacket() {
        AddItemEntityPacket addEntity = new AddItemEntityPacket();
        addEntity.entityUniqueId = this.getId();
        addEntity.entityRuntimeId = this.getId();
        addEntity.x = (float) this.x;
        addEntity.y = (float) this.y + this.getBaseOffset();
        addEntity.z = (float) this.z;
        addEntity.speedX = (float) this.motionX;
        addEntity.speedY = (float) this.motionY;
        addEntity.speedZ = (float) this.motionZ;
        addEntity.metadata = this.dataProperties.clone();
        addEntity.item = this.item;
        return addEntity;
    }
}
