package scrabble.server.network;

import scrabble.server.model.*;
import scrabble.protocol.Message;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerNetworkHandler {
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ServerModel model;
    private ExecutorService executor;
    private volatile boolean running;

    public ServerNetworkHandler(ServerModel model) {
        this.model = model;
        this.executor = Executors.newCachedThreadPool();
        this.running = false;
    }

    public void start(int port) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(port));

        selector = Selector.open();
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        running = true;
        System.out.println("Сервер запущен на порту " + port);

        executor.submit(this::runServerLoop);
    }

    private void runServerLoop() {
        try {
            while (running) {
                selector.select(100);

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();

                    if (key.isAcceptable()) {
                        acceptClient(key);
                    } else if (key.isReadable()) {
                        readFromClient(key);
                    }

                    iter.remove();
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка в основном цикле сервера: " + e.getMessage());
        }
    }

    private void acceptClient(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        String clientId = "client_" + System.currentTimeMillis() + "_" + clientChannel.hashCode();
        ClientHandler handler = new ClientHandler(clientChannel, clientId, model);

        clientChannel.register(selector, SelectionKey.OP_READ, handler);
        model.registerClient(clientId, handler);

        System.out.println("Клиент подключен: " + clientId);
    }

    private void readFromClient(SelectionKey key) throws IOException {
        ClientHandler handler = (ClientHandler) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = channel.read(buffer);

        if (bytesRead == -1) {
            
            handler.disconnect();
            key.cancel();
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            String message = StandardCharsets.UTF_8.decode(buffer).toString();
            handler.processMessage(message);
        }
    }

    public void broadcastToRoom(String roomId, Message message, String excludeClientId) {
        GameRoom room = model.getRoom(roomId);
        if (room != null) {
            for (String playerId : room.getPlayerIds()) {
                if (!playerId.equals(excludeClientId)) {
                    sendToClient(playerId, message);
                }
            }
        }
    }

    public void sendToClient(String clientId, Message message) {
        ClientHandler handler = model.getClientHandler(clientId);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    public void stop() {
        running = false;
        executor.shutdown();

        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при остановке сервера: " + e.getMessage());
        }

        System.out.println("Сервер остановлен");
    }
}