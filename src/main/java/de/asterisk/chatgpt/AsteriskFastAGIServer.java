package de.asterisk.chatgpt;

import de.asterisk.chatgpt.scripts.DefaultChatGPTScript;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.AgiScript;
import org.asteriskjava.fastagi.AgiServerThread;
import org.asteriskjava.fastagi.DefaultAgiServer;
import org.asteriskjava.fastagi.MappingStrategy;

public class AsteriskFastAGIServer {

    private final AgiServerThread serverThread;

    public AsteriskFastAGIServer() {
        this.serverThread = new AgiServerThread();
        this.serverThread.setAgiServer(new DefaultAgiServer(new MappingStrategy() {
            @Override
            public AgiScript determineScript(AgiRequest agiRequest, AgiChannel agiChannel) {
                return new DefaultChatGPTScript();
                //return new RAGChatGTPScript();
            }
        }));
        this.serverThread.setDaemon(false);
    }

    public void start() {
        this.serverThread.startup();
    }
}
