package harmonised.pmmo.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import harmonised.pmmo.config.Requirements;
import harmonised.pmmo.network.MessageDoubleTranslation;
import harmonised.pmmo.network.MessageUpdateNBT;
import harmonised.pmmo.network.MessageXp;
import harmonised.pmmo.network.NetworkHandler;
import harmonised.pmmo.skills.AttributeHandler;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.skills.XP;
import harmonised.pmmo.util.DP;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.management.Attribute;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class PmmoCommand
{
    private static final Logger LOGGER = LogManager.getLogger();

    public static void register( CommandDispatcher<CommandSource> dispatcher )
    {
        String[] suggestSkill = new String[15];
        suggestSkill[0] = "Mining";
        suggestSkill[1] = "Building";
        suggestSkill[2] = "Excavation";
        suggestSkill[3] = "Woodcutting";
        suggestSkill[4] = "Farming";
        suggestSkill[5] = "Agility";
        suggestSkill[6] = "Endurance";
        suggestSkill[7] = "Combat";
        suggestSkill[8] = "Archery";
        suggestSkill[9] = "Smithing";
        suggestSkill[10] = "Flying";
        suggestSkill[11] = "Swimming";
        suggestSkill[12] = "Fishing";
        suggestSkill[13] = "Crafting";
        suggestSkill[14] = "Magic";

        String[] suggestClear = new String[1];
        suggestClear[0] = "iagreetothetermsandconditions";

        String[] levelOrXp = new String[2];
        levelOrXp[0] = "level";
        levelOrXp[1] = "xp";

        String[] suggestPref = new String[6];
        suggestPref[0] = "maxReachBoost";
        suggestPref[1] = "maxSpeedBoost";
        suggestPref[2] = "maxSprintJumpBoost";
        suggestPref[3] = "maxCrouchJumpBoost";
        suggestPref[4] = "maxExtraHeartBoost";
        suggestPref[5] = "maxExtraDamageBoost";

        dispatcher.register( Commands.literal( "pmmo" )
                  .then( Commands.literal( "admin" )
                  .requires( player -> { return player.hasPermissionLevel( 2 ); })
                  .then( Commands.argument( "target", EntityArgument.players() )
                  .then( Commands.literal( "set" )
                  .then( Commands.argument( "Skill", StringArgumentType.word() )
                  .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestSkill, theBuilder ) )
                  .then( Commands.argument( "Level|Xp", StringArgumentType.word() )
                  .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( levelOrXp, theBuilder ) )
                  .then( Commands.argument( "New Value", DoubleArgumentType.doubleArg() )
                  .executes( PmmoCommand::commandSet )
                  ))))
                  .then( Commands.literal( "add" )
                  .then( Commands.argument( "Skill", StringArgumentType.word() )
                  .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestSkill, theBuilder ) )
                  .then( Commands.argument( "Level|Xp", StringArgumentType.word() )
                  .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( levelOrXp, theBuilder ) )
                  .then( Commands.argument( "Value To Add", DoubleArgumentType.doubleArg() )
                  .executes( PmmoCommand::commandAdd )
                  ))))
                  .then( Commands.literal( "clear" )
                  .executes( PmmoCommand::commandClear ) )))
                  .then( Commands.literal( "reload" )
                  .requires( player -> { return player.hasPermissionLevel( 2 ); } )
                  .executes( PmmoCommand::commandReloadConfig )
                  )
                  .then( Commands.literal( "resync" )
                  .executes( context -> commandSync( context, EntityArgument.getPlayers( context, "target" ) ) )
                  )
                  .then(Commands.literal( "resync" )
                  .executes( context -> commandSync( context, null )))
                  .then( Commands.literal( "tools" )
                  .then( Commands.literal( "levelatxp" )
                  .then( Commands.argument( "xp", DoubleArgumentType.doubleArg() )
                  .executes( PmmoCommand::commandLevelAtXp )
                  ))
                  .then( Commands.literal( "xpatlevel" )
                  .then(  Commands.argument( "level", DoubleArgumentType.doubleArg() )
                  .executes( PmmoCommand::commandXpAtLevel )
                  ))
                  .then( Commands.literal( "xpto" )
                  .then(  Commands.argument( "level", DoubleArgumentType.doubleArg() )
                  .executes( PmmoCommand::commandXpFromTo )
                  .then(  Commands.argument( "goal level", DoubleArgumentType.doubleArg() )
                  .executes( PmmoCommand::commandXpFromTo )
                  ))))
                  .then( Commands.literal( "prefs" )
                  .then( Commands.argument( "option", StringArgumentType.word() )
                  .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestPref, theBuilder ) )
                  .executes( PmmoCommand::commandPref )
                  .then( Commands.argument( "new value", DoubleArgumentType.doubleArg() )
                  .executes( PmmoCommand::commandPref )
                  )))
                  .then( Commands.literal( "checkstat" )
                  .then( Commands.argument( "player name", EntityArgument.player() )
                  .then( Commands.argument( "skill name", StringArgumentType.word() )
                  .suggests( ( ctx, theBuilder ) -> ISuggestionProvider.suggest( suggestSkill, theBuilder ) )
                  .executes( PmmoCommand::commandCheckStat )
                  ))));
    }

    private static int commandClear( CommandContext<CommandSource> context ) throws CommandException
    {
        String[] args = context.getInput().split( " " );

        try
        {
            Collection<ServerPlayerEntity> players = EntityArgument.getPlayers( context, "target" );

            for( ServerPlayerEntity player : players )
            {
                AttributeHandler.updateAll( player );

                NetworkHandler.sendToPlayer( new MessageXp( 0f, 42069, 0, true ), player );
                player.getPersistentData().getCompound( "pmmo" ).put( "skills", new CompoundNBT() );

                player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.skillsCleared" ), false );
            }
        }
        catch( CommandSyntaxException e )
        {
            LOGGER.error( "Clear Command Failed to get Players [" + Arrays.toString(args) + "]", e );
        }

        return 1;
    }

    private static int commandSet(CommandContext<CommandSource> context) throws CommandException
    {
        String[] args = context.getInput().split( " " );
        String skillName = args[4].toLowerCase();
        int skillInt = Skill.getInt( skillName );
        PlayerEntity sender = null;

        try
        {
            sender = context.getSource().asPlayer();
        }
        catch( CommandSyntaxException e )
        {
            //not player, it's fine
        }

        if( skillInt != 0 )
        {
            try
            {
                Collection<ServerPlayerEntity> players = EntityArgument.getPlayers( context, "target" );

                for( ServerPlayerEntity player : players )
                {
                    CompoundNBT skillsTag = XP.getSkillsTag( player );
                    double newValue = Double.parseDouble( args[6] );

                    if( newValue > XP.maxXp )
                        newValue = XP.maxXp;

                    if( newValue < 0 )
                        newValue = 0;

                    if( args[5].toLowerCase().equals( "level" ) )
                    {
                        if( newValue > XP.maxLevel )
                            newValue = XP.maxLevel;

                        double newLevelXp = XP.xpAtLevel( newValue );

                        NetworkHandler.sendToPlayer( new MessageXp( newLevelXp, skillInt, 0, true ), player );
                        skillsTag.putDouble( skillName, newLevelXp );

                        player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.setLevel", skillName, (newValue % 1 == 0 ? (int) Math.floor(newValue) : DP.dp(newValue) ) ), false );
                    }
                    else if( args[5].toLowerCase().equals( "xp" ) )
                    {
                        NetworkHandler.sendToPlayer( new MessageXp( newValue, skillInt, 0, true ), player );
                        skillsTag.putDouble( skillName, newValue );

                        player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.setXp", skillName, DP.dp(newValue) ), false );
                    }
                    else
                    {
                        LOGGER.error( "Invalid 6th Element in command (level|xp) " + Arrays.toString( args ) );

                        if( sender != null )
                            sender.sendStatusMessage( new TranslationTextComponent( "pmmo.text.invalidChoice", args[5] ).setStyle( new Style().setColor( TextFormatting.RED ) ), false );
                    }

                    AttributeHandler.updateAll( player );
                }
            }
            catch( CommandSyntaxException e )
            {
                LOGGER.error( "Set Command Failed to get Players [" + Arrays.toString(args) + "]", e );
            }
        }
        else
        {
            LOGGER.error( "Invalid 5th Element in command (skill name) " + Arrays.toString( args ) );

            if( sender != null )
                sender.sendStatusMessage( new TranslationTextComponent( "pmmo.text.invalidSkill", skillName ).setStyle( new Style().setColor( TextFormatting.RED ) ), false );
        }

        return 1;
    }

    private static int commandAdd(CommandContext<CommandSource> context) throws CommandException
    {
        String[] args = context.getInput().split( " " );
        String skillName = args[4].toLowerCase();
        int skillInt = Skill.getInt( skillName );
        PlayerEntity sender = null;

        try
        {
            sender = context.getSource().asPlayer();
        }
        catch( CommandSyntaxException e )
        {
            //not player, it's fine
        }

        if( skillInt != 0 )
        {
            try
            {
                Collection<ServerPlayerEntity> players = EntityArgument.getPlayers( context, "target" );

                for( ServerPlayerEntity player : players )
                {
                    CompoundNBT skillsTag = XP.getSkillsTag( player );
                    double newValue = Double.parseDouble( args[6] );
                    double playerXp = skillsTag.getDouble( skillName );
                    double newLevelXp;

                    if( args[5].toLowerCase().equals( "level" ) )
                    {
                        newLevelXp = XP.xpAtLevel( XP.levelAtXp( playerXp ) + newValue );

                        if( newLevelXp > XP.maxXp )
                            newLevelXp = XP.maxXp;

                        if( newLevelXp < 0 )
                            newLevelXp = 0;

                        NetworkHandler.sendToPlayer( new MessageXp( newLevelXp, skillInt, 0, true ), player );
                        skillsTag.putDouble( skillName, newLevelXp );

                        player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.addLevel", skillName, (newValue % 1 == 0 ? (int) Math.floor(newValue) : DP.dp(newValue) ) ), false );
                    }
                    else if( args[5].toLowerCase().equals( "xp" ) )
                    {
                        newLevelXp = newValue + playerXp;

                        if( newLevelXp > XP.maxXp )
                            newLevelXp = XP.maxXp;

                        if( newLevelXp < 0 )
                            newLevelXp = 0;

                        NetworkHandler.sendToPlayer( new MessageXp( newLevelXp, skillInt, 0, true ), player );
                        skillsTag.putDouble( skillName, newLevelXp );

                        player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.addXp", skillName, DP.dp(newValue) ), false );
                    }
                    else
                    {
                        LOGGER.error( "Invalid 6th Element in command (level|xp) " + Arrays.toString( args ) );

                        if( sender != null )
                            sender.sendStatusMessage( new TranslationTextComponent( "pmmo.text.invalidChoice", args[5] ).setStyle( new Style().setColor( TextFormatting.RED ) ), false );
                    }

                    AttributeHandler.updateAll( player );
                }
            }
            catch( CommandSyntaxException e )
            {
                LOGGER.error( "Add Command Failed to get Players [" + Arrays.toString(args) + "]", e );
            }
        }
        else
        {
            LOGGER.error( "Invalid 5th Element in command (skill name) " + Arrays.toString( args ) );

            if( sender != null )
                sender.sendStatusMessage( new TranslationTextComponent( "pmmo.text.invalidSkill", skillName ).setStyle( new Style().setColor( TextFormatting.RED ) ), false );
        }

        return 1;
    }

    private static int commandSync( CommandContext<CommandSource> context, @Nullable  Collection<ServerPlayerEntity> players ) throws CommandException
    {
        if( players != null )
        {
            for( ServerPlayerEntity player : players )
            {
                XP.syncPlayer( player );
                player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.skillsResynced" ), false );
            }
        }
        else
        {
            try
            {
                PlayerEntity player = context.getSource().asPlayer();
                XP.syncPlayer( player );
                player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.skillsResynced" ), false );
            }
            catch( CommandSyntaxException e )
            {
                LOGGER.error( "Sync command fired not from player " + context.getInput(), e );
            }
        }

        return 1;
    }

    private static int commandLevelAtXp(CommandContext<CommandSource> context) throws CommandException
    {
        PlayerEntity player = (PlayerEntity) context.getSource().getEntity();
        String[] args = context.getInput().split(" ");
        double xp = Double.parseDouble( args[3] );

        if( xp < 0 )
            xp = 0;

        if( xp >= XP.maxXp )
            player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.levelAtXp", DP.dp( xp ), XP.maxLevel ), false );
        else
            player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.levelAtXp", DP.dp( xp ), XP.levelAtXpDecimal( xp ) ), false );
        return 1;
    }

    private static int commandXpAtLevel(CommandContext<CommandSource> context) throws CommandException
    {
        PlayerEntity player = (PlayerEntity) context.getSource().getEntity();
        String[] args = context.getInput().split(" ");
        double level = Double.parseDouble( args[3] );

        if( level < 1 )
            level = 1;

        if( level > XP.maxLevel )
            level = XP.maxLevel;

        player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.xpAtLevel", ( level % 1 == 0 ? (int) Math.floor( level ) : DP.dp(level) ), DP.dp( XP.xpAtLevelDecimal( level ) ) ), false );

        return 1;
    }

    private static int commandXpFromTo(CommandContext<CommandSource> context) throws CommandException
    {
        PlayerEntity player = (PlayerEntity) context.getSource().getEntity();
        String[] args = context.getInput().split(" ");

        double level = Double.parseDouble( args[3] );
        if( level < 1 )
            level = 1;
        if( level > XP.maxLevel )
            level = XP.maxLevel;
        double xp = XP.xpAtLevelDecimal( level );
        if( xp < 0 )
            xp = 0;

        if( args.length > 4 )
        {
            double goalLevel = Double.parseDouble( args[4] );
            if( goalLevel < 1 )
                goalLevel = 1;
            if( goalLevel > XP.maxLevel )
                goalLevel = XP.maxLevel;

            if( goalLevel < level )
            {
                double temp = goalLevel;
                goalLevel = level;
                level = temp;

                xp = XP.xpAtLevelDecimal( level );
            }

            double goalXp = XP.xpAtLevelDecimal( goalLevel );
            if( goalXp < 0 )
                goalXp = 0;

            player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.xpFromTo", DP.dp(goalXp - xp), ( level % 1 == 0 ? (int) Math.floor( level ) : DP.dp(level) ), ( goalLevel % 1 == 0 ? (int) Math.floor( goalLevel ) : DP.dp(goalLevel) ) ), false );
        }
        else
            player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.xpAtLevel", ( level % 1 == 0 ? (int) Math.floor( level ) : DP.dp(level) ), DP.dp(xp) ), false );

        return 1;
    }

    private static int commandPref(CommandContext<CommandSource> context) throws CommandException
    {
        PlayerEntity player = (PlayerEntity) context.getSource().getEntity();
        String[] args = context.getInput().split(" ");
        CompoundNBT prefsTag = XP.getPreferencesTag( player );
        double value = Double.parseDouble( args[3] );
        if( value < 0 )
            value = 0;

        switch( args[2].toLowerCase() )
        {
            case "maxreachboost":
                prefsTag.putDouble( "maxReachBoost", value );
                break;

            case "maxspeedboost":
                prefsTag.putDouble( "maxSpeedBoost", value );
                break;

            case "maxsprintjumpboost":
                prefsTag.putDouble( "maxSprintJumpBoost", value );
                break;

            case "maxcrouchjumpboost":
                prefsTag.putDouble( "maxCrouchJumpBoost", value );
                break;

            case "maxextraheartboost":
                prefsTag.putDouble( "maxExtraHeartBoost", value );
                break;

            case "maxextradamageboost":
                prefsTag.putDouble( "maxExtraDamageBoost", value );
                break;

            default:
                player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.invalidChoice", args[2] ).setStyle( new Style().setColor( TextFormatting.RED ) ), false );
                return 1;
        }

        NetworkHandler.sendToPlayer( new MessageUpdateNBT( prefsTag, "prefs" ), (ServerPlayerEntity) player );
        AttributeHandler.updateAll( player );

        player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.hasBeenSet", args[2], args[3] ), false );

        return 1;
    }

    private static int commandCheckStat(CommandContext<CommandSource> context) throws CommandException
    {
        PlayerEntity sender = (PlayerEntity) context.getSource().getEntity();
        String[] args = context.getInput().split(" ");
        String skillName = args[3].toLowerCase();

        if( Skill.getInt( skillName ) != 0 )
        {
            try
            {
                ServerPlayerEntity target = EntityArgument.getPlayer( context, "player name" );
                double level = XP.levelAtXpDecimal( XP.getSkillsTag( target ).getDouble( skillName ) );

                sender.sendStatusMessage(  new TranslationTextComponent( "pmmo.text.playerLevelDisplay", target.getDisplayName().getString(), (level % 1 == 0 ? (int) Math.floor(level) : DP.dp(level)), new TranslationTextComponent( "pmmo.text." + skillName ).setStyle( new Style().setColor( XP.skillTextFormat.get( skillName ) ) ) ), false );
            }
            catch( CommandSyntaxException e )
            {
                sender.sendStatusMessage(  new TranslationTextComponent( "pmmo.text.invalidPlayer", args[2] ).setStyle( new Style().setColor( TextFormatting.RED ) ), false );
            }
        }
        else
            sender.sendStatusMessage( new TranslationTextComponent( "pmmo.text.invalidSkill", skillName ).setStyle( new Style().setColor( TextFormatting.RED ) ), false );

        return 1;
    }

    private static int commandReloadConfig(CommandContext<CommandSource> context) throws CommandException
    {
        Requirements.init();    //load up locally

        context.getSource().getServer().getPlayerList().getPlayers().forEach( player ->
        {
            XP.syncPlayerConfig( player );
            player.sendStatusMessage( new TranslationTextComponent( "pmmo.text.jsonConfigReload" ).setStyle( new Style().setColor( TextFormatting.GREEN ) ), false );
        });

        return 1;
    }
}
