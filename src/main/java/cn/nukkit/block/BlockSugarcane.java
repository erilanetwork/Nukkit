package cn.nukkit.block;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.block.BlockGrowEvent;
import cn.nukkit.event.block.BlockPhysicsBreakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemDye;
import cn.nukkit.level.Level;
import cn.nukkit.level.particle.BoneMealParticle;
import cn.nukkit.math.BlockFace;
import cn.nukkit.utils.BlockColor;

/**
 * Created by Pub4Game on 09.01.2016.
 */
public class BlockSugarcane extends BlockFlowable {

    public BlockSugarcane() {
        this(0);
    }

    public BlockSugarcane(int meta) {
        super(meta);
    }

    @Override
    public String getName() {
        return "Sugar Cane";
    }

    @Override
    public int getId() {
        return SUGARCANE_BLOCK;
    }

    @Override
    public Item toItem() {
        return Item.get(Item.SUGARCANE);
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Item item, Player player) {
        if (item.getId() == Item.DYE && item.getDamage() == ItemDye.BONE_MEAL) {
            int count = 1;

            for (int i = 1; i <= 2; i++) {
                int id = this.level.getBlockIdAt(this.getFloorX(), this.getFloorY() - i, this.getFloorZ());

                if (id == SUGARCANE_BLOCK) {
                    count++;
                }
            }

            if (count < 3) {
                boolean success = false;
                int toGrow = 3 - count;

                for (int i = 1; i <= toGrow; i++) {
                    Block block = this.up(i);
                    if (block.getId() == 0) {
                        BlockGrowEvent ev = new BlockGrowEvent(block, Block.get(SUGARCANE_BLOCK));
                        Server.getInstance().getPluginManager().callEvent(ev);

                        if (!ev.isCancelled()) {
                            this.getLevel().setBlock(block, ev.getNewState(), true);
                            success = true;
                        }
                    } else if (block.getId() != SUGARCANE_BLOCK) {
                        break;
                    }
                }

                if (success) {
                    if (player != null && !player.isCreative()) {
                        item.count--;
                    }

                    this.level.addParticle(new BoneMealParticle(this));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int onUpdate(int type) {
        Block down = this.down();
        boolean isRoot = (down.getId() != SUGARCANE_BLOCK);
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (isRoot && down.isTransparent()) {
                BlockPhysicsBreakEvent event = new BlockPhysicsBreakEvent(this, type, true);
                Server.getInstance().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return Level.BLOCK_UPDATE_NORMAL;
                }

                int yAbove = 0;
                while (true) {
                    Block target = this.getLevel().getBlock((int) this.x, (int) this.y + yAbove, (int) this.z);
                    if (target.getId() == SUGARCANE_BLOCK) {
                        this.getLevel().dropItem(
                                target.add(0.5, 0.5, 0.5),
                                Item.get(Item.SUGARCANE, 0, 1)
                        );
                        this.getLevel().setBlock(
                                (int) this.x, (int) this.y + yAbove, (int) this.z,
                                BlockLayer.NORMAL,
                                Block.get(AIR),
                                false, false, false
                        );
                        yAbove++;
                    } else {
                        break;
                    }
                }
                return Level.BLOCK_UPDATE_NORMAL;
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (isRoot) {
                if (this.getDamage() == 0x0F) {
                    for (int y = 1; y < 3; ++y) {
                        Block b = this.getLevel().getBlock((int) this.x, (int) this.y + y, (int) this.z);
                        if (b.getId() == AIR) {
                            BlockGrowEvent ev = new BlockGrowEvent(b, Block.get(SUGARCANE_BLOCK));
                            Server.getInstance().getPluginManager().callEvent(ev);
                            if (!ev.isCancelled()) {
                                this.getLevel().setBlock(b, ev.getNewState(), true, true);
                            }
                            break;
                        }
                    }
                    this.setDamage(0);
                } else {
                    this.setDamage(this.getDamage() + 1);
                }
                this.level.setBlock((int) this.x, (int) this.y, (int) this.z,
                        BlockLayer.NORMAL, this, false, true, false);
                return Level.BLOCK_UPDATE_RANDOM;
            }
        }
        return 0;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, double fx, double fy, double fz, Player player) {
        if (block.getId() != AIR) {
            return false;
        }
        Block down = this.down();
        int id = down.getId();
        if (id == SUGARCANE_BLOCK) {
            this.getLevel().setBlock(block, Block.get(SUGARCANE_BLOCK), true);
            return true;
        } else if (id == GRASS || id == DIRT || id == SAND || id == PODZOL || id == MYCELIUM) {
            Block block0 = down.north();
            Block block1 = down.south();
            Block block2 = down.west();
            Block block3 = down.east();
            if (block0 instanceof BlockWater || block1 instanceof BlockWater || block2 instanceof BlockWater || block3 instanceof BlockWater || block0 instanceof BlockIceFrosted || block1 instanceof BlockIceFrosted || block2 instanceof BlockIceFrosted || block3 instanceof BlockIceFrosted) {
                this.getLevel().setBlock(block, Block.get(SUGARCANE_BLOCK), true);
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public boolean breakWhenPushed() {
        return true;
    }
}
