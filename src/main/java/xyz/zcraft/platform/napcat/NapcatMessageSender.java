package xyz.zcraft.platform.napcat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.platform.AbstractApiMessageSender;

public class NapcatMessageSender extends AbstractApiMessageSender {
    private static final Logger LOG = LogManager.getLogger(NapcatMessageSender.class);

    public NapcatMessageSender(String httpEndpoint, String authToken) {
        super(new NapcatPrivateMessageApi(httpEndpoint, authToken), LOG);
    }
}

