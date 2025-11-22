package io.github.headlesshq.headlessmc.launcher.command;

import lombok.CustomLog;
import lombok.Data;
import io.github.headlesshq.headlessmc.api.HasId;
import io.github.headlesshq.headlessmc.api.HasName;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.FindByCommand;
import io.github.headlesshq.headlessmc.api.util.Table;
import io.github.headlesshq.headlessmc.launcher.Launcher;
import io.github.headlesshq.headlessmc.auth.ValidatedAccount;

import java.util.ArrayList;
import java.util.List;

@CustomLog
public class AccountsCommand extends AbstractLauncherCommand implements FindByCommand<AccountsCommand.ValidatedAccountWithId> {
    public AccountsCommand(Launcher ctx) {
        super(ctx, "account", "List accounts or chose the primary account.");
    }

    @Override
    public void execute(String line, String... args) throws CommandException {
        if (args.length < 2) {
            logTable();
            return;
        }

        FindByCommand.super.execute(line, args);
    }

    @Override
    public void execute(ValidatedAccountWithId account, String... args) throws CommandException {
        if (account.getAccount() != null) {
            ctx.getAccountManager().addAccount(account.getAccount()); // this will make this account the primary account
        } else if (account.getYggdrasilAccount() != null) {
            ctx.getAccountManager().addYggdrasilAccount(account.getYggdrasilAccount());
        }
        logTable();
        ctx.log("");
        ctx.log("Account " + account.getName() + " selected.");
    }

    @Override
    public Iterable<ValidatedAccountWithId> getIterable() {
        List<ValidatedAccountWithId> result = new ArrayList<>();
        int id = 0;
        // 添加 Microsoft 账户
        for (ValidatedAccount account : ctx.getAccountManager().getAccounts()) {
            result.add(new ValidatedAccountWithId(account, id++, "MSA"));
        }
        // 添加 Yggdrasil 账户
        for (io.github.headlesshq.headlessmc.auth.YggdrasilAccount account : ctx.getAccountManager().getYggdrasilAccounts()) {
            result.add(new ValidatedAccountWithId(account, id++, "Yggdrasil"));
        }

        return result;
    }

    @Data
    public static class ValidatedAccountWithId implements HasId, HasName {
        private final ValidatedAccount account;
        private final io.github.headlesshq.headlessmc.auth.YggdrasilAccount yggdrasilAccount;
        private final int id;
        private final String type;

        public ValidatedAccountWithId(ValidatedAccount account, int id, String type) {
            this.account = account;
            this.yggdrasilAccount = null;
            this.id = id;
            this.type = type;
        }

        public ValidatedAccountWithId(io.github.headlesshq.headlessmc.auth.YggdrasilAccount yggdrasilAccount, int id, String type) {
            this.account = null;
            this.yggdrasilAccount = yggdrasilAccount;
            this.id = id;
            this.type = type;
        }

        @Override
        public String getName() {
            if (account != null) {
                return account.getName();
            } else if (yggdrasilAccount != null) {
                return yggdrasilAccount.getName();
            }
            return "Unknown";
        }
    }

    private void logTable() {
        ctx.log(new Table<ValidatedAccountWithId>()
                    .withColumn("id", v -> String.valueOf(v.getId()))
                    .withColumn("type", v -> v.getType())
                    .withColumn("name", HasName::getName)
                    .addAll(getIterable())
                    .build());
    }

}
