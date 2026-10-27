package io.github.headlesshq.headlessmc.mods.github;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The {@code hmc-mcp-bridge}, distributed by <a href=https://github.com/headlesshq/headlessmc-mcp>headlesshq/headlessmc-mcp</a>.
 */
@ApplicationScoped
public class HmcMcpRepository extends AbstractHmcSpecificsAddonRepository {
    /**
     * The name of this mod in HeadlessMc.
     */
    public static final String NAME = "hmc-mcp";

    /**
     * Constructs a new HmcMcpRepository.
     */
    public HmcMcpRepository() {
        super(NAME, "headlessmc-mcp", "hmc-mcp-bridge", "Bridge between the game and the HeadlessMc MCP server.", "hmc_mcp_bridge");
    }

}
