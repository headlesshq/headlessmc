package io.github.headlesshq.headlessmc.auth;

import io.github.headlesshq.headlessmc.api.Application;
import io.github.headlesshq.headlessmc.api.command.CommandException;
import io.github.headlesshq.headlessmc.api.command.CommandLineManager;
import io.github.headlesshq.headlessmc.api.command.SupportsHidingPasswords;
import jakarta.inject.Inject;
import lombok.CustomLog;
import lombok.Setter;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import net.raphimc.minecraftauth.step.msa.StepJfxWebViewMsaCode;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.logging.ILogger;
import net.raphimc.minecraftauth.util.logging.JavaConsoleLogger;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.function.Consumer;

@Setter
@CustomLog
public abstract class AbstractLoginCommand {
    private final Application ctx;
    private final LoginState state;

    @CommandLine.Option(names = {"--email", "-e"}, description = "Logs in with an E-Mail.")
    private @Nullable String email;

    @CommandLine.Option(names = {"--password", "-p"}, description = "Specify together with E-Mail for logging in (not recommended).")
    private @Nullable String password;

    @CommandLine.Option(names = {"--webview", "-wv"}, description = "Opens a window to log you in. Might not be supported.")
    private boolean webview = false;

    @CommandLine.Option(names = {"--cancel", "-c"}, description = "Opens a window to log you in. Might not be supported.")
    private boolean cancel = false;

    @Inject
    public AbstractLoginCommand(Application ctx, LoginState state) {
        this.state = state;
        this.ctx = ctx;
        replaceLoggerOnConstruction();
    }

    protected abstract void onSuccessfulLogin(StepFullJavaSession.FullJavaSession session);

    protected void login() throws CommandException {
        if (cancel) {
            cancelLoginProcess();
        }

        if (webview) {
            loginWithWebview();
            return;
        }

        String email = this.email;
        if (email != null) {
            loginWithCredentials(email, password);
        } else if (password != null) {
            throw new CommandException("Please specify --email <E-Mail>.");
        } else if (!cancel) {
            loginWithDeviceCode();
        }
    }

    protected void loginWithCredentials(String email, @Nullable String password) throws CommandException {
        CommandLineManager clm = ctx.getCommandLine();
        if (password != null) {
            login(email, password);
            return;
        }

        String helpMessage = "Enter your password or type 'abort' to cancel the login process."
            + ((ctx.getCommandLine().getReader() instanceof SupportsHidingPasswords)
                ? ""
                : " (Your password will be visible when you type!)");
        ctx.log(helpMessage);

        SupportsHidingPasswords passwords = ctx.getCommandLine().getReader().getPasswordSupport();
        boolean passwordsHiddenBefore = passwords.isHidingPasswords();
        passwords.setHidingPasswords(true);
        ctx.getCommandLine().setInteractiveContext(
            new LoginContext(ctx, ctx.getCommandLine().getInteractiveContext(), helpMessage) {
                @Override
                protected void onCommand(String password) {
                    try {
                        login(email, password);
                    } finally {
                        returnToPreviousContext();
                        if (!passwordsHiddenBefore) {
                            passwords.setHidingPasswords(false);
                        }
                    }
                }
            }
        );
    }

    protected void login(String email, String password) {
        try {
            HttpClient httpClient = state.getHttpClientFactory().get();
            StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(
                getLogger(), httpClient, new StepCredentialsMsaCode.MsaCredentials(email, password));
            onSuccessfulLogin(session);
        } catch (Exception e) {
            ctx.log("Failed to login: " + e.getMessage());
            log.warn(e);
        }
    }

    protected void loginWithWebview() throws CommandException {
        if (webview == null) {
            webview = provideWebview();
            if (webview == null) {
                return;
            }
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpClient httpClient = httpClientFactory.get();

                    // TODO: look into this?
                    if (webviewWindow != null) {
                        CookieHandler.setDefault(new CookieManager());
                    }

                    StepFullJavaSession.FullJavaSession session = webview.getFromInput(getLogger(), httpClient, getWebViewCallback());
                    ctx.log("Session from Webview: " + session.getMcProfile().getName());
                    onSuccessfulLogin(session);
                } catch (InterruptedException e) {
                    ctx.log("Login process cancelled successfully.");
                } catch (NoClassDefFoundError e) {
                    log.debug(e.getMessage());
                    ctx.log("Your version of Java does not support Webview! It usually comes bundled with JDK 8 or in the headlessmc-launcher-jfx jar.");
                } catch (Throwable t) {
                    ctx.log("Failed to login with webview: " + t.getMessage());
                } finally {
                    synchronized (threads) {
                        threads.remove(this);
                    }
                }
            }
        };

        startLoginThread(thread);
    }

    protected StepJfxWebViewMsaCode.JavaFxWebView getWebViewCallback() {
        Consumer<JFrame> openCallback = frame -> frame.setVisible(true);
        return new StepJfxWebViewMsaCode.JavaFxWebView(
            openCallback,
            frame ->  {
                // dispose old JFrames, but keep one so JavaFX does not end!
                synchronized (webviewLock) {
                    Window before = webviewWindow;
                    if (before != null && frame.isVisible()) {
                        before.dispose();
                    }

                    frame.setVisible(false);
                    webviewWindow = frame;
                }
            }
        );
    }

    protected AbstractStep<?, StepFullJavaSession.FullJavaSession> provideWebview() throws CommandException {
        try {
            return MinecraftAuth.builder()
                                .withClientId(MicrosoftConstants.JAVA_TITLE_ID)
                                .withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
                                .javaFxWebView()
                                .withDeviceToken("Win32")
                                .sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
                                .buildMinecraftJavaProfileStep(true);
        } catch (NoClassDefFoundError e) {
            log.debug(e.getMessage());
            ctx.log("Your version of Java does not support Webview! It usually comes bundled with JDK 8 or in the headlessmc-launcher-jfx jar.");
        } catch (Throwable t) {
            throw new CommandException("Failed to login with webview: " + t.getMessage());
        }

        return null;
    }

    protected void loginWithDeviceCode() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    HttpClient httpClient = state.getHttpClientFactory().get();

                    StepMsaDeviceCode.MsaDeviceCodeCallback callback = new StepMsaDeviceCode.MsaDeviceCodeCallback(
                        msaDeviceCode -> ctx.log("Go to " + msaDeviceCode.getDirectVerificationUri()));

                    StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(getLogger(), httpClient, callback);

                    onSuccessfulLogin(session);
                } catch (InterruptedException e) {
                    ctx.log("Login process cancelled successfully.");
                } catch (Exception e) {
                    if (e.getCause() instanceof InterruptedException) {
                        ctx.log("Login process cancelled successfully.");
                    } else {
                        ctx.log("Failed to login with device code: " + e.getMessage());
                        log.info(e);
                    }
                } finally {
                    synchronized (threads) {
                        threads.remove(this);
                    }
                }
            }
        };

        startLoginThread(thread);
    }

    protected void cancelLoginProcess() {
        if (!state.cancelLogin()) {
            ctx.log("There was no login process to cancel.");
        }
    }

    protected void startLoginThread(Thread thread) {
        int threadId = 0;
        synchronized (threads) {
            String name = "HMC Login Thread - " + threadId;
            while (hasThreadWithName(name)) {
                threadId++;
                name = "HMC Login Thread - " + threadId;
            }

            thread.setName("HMC Login Thread - " + threadId);
            thread.setDaemon(true);
            threads.add(thread);
        }

        ctx.log("Starting login process " + threadId + ", enter 'login -cancel " + threadId + "' to cancel the login process.");
        thread.start();
    }

    protected ILogger getLogger() {
        // TODO: verbose option?
        return NoLogging.INSTANCE;
    }

    protected void replaceLoggerOnConstruction() {
        replaceLogger();
    }

    public static void replaceLogger() {
        MinecraftAuth.LOGGER = new JavaConsoleLogger(java.util.logging.Logger.getLogger("MinecraftAuth"));
    }

}
