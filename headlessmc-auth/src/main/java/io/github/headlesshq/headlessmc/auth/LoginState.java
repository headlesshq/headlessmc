package io.github.headlesshq.headlessmc.auth;

import io.github.headlesshq.headlessmc.api.command.CommandException;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.function.Supplier;

@Getter
@ApplicationScoped
public class LoginState {
    private final Object webviewLock = new Object();
    private final Object threadLock = new Object();

    private Supplier<HttpClient> httpClientFactory = MinecraftAuth::createHttpClient;
    private volatile @Nullable AbstractStep<?, StepFullJavaSession.FullJavaSession> webview;
    private volatile @Nullable Window webviewWindow = null;
    private volatile @Nullable Thread loginThread = null;

    public boolean cancelLogin() {
        synchronized (threadLock) {
            Thread thread = loginThread;
            if (thread != null) {
                thread.interrupt();
                loginThread = null;
                return true;
            }
        }

        return false;
    }

    private AbstractStep<?, StepFullJavaSession.FullJavaSession> provideWebview() throws CommandException {
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

}
