package harmonised.pmmo.impl;

import java.util.function.BiPredicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;

import harmonised.pmmo.api.enums.ReqType;
import harmonised.pmmo.util.MsLoggy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PredicateRegistry {
	public PredicateRegistry() {}
	
	private LinkedListMultimap<String, BiPredicate<Player, ItemStack>> reqPredicates = LinkedListMultimap.create();
	private LinkedListMultimap<String, BiPredicate<Player, BlockEntity>> reqBreakPredicates = LinkedListMultimap.create();
	private LinkedListMultimap<String, BiPredicate<Player, Entity>> reqEntityPredicates = LinkedListMultimap.create();
	
	/** registers a predicate to be used in determining if a given player is permitted
	 * to perform a particular action. [Except for break action.  see registerBreakPredicate.
	 * The ResouceLocation and JType parameters are 
	 * conditions for when this check should be applied and are used by PMMO to know
	 * which predicates apply in which contexts.  The predicate itself links to the 
	 * compat mod's logic which handles the external behavior.
	 * 
	 * @param res the block, item, or entity registrykey
	 * @param jType the PMMO behavior type
	 * @param pred what executes to determine if player is permitted to perform the action
	 */
	public void registerPredicate(ResourceLocation res, ReqType jType, BiPredicate<Player, ItemStack> pred) {
		Preconditions.checkNotNull(pred);
		String condition = jType.toString()+";"+res.toString();
		reqPredicates.get(condition).add(pred);
		MsLoggy.info("Predicate Registered: "+condition);
	}
	
	/** registers a predicate to be used in determining if a given player is permitted
	 * to break a block.  The ResouceLocation and JType parameters are 
	 * conditions for when this check should be applied and are used by PMMO to know
	 * which predicates apply in which contexts.  The predicate itself links to the 
	 * compat mod's logic which handles the external behavior.
	 * 
	 * @param res the block, item, or entity registrykey
	 * @param jType the PMMO behavior type
	 * @param pred what executes to determine if player is permitted to perform the action
	 */
	public void registerBreakPredicate(ResourceLocation res, ReqType jType, BiPredicate<Player, BlockEntity> pred) {
		Preconditions.checkNotNull(pred);
		String condition = jType.toString()+";"+res.toString();
		reqBreakPredicates.get(condition).add(pred);
		MsLoggy.info("Predicate Registered: "+condition);
	}
	
	public void registerEntityPredicate(ResourceLocation res, ReqType type, BiPredicate<Player, Entity> pred) {
		Preconditions.checkNotNull(pred);
		String condition = type.toString()+";"+res.toString();
		reqEntityPredicates.get(condition).add(pred);
		MsLoggy.info("Entity Predicate Regsitered: "+condition);
	}
	
	/**this is an internal method to check if a predicate exists for the given conditions
	 * 
	 * @param res res the block, item, or entity registrykey
	 * @param jType the PMMO behavior type
	 * @return whether or not a predicate is registered for the parameters
	 */
	public boolean predicateExists(ResourceLocation res, ReqType type) 
	{
		String key = type.toString()+";"+res.toString();
		return reqPredicates.containsKey(key) ||
				reqBreakPredicates.containsKey(key) ||
				reqEntityPredicates.containsKey(key);
	}
	
	/**this is executed by PMMO logic to determine if the player is permitted to perform
	 * the action according to the object and type contexts.  
	 * 
	 * @param player the player performing the action
	 * @param res res res the block, item, or entity registrykey
	 * @param jType the PMMO behavior type
	 * @return whether the player is permitted to do the action (true if yes)
	 */
	public boolean checkPredicateReq(Player player, ItemStack stack, ReqType jType) 
	{
		if (!predicateExists(stack.getItem().getRegistryName(), jType)) 
			return false;
		for (BiPredicate<Player, ItemStack> pred : reqPredicates.get(jType.toString()+";"+stack.getItem().getRegistryName().toString())) {
			if (!pred.test(player, stack)) return false;
		}
		return true;
	}
	
	/**this is executed by PMMO logic to determine if the player is permitted to break
	 * the block according to the object and type contexts.  
	 * 
	 * @param player the player performing the action
	 * @param res res res the block, item, or entity registrykey
	 * @param jType the PMMO behavior type
	 * @return whether the player is permitted to do the action (true if yes)
	 */
	public boolean checkPredicateReq(Player player, BlockEntity tile, ReqType jType) 
	{
		ResourceLocation res = tile.getBlockState().getBlock().getRegistryName();
		if (!predicateExists(res, jType)) 
			return false;
		for (BiPredicate<Player, BlockEntity> pred : reqBreakPredicates.get(jType.toString()+";"+res.toString())) {
			if (!pred.test(player, tile)) return false;
		}
		return true;
	}
	
	public boolean checkPredicateReq(Player player, Entity entity, ReqType type) {
		ResourceLocation res = new ResourceLocation(entity.getEncodeId());
		if (!predicateExists(res, type))
			return false;
		for (BiPredicate<Player, Entity> pred : reqEntityPredicates.get(type.toString()+";"+res.toString())) {
			if (!pred.test(player, entity)) return false;
		}
		return true;
	}
}