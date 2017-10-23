package ch.squaredesk.nova.comm.websockets;

class DummySession /* implements Session */ {
    /*
    private final Set<MessageHandler> messageHandlers = new CopyOnWriteArraySet<>();
    @Override
    public WebSocketContainer getContainer() {
        return null;
    }

    @Override
    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        messageHandlers.add(handler);
    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) {
        messageHandlers.add(handler);
    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) {
        messageHandlers.add(handler);
    }

    @Override
    public Set<MessageHandler> getMessageHandlers() {
        return messageHandlers;
    }

    @Override
    public void removeMessageHandler(MessageHandler handler) {
        messageHandlers.remove(handler);
    }

    @Override
    public String getProtocolVersion() {
        return null;
    }

    @Override
    public String getNegotiatedSubprotocol() {
        return null;
    }

    @Override
    public List<Extension> getNegotiatedExtensions() {
        return null;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public long getMaxIdleTimeout() {
        return 0;
    }

    @Override
    public void setMaxIdleTimeout(long milliseconds) {

    }

    @Override
    public void setMaxBinaryMessageBufferSize(int length) {

    }

    @Override
    public int getMaxBinaryMessageBufferSize() {
        return 0;
    }

    @Override
    public void setMaxTextMessageBufferSize(int length) {

    }

    @Override
    public int getMaxTextMessageBufferSize() {
        return 0;
    }

    @Override
    public RemoteEndpoint.Async getAsyncRemote() {
        return new RemoteEndpoint.Async() {
            @Override
            public long getSendTimeout() {
                return 0;
            }

            @Override
            public void setSendTimeout(long timeoutmillis) {

            }

            @Override
            public void sendText(String text, SendHandler handler) {

            }

            @Override
            public Future<Void> sendText(String text) {
                return null;
            }

            @Override
            public Future<Void> sendBinary(ByteBuffer data) {
                return null;
            }

            @Override
            public void sendBinary(ByteBuffer data, SendHandler handler) {

            }

            @Override
            public Future<Void> sendObject(Object data) {
                return null;
            }

            @Override
            public void sendObject(Object data, SendHandler handler) {

            }

            @Override
            public void setBatchingAllowed(boolean allowed) throws IOException {

            }

            @Override
            public boolean getBatchingAllowed() {
                return false;
            }

            @Override
            public void flushBatch() throws IOException {

            }

            @Override
            public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

            }

            @Override
            public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

            }
        };
    }

    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        return new RemoteEndpoint.Basic() {

            @Override
            public void setBatchingAllowed(boolean allowed) throws IOException {

            }

            @Override
            public boolean getBatchingAllowed() {
                return false;
            }

            @Override
            public void flushBatch() throws IOException {

            }

            @Override
            public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

            }

            @Override
            public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

            }

            @Override
            public void sendText(String text) throws IOException {

            }

            @Override
            public void sendBinary(ByteBuffer data) throws IOException {

            }

            @Override
            public void sendText(String partialMessage, boolean isLast) throws IOException {

            }

            @Override
            public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException {

            }

            @Override
            public OutputStream getSendStream() throws IOException {
                return null;
            }

            @Override
            public Writer getSendWriter() throws IOException {
                return null;
            }

            @Override
            public void sendObject(Object data) throws IOException, EncodeException {

            }
        };
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void close(CloseReason closeReason) throws IOException {

    }

    @Override
    public URI getRequestURI() {
        return null;
    }

    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        return null;
    }

    @Override
    public String getQueryString() {
        return null;
    }

    @Override
    public Map<String, String> getPathParameters() {
        return null;
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return null;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public Set<Session> getOpenSessions() {
        return null;
    }
    */
}
