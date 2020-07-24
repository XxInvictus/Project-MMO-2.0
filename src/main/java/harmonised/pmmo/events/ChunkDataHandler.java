package harmonised.pmmo.events;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.event.world.ChunkDataEvent;

import java.util.*;

public class ChunkDataHandler
{
    private static Map<ResourceLocation, Map<ChunkPos, Map<BlockPos, UUID>>> placedMap;

    public static void init()
    {
        placedMap = new HashMap<>();
    }

    public static void handleChunkDataLoad( ChunkDataEvent.Load event )
    {
        CompoundNBT levelNBT = event.getData();

        if( levelNBT != null )
        {
            if( levelNBT.contains( "placedPos" ) )
            {
                ResourceLocation dimResLoc = event.getWorld().getDimension().getType().getRegistryName();
                ChunkPos chunkPos = event.getChunk().getPos();

                if( !placedMap.containsKey( dimResLoc ) )
                    placedMap.put( dimResLoc, new HashMap<>() );

                CompoundNBT placedPosNBT = ( (CompoundNBT) levelNBT.get( "placedPos" ) );
                if( placedPosNBT == null )
                    return;
                Map<ChunkPos, Map<BlockPos, UUID>> chunkMap = placedMap.get( dimResLoc );
                Map<BlockPos, UUID> blockMap = new HashMap<>();
                Set<String> keySet = placedPosNBT.keySet();

                keySet.forEach( key ->
                {
                    CompoundNBT entry = placedPosNBT.getCompound( key );
                    blockMap.put( NBTUtil.readBlockPos( entry.getCompound( "pos" ) ), NBTUtil.readUniqueId( entry.getCompound( "UUID" ) ) );
                });

                chunkMap.remove( chunkPos );
                chunkMap.put( chunkPos, blockMap );
            }
        }
    }

    public static void handleChunkDataSave( ChunkDataEvent.Save event )
    {
        ResourceLocation dimResLoc = event.getWorld().getDimension().getType().getRegistryName();
        if( placedMap.containsKey( dimResLoc ) )
        {
            ChunkPos chunkPos = event.getChunk().getPos();
            if( placedMap.get( dimResLoc ).containsKey( chunkPos ) )
            {
                CompoundNBT levelNBT = (CompoundNBT) event.getData().get( "Level" );
                if( levelNBT == null )
                    return;

                CompoundNBT newPlacedNBT = new CompoundNBT();
                CompoundNBT insidesNBT;

                int i = 0;

                for( Map.Entry<BlockPos, UUID> entry : placedMap.get( dimResLoc ).get( chunkPos ).entrySet() )
                {
                    insidesNBT = new CompoundNBT();
                    insidesNBT.put( "pos", NBTUtil.writeBlockPos( entry.getKey() ) );
                    insidesNBT.put( "UUID", NBTUtil.writeUniqueId( entry.getValue() ) );
                    newPlacedNBT.put( i++ + "", insidesNBT );
                }

                levelNBT.put( "placedPos", newPlacedNBT );
            }
        }
    }

    public static void addPos( ResourceLocation dimResLoc, BlockPos blockPos, UUID uuid )
    {
        ChunkPos chunkPos = new ChunkPos( blockPos );

        if( !placedMap.containsKey( dimResLoc ) )
            placedMap.put( dimResLoc, new HashMap<>() );

        Map<ChunkPos, Map<BlockPos, UUID>> chunkMap = placedMap.get( dimResLoc );

        if( !chunkMap.containsKey( chunkPos ) )
            chunkMap.put( chunkPos, new HashMap<>() );

        Map<BlockPos, UUID> blockMap = chunkMap.get( chunkPos );

        blockMap.put( blockPos, uuid );

//        System.out.println( chunkMap.size() );
//        System.out.println( blockMap.size() );
    }

    public static void delPos( ResourceLocation dimResLoc, BlockPos blockPos )
    {
        ChunkPos chunkPos = new ChunkPos( blockPos );

        if( !placedMap.containsKey( dimResLoc ) )
            placedMap.put( dimResLoc, new HashMap<>() );

        Map<ChunkPos, Map<BlockPos, UUID>> chunkMap = placedMap.get( dimResLoc );

        if( !chunkMap.containsKey( chunkPos ) )
            chunkMap.put( chunkPos, new HashMap<>() );

        Map<BlockPos, UUID> blockMap = chunkMap.get( chunkPos );

        blockMap.remove( blockPos );

//        System.out.println( chunkMap.size() );
//        System.out.println( blockMap.size() );
    }

    public static UUID checkPos( ResourceLocation dimResLoc, BlockPos blockPos )
    {
        ChunkPos chunkPos = new ChunkPos( blockPos );

        if( !placedMap.containsKey( dimResLoc ) )
            placedMap.put( dimResLoc, new HashMap<>() );

        Map<ChunkPos, Map<BlockPos, UUID>> chunkMap = placedMap.get( dimResLoc );

        if( !chunkMap.containsKey( chunkPos ) )
            chunkMap.put( chunkPos, new HashMap<>() );

        Map<BlockPos, UUID> blockMap = chunkMap.get( chunkPos );

        return blockMap.get( blockPos );
    }
}