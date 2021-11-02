package org.cloud.client.model;

import org.cloud.core.Command;

public interface ReadCommandListener {

    void processReceivedCommand(Command command);
}
