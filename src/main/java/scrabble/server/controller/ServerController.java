package scrabble.server.controller;

import scrabble.server.model.ServerModel;
import scrabble.server.network.ServerNetworkHandler;

public class ServerController {
    private ServerModel model;
    private ServerNetworkHandler networkHandler;
    private boolean isRunning;
    private int currentPort;

    public ServerController() {
        this.model = new ServerModel();
        this.isRunning = false;
    }

    public void startServer(int port) {
        if (!isRunning) {
            try {
                networkHandler = new ServerNetworkHandler(model);
                networkHandler.start(port);
                isRunning = true;
                currentPort = port;
            } catch (Exception e) {
                System.err.println("Ошибка запуска сервера: " + e.getMessage());
                isRunning = false;
                throw new RuntimeException("Не удалось запустить сервер", e);
            }
        }
    }

    public void stopServer() {
        if (isRunning && networkHandler != null) {
            networkHandler.stop();
            isRunning = false;
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public ServerModel getModel() {
        return model;
    }

    public int getConnectedClientsCount() {
        return model.getAllClientHandlers().size();
    }

    public int getActiveRoomsCount() {
        return 0;
    }
}