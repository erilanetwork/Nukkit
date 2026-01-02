package cn.nukkit.event.block;

import cn.nukkit.block.Block;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import lombok.Getter;

/**
 * @author EngincanErgunGG
 */
@Getter
public class BlockPhysicsBreakEvent extends BlockEvent implements Cancellable {

    @Getter
    private static final HandlerList handlers = new HandlerList();

    @Getter
    private boolean isRoot;

    private final int updateType;

    public BlockPhysicsBreakEvent(Block block, int updateType, boolean isRoot) {
        super(block);
        this.updateType = updateType;
        this.isRoot = isRoot;
    }
}