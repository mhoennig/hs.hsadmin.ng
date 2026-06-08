package net.hostsharing.hsadminng.hs.hosting.asset;

import net.hostsharing.hsadminng.config.MessageTranslator;
import net.hostsharing.hsadminng.config.MessagesResourceConfig;
import net.hostsharing.hsadminng.config.WebSecurityConfigForWebMvcTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HsHostingAssetPropsController.class)
@Import({ MessagesResourceConfig.class,
          MessageTranslator.class,
          WebSecurityConfigForWebMvcTests.class })
@ActiveProfiles({"fake-jwt", "test"})
class HsHostingAssetPropsControllerRestTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void returnsHostingAssetTypes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/hosting/asset-types")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("MANAGED_WEBSPACE")))
                .andExpect(jsonPath("$", hasItem("UNIX_USER")));
    }

    @Test
    void returnsPropertiesOfHostingAssetType() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/hs/hosting/asset-types/UNIX_USER")
                        .accept(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].propertyName", notNullValue()));
    }
}
