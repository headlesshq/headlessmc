package io.github.headlesshq.headlessmc.launcher.mods.command;

import io.github.headlesshq.headlessmc.api.traits.HasId;
import io.github.headlesshq.headlessmc.api.traits.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.launcher.mods.ModdableGame;
import io.github.headlesshq.headlessmc.launcher.mods.ModdableGameProvider;
import io.github.headlesshq.headlessmc.launcher.mods.files.ModFile;
import io.github.headlesshq.headlessmc.launcher.mods.files.ModFileReadResult;

import java.io.IOException;
import java.nio.file.Files;

public class RemoveModCommand extends AbstractModdableGameCommand {
    public RemoveModCommand(Launcher ctx, ModdableGameProvider provider) {
        super(ctx, "remove", "Removes mods of a game.", provider);
    }

    @Override
    public void execute(ModdableGame game, String... args) throws CommandException {
        try {
            if (args.length <= 2) {
                throw new CommandException("Please specify a mod to delete.");
            }

            ModFileReadResult result = ctx.getModManager().getModFileReaderManager().read(game);
            if (result.getNonModFiles() > 0) {
                ctx.log(result.getNonModFiles() + " mod files could not be read and might be for other platforms.");
            }

            ModFile modFile = HasName.getByName(args[2], result.getMods());
            if (modFile == null) {
                modFile = HasId.getById(args[2], result.getMods());
                if (modFile == null) {
                    throw new CommandException("Mod '" + args[2] + "' not found in '" + game.getName() + "'.");
                }
            }

            for (ModFile otherMod : result.getMods()) {
                if (otherMod.getPath().equals(modFile.getPath()) && otherMod.getId() != modFile.getId()) {
                    ctx.log(otherMod.getName() + " will also be removed!");
                }
            }

            Files.deleteIfExists(modFile.getPath());
            ctx.log("Mod '" + modFile.getName() + "' deleted successfully.");
        } catch (IOException e) {
            throw new CommandException(e.getMessage());
        }
    }

}
