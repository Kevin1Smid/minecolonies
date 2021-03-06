package com.minecolonies.coremod.items;

import com.ldtteam.structurize.placement.handlers.placement.PlacementError;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.LanguageHandler;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.creativetab.ModCreativeTabs;
import com.minecolonies.coremod.MineColonies;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.CANT_PLACE_COLONY_IN_OTHER_DIM;

/**
 * Class to handle the placement of the supplychest and with it the supplyship.
 */
public class ItemSupplyChestDeployer extends AbstractItemMinecolonies
{
    /**
     * StructureIterator name and location.
     */
    private static final String SUPPLY_SHIP_STRUCTURE_NAME = "supplyship";

    /**
     * Offset south/west of the supply chest.
     */
    private static final int OFFSET_DISTANCE = 14;

    /**
     * Offset south/east of the supply chest.
     */
    private static final int OFFSET_LEFT = 5;

    /**
     * Offset y of the supply chest.
     */
    private static final int OFFSET_Y = -2;

    /**
     * Height to scan in which should be air.
     */
    private static final int SCAN_HEIGHT = 7;

    /**
     * Creates a new supplychest deployer. The item is not stackable.
     *
     * @param properties the properties.
     */
    public ItemSupplyChestDeployer(final Item.Properties properties)
    {
        super("supplychestdeployer", properties.maxStackSize(1).group(ModCreativeTabs.MINECOLONIES));
    }

    @NotNull
    @Override
    public ActionResultType onItemUse(final ItemUseContext ctx)
    {
        if (ctx.getWorld().isRemote)
        {
            if (!MineColonies.getConfig().getCommon().allowOtherDimColonies.get() && ctx.getWorld().getDimension().getType().getId() != 0)
            {
                return ActionResultType.FAIL;
            }
            placeSupplyShip(ctx.getPos(), ctx.getPlayer().getHorizontalFacing());
        }

        return ActionResultType.FAIL;
    }

    @NotNull
    @Override
    public ActionResult<ItemStack> onItemRightClick(final World worldIn, final PlayerEntity playerIn, final Hand hand)
    {
        final ItemStack stack = playerIn.getHeldItem(hand);
        if (worldIn.isRemote)
        {
            if (!MineColonies.getConfig().getCommon().allowOtherDimColonies.get() && worldIn.getDimension().getType().getId() != 0)
            {
                LanguageHandler.sendPlayerMessage(playerIn, CANT_PLACE_COLONY_IN_OTHER_DIM);
                return new ActionResult<>(ActionResultType.FAIL, stack);
            }
            placeSupplyShip(null, playerIn.getHorizontalFacing());
        }

        return new ActionResult<>(ActionResultType.FAIL, stack);
    }

    /**
     * Places a supply chest on the given position looking to the given direction.
     *
     * @param pos       the position to place the supply chest at.
     * @param direction the direction the supply chest should face.
     */
    private void placeSupplyShip(@Nullable final BlockPos pos, @NotNull final Direction direction)
    {
        if (pos == null)
        {
            MineColonies.proxy.openBuildToolWindow(null, SUPPLY_SHIP_STRUCTURE_NAME, 0);
            return;
        }

        final BlockPos tempPos;
        final int rotations;
        switch (direction)
        {
            case SOUTH:
                tempPos = pos.add(OFFSET_LEFT, OFFSET_Y, OFFSET_DISTANCE);
                rotations = ROTATE_THREE_TIMES;
                break;
            case NORTH:
                tempPos = pos.add(-OFFSET_LEFT, OFFSET_Y, -OFFSET_DISTANCE);
                rotations = ROTATE_ONCE;
                break;
            case EAST:
                tempPos = pos.add(OFFSET_DISTANCE, OFFSET_Y, -OFFSET_LEFT);
                rotations = ROTATE_TWICE;
                break;
            default:
                tempPos = pos.add(-OFFSET_DISTANCE, OFFSET_Y, OFFSET_LEFT);
                rotations = ROTATE_0_TIMES;
                break;
        }
        MineColonies.proxy.openBuildToolWindow(tempPos, SUPPLY_SHIP_STRUCTURE_NAME, rotations);
    }

    /**
     * Checks if the ship can be placed.
     *
     * @param world              the world.
     * @param pos                the pos.
     * @param size               the size.
     * @param placementErrorList the list of placement errors.
     * @param placer             the placer.
     * @return true if so.
     */
    public static boolean canShipBePlaced(
      @NotNull final World world, @NotNull final BlockPos pos, final BlockPos size, @NotNull final List<PlacementError> placementErrorList, final
    PlayerEntity placer)
    {
        for (int z = pos.getZ() - size.getZ() / 2 + 1; z < pos.getZ() + size.getZ() / 2 + 1; z++)
        {
            for (int x = pos.getX() - size.getX() / 2 + 1; x < pos.getX() + size.getX() / 2 + 1; x++)
            {
                checkIfWaterAndNotInColony(world, new BlockPos(x, pos.getY() + 2, z), placementErrorList, placer);
            }
        }

        for (int z = pos.getZ() - size.getZ() / 2 + 1; z < pos.getZ() + size.getZ() / 2 + 1; z++)
        {
            for (int x = pos.getX() - size.getX() / 2 + 1; x < pos.getX() + size.getX() / 2 + 1; x++)
            {
                if (!world.isAirBlock(new BlockPos(x, pos.getY() + SCAN_HEIGHT, z)))
                {
                    final PlacementError placementError = new PlacementError(PlacementError.PlacementErrorType.NEEDS_AIR_ABOVE, new BlockPos(x, pos.getY() + SCAN_HEIGHT, z));
                    placementErrorList.add(placementError);
                }
            }
        }
        return placementErrorList.isEmpty();
    }

    /**
     * Check if the there is water at one of three positions.
     *
     * @param world              the world.
     * @param pos                the first position.
     * @param placementErrorList a list of placement errors.
     * @param placer             the player placing the supply camp.
     */
    private static void checkIfWaterAndNotInColony(final World world, final BlockPos pos, @NotNull final List<PlacementError> placementErrorList, final PlayerEntity placer)
    {
        final boolean isWater = BlockUtils.isWater(world.getBlockState(pos));
        final boolean notInAnyColony = hasPlacePermission(world, pos, placer);
        if (!isWater)
        {
            final PlacementError placementError = new PlacementError(PlacementError.PlacementErrorType.NOT_WATER, pos);
            placementErrorList.add(placementError);
        }
        if (!notInAnyColony)
        {
            final PlacementError placementError = new PlacementError(PlacementError.PlacementErrorType.INSIDE_COLONY, pos);
            placementErrorList.add(placementError);
        }
    }

    /**
     * Check if any of the coordinates is in any colony.
     *
     * @param world  the world to check in.
     * @param pos    the first position.
     * @param placer the placer.
     * @return true if no colony found.
     */
    private static boolean hasPlacePermission(final World world, final BlockPos pos, final PlayerEntity placer)
    {
        final IColony colony = IColonyManager.getInstance().getColonyByPosFromWorld(world, pos);
        return colony == null || colony.getPermissions().hasPermission(placer, Action.PLACE_BLOCKS);
    }
}
