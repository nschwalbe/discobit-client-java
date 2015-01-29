package com.v3rticle.oss.discobit;

import com.v3rticle.oss.discobit.client.DiscobitClient;
import com.v3rticle.oss.discobit.client.DiscobitClientFactory;
import com.v3rticle.oss.discobit.client.DiscobitOperationException;
import com.v3rticle.oss.discobit.client.bootstrap.DiscobitOptions;
import org.junit.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public class DiscobitClientTest {

	static DiscobitClient discobit;

    private static final UUID CONFIG_UUID = UUID.fromString("b9622d1c-ed4b-4415-a15b-e2489a1bc984");

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.setProperty(DiscobitOptions.DISCOBIT_SERVER_URL, "http://127.0.0.1:8004");
		System.setProperty(DiscobitOptions.DISCOBIT_SERVER_USERNAME, "cfgadmin");
		System.setProperty(DiscobitOptions.DISCOBIT_SERVER_PASSWORD, "cfgadmin");
		System.clearProperty(DiscobitOptions.DISCOBIT_CONFIG_UUID);
		discobit = DiscobitClientFactory.getClient();
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {

        // TODO remove test space
    }

    @Test
    @Ignore("Create Configuration fails!")
    public void createAppSpaceAndConfig() throws DiscobitOperationException {

        int spaceID = discobit.createApplicationSpace("junit", "junit@v3rticle.com", "junit-app-" + System.currentTimeMillis(), "http://v3rticle.com");
        Assert.assertTrue(spaceID >= 0);

        UUID cUUID = discobit.createConfiguration(spaceID, "junit-cfg-" + System.currentTimeMillis(), "test configuration");
        Assert.assertNotNull(cUUID);
    }


	@Test
	public void readConfiguration() throws DiscobitOperationException {

        Properties config = discobit.getConfig(CONFIG_UUID);

		Assert.assertNotNull(config);
		Assert.assertTrue(config.size() > 0);
	}

    @Test
    public void checkConfigExists() throws DiscobitOperationException {

        boolean exists = discobit.checkConfigExists(CONFIG_UUID.toString());
        Assert.assertTrue(exists);
    }

    @Test
    @Ignore("Get Config Property fails")
    public void getConfigProperty() throws DiscobitOperationException {

        String property = discobit.getConfigProperty(CONFIG_UUID, "app.stage", false);

        Assert.assertNotNull(property);
        Assert.assertEquals("development", property);
    }

    @Test
    public void pushConfig() throws IOException {

        Path tmpFile = Files.createTempFile("Discobit", "test.conf");
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile, Charset.forName("UTF-8"))) {

            writer.write("my.prop=abc");
            writer.newLine();
            writer.write("my.other.prop=xyz");
        }


        boolean successPush = discobit.pushConfiguration(CONFIG_UUID.toString(), tmpFile.toFile());
        Assert.assertTrue(successPush);
    }
}
