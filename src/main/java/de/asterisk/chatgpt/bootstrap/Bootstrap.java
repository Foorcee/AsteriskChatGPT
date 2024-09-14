package de.asterisk.chatgpt.bootstrap;

import de.asterisk.chatgpt.AsteriskFastAGIServer;

public class Bootstrap {

    public static void main(String[] args) {
        AsteriskFastAGIServer asteriskFastAGIServer = new AsteriskFastAGIServer();
        asteriskFastAGIServer.start();
    }
}
