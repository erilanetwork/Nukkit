package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.block.BlockGrowEvent;
import cn.nukkit.event.block.BlockPhysicsBreakEvent;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.utils.BlockColor;

/**
 * @author Nukkit Project Team
 */
public class BlockCactus extends BlockTransparentMeta {

    public BlockCactus(int meta) {
        super(meta);
    }

    public BlockCactus() {
        this(0);
    }

    @Override
    public int getId() {
        return CACTUS;
    }

    @Override
    public double getHardness() {
        return 0.4;
    }

    @Override
    public double getResistance() {
        return 2;
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public double getMinX() {
        return this.x + 0.0625;
    }

    @Override
    public double getMinZ() {
        return this.z + 0.0625;
    }

    @Override
    public double getMaxX() {
        return this.x + 0.9375;
    }

    @Override
    public double getMaxZ() {
        return this.z + 0.9375;
    }

    @Override
    protected AxisAlignedBB recalculateCollisionBoundingBox() {
        return new SimpleAxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
    }

    @Override
    public double getMaxY() {
        return this.y + 0.9375;
    }

    @Override
    public void onEntityCollide(Entity entity) {
        entity.attack(new EntityDamageByBlockEvent(this, entity, DamageCause.CONTACT, 1));
    }

    @Override
    public int onUpdate(int type) {
        Block down = this.down();
        boolean isRoot = (down.getId() != CACTUS);

        if (type == Level.BLOCK_UPDATE_NORMAL) {
            boolean invalidDown = down.getId() != SAND && down.getId() != CACTUS;
            boolean invalidSide = false;

            for (BlockFace side : BlockFace.Plane.HORIZONTAL) {
                if (!getSide(side).canBeFlowedInto()) {
                    invalidSide = true;
                    break;
                }
            }

            if (invalidDown || invalidSide) {
                boolean rootBreak = (down.getId() != CACTUS);

                if (rootBreak) {
                    BlockPhysicsBreakEvent event = new BlockPhysicsBreakEvent(this, type, true);
                    Server.getInstance().getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return Level.BLOCK_UPDATE_NORMAL;
                    }
                }

                int yAbove = 0;
                while (true) {
                    Block target = this.getLevel().getBlock((int) this.x, (int) this.y + yAbove, (int) this.z);
                    if (target.getId() == CACTUS) {
                        this.getLevel().dropItem(
                                target.add(0.5, 0.5, 0.5),
                                Item.get(Block.CACTUS, 0, 1)
                        );
                        this.getLevel().setBlock((int) this.x, (int) this.y + yAbove, (int) this.z, BlockLayer.NORMAL, Block.get(AIR), false, false, true);
                        yAbove++;
                    } else {
                        break;
                    }
                }

                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (down.getId() == SAND || down.getId() == CACTUS) {
                if (this.getDamage() == 0x0F) {
                    int height = 1;

                    int yDown = (int) this.y - 1;
                    while (true) {
                        Block below = this.getLevel().getBlock((int) this.x, yDown, (int) this.z);
                        if (below.getId() == CACTUS) {
                            height++;
                            yDown--;
                        } else {
                            break;
                        }
                    }
                    int yUp = (int) this.y + 1;
                    while (true) {
                        Block above = this.getLevel().getBlock((int) this.x, yUp, (int) this.z);
                        if (above.getId() == CACTUS) {
                            height++;
                            yUp++;
                        } else {
                            break;
                        }
                    }

                    if (height < 3) {
                        Block growPos = this.getLevel().getBlock((int) this.x, yUp, (int) this.z);
                        if (growPos.getId() == AIR) {
                            BlockGrowEvent event = new BlockGrowEvent(growPos, Block.get(CACTUS));
                            Server.getInstance().getPluginManager().callEvent(event);
                            if (!event.isCancelled()) {
                                this.getLevel().setBlock(growPos, event.getNewState(), true, true);
                            }
                        }
                    }

                    this.setDamage(0);
                } else {
                    this.setDamage(this.getDamage() + 1);
                }

                this.level.setBlock((int) this.x, (int) this.y, (int) this.z,
                        BlockLayer.NORMAL, this, false, true, false);
            }
        }

        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        Block down = this.down();
        if (down.getId() == SAND || down.getId() == CACTUS) {
            Block block0 = north();
            Block block1 = south();
            Block block2 = west();
            Block block3 = east();
            if (block0.canBeFlowedInto() && block1.canBeFlowedInto() && block2.canBeFlowedInto() && block3.canBeFlowedInto()) {
                this.getLevel().setBlock(this, this, true, true);
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Cactus";
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public Item[] getDrops(Item item) {
        return new Item[]{
                Item.get(Item.CACTUS, 0, 1)
        };
    }

    @Override
    public WaterloggingType getWaterloggingType() {
        return WaterloggingType.WHEN_PLACED_IN_WATER;
    }

    @Override
    public boolean breakWhenPushed() {
        return true;
    }
}