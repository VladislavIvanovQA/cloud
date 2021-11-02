package org.cloud.client;

import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.cloud.client.controllers.AuthController;
import org.cloud.client.controllers.CloudMainController;
import org.cloud.client.controllers.RegController;
import org.cloud.client.model.Network;

import java.io.IOException;


public class Client extends Application {
    private static final String MAIN_WINDOW_FXML = "mainWindow.fxml";
    private static final String AUTH_DIALOG_FXML = "authDialog.fxml";
    private static final String REG_DIALOG_FXML = "regDialog.fxml";
    public static Client INSTANCE;
    private Stage mainStage;
    private Stage authStage;
    private Stage regStage;
    private FXMLLoader mainLoader;
    private FXMLLoader authLoader;
    private FXMLLoader regLoader;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        INSTANCE = this;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.mainStage = primaryStage;
        initViews();
        getChatStage().show();
        getAuthStage().show();
        getAuthController().initMessageHandler();
    }

    public Stage getMainStage() {
        return mainStage;
    }

    public Stage getAuthStage() {
        return authStage;
    }

    public Stage getRegStage() {
        return regStage;
    }

    private AuthController getAuthController() {
        return authLoader.getController();
    }

    public CloudMainController getMainController() {
        return mainLoader.getController();
    }

    public RegController getRegController() {
        return regLoader.getController();
    }

    private void initViews() throws IOException {
        initMainWindow();
        initAuthDialog();
        initRegDialog();
    }

    private void initMainWindow() throws IOException {
        mainLoader = new FXMLLoader();
        mainLoader.setLocation(Client.class.getResource(MAIN_WINDOW_FXML));

        Parent root = mainLoader.load();
        this.mainStage.setScene(new Scene(root));

        setStageForSecondScreen(mainStage);
        mainStage.setResizable(false);
    }

    private void initAuthDialog() throws IOException {
        authLoader = new FXMLLoader();
        authLoader.setLocation(Client.class.getResource(AUTH_DIALOG_FXML));
        Parent authDialogPanel = authLoader.load();

        authStage = new Stage();
        authStage.initOwner(mainStage);
        authStage.setResizable(false);
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.setScene(new Scene(authDialogPanel));
        authStage.setTitle("Авторизация");
        authStage.setOnCloseRequest(event -> System.exit(0));
    }

    private void initRegDialog() throws IOException {
        regLoader = new FXMLLoader();
        regLoader.setLocation(Client.class.getResource(REG_DIALOG_FXML));
        Parent regDialogPanel = regLoader.load();

        regStage = new Stage();
        regStage.initOwner(mainStage);
        regStage.setResizable(false);
        regStage.initModality(Modality.WINDOW_MODAL);
        regStage.setScene(new Scene(regDialogPanel));
        regStage.setTitle("Регистрация");
        regStage.setOnCloseRequest(Event::consume);
    }

    private void setStageForSecondScreen(Stage primaryStage) {
        Screen secondScreen = getSecondScreen();
        Rectangle2D bounds = secondScreen.getBounds();
        primaryStage.setX(bounds.getMinX() + (bounds.getWidth() - 300) / 2);
        primaryStage.setY(bounds.getMinY() + (bounds.getHeight() - 200) / 2);
    }

    private Screen getSecondScreen() {
        for (Screen screen : Screen.getScreens()) {
            if (!screen.equals(Screen.getPrimary())) {
                return screen;
            }
        }
        return Screen.getPrimary();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    public Stage getChatStage() {
        return mainStage;
    }

    public void switchToMainChatWindow(String username) {
        Network.username = username;
        getMainStage().setTitle("Имя пользователя: " + username);
        getMainController().initMessageHandler();
        getMainController().init();
        getAuthController().close();
        getRegController().close();
        getRegStage().close();
        getAuthStage().close();
    }

    public void switchToRegistrationWindow() {
        getAuthStage().close();
        getRegController().initMessageHandler();
        getRegStage().show();
    }
}
