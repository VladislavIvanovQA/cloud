package org.cloud.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.cloud.client.controllers.*;
import org.cloud.client.model.Network;

import java.io.IOException;


public class Client extends Application {
    private static final String MAIN_WINDOW_FXML = "mainWindow.fxml";
    private static final String AUTH_DIALOG_FXML = "authDialog.fxml";
    private static final String REG_DIALOG_FXML = "regDialog.fxml";
    private static final String SHARE_DIALOG_FXML = "shareDialog.fxml";
    private static final String GETTER_SHARE_DIALOG_FXML = "getShareDialog.fxml";
    public static Client INSTANCE;
    private Stage mainStage;
    private Stage authStage;
    private Stage regStage;
    private Stage shareStage;
    private Stage getterShareStage;
    private FXMLLoader mainLoader;
    private FXMLLoader authLoader;
    private FXMLLoader regLoader;
    private FXMLLoader shareLoader;
    private FXMLLoader getterShareLoader;

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

    public Stage getShareStage() {
        return shareStage;
    }

    public Stage getGetterShareStage() {
        return getterShareStage;
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

    public ShareController getShareController() {
        return shareLoader.getController();
    }

    public GetterShareController getGetterShareController() {
        return getterShareLoader.getController();
    }

    private void initViews() throws IOException {
        initMainWindow();
        initAuthDialog();
        initRegDialog();
        initShareDialog();
        initGetterShareDialog();
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
        authStage.setTitle("Authentication");
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
        regStage.setTitle("Registration");
        regStage.setOnCloseRequest(event -> System.exit(0));
    }

    private void initShareDialog() throws IOException {
        shareLoader = new FXMLLoader();
        shareLoader.setLocation(Client.class.getResource(SHARE_DIALOG_FXML));
        Parent shareDialogPanel = shareLoader.load();

        shareStage = new Stage();
        shareStage.initOwner(mainStage);
        shareStage.setResizable(false);
        shareStage.initModality(Modality.WINDOW_MODAL);
        shareStage.setScene(new Scene(shareDialogPanel));
        shareStage.setTitle("Share file");
        shareStage.setOnCloseRequest(event -> shareStage.close());
    }

    private void initGetterShareDialog() throws IOException {
        getterShareLoader = new FXMLLoader();
        getterShareLoader.setLocation(Client.class.getResource(GETTER_SHARE_DIALOG_FXML));
        Parent shareDialogPanel = getterShareLoader.load();

        getterShareStage = new Stage();
        getterShareStage.initOwner(mainStage);
        getterShareStage.setResizable(false);
        getterShareStage.initModality(Modality.WINDOW_MODAL);
        getterShareStage.setScene(new Scene(shareDialogPanel));
        getterShareStage.setTitle("Getting share file");
        getterShareStage.setOnCloseRequest(event -> getterShareStage.close());
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

    public void switchToMainChatWindow(String username) {
        Network.username = username;
        getMainStage().show();
        getMainStage().setTitle("Username: " + username);
        getMainController().initMessageHandler();
        getMainController().init();
        getAuthController().close();
        getAuthStage().close();
        getRegController().close();
        getRegStage().close();
    }

    public void switchToAuthWindow() {
        getMainController().close();
        getMainStage().close();
        getAuthStage().show();
        getAuthController().initMessageHandler();
    }

    public void switchToRegistrationWindow() {
        getAuthStage().close();
        getAuthController().close();
        getRegController().initMessageHandler();
        getRegStage().show();
    }

    public void switchToShareWindow(String filename) {
        getShareController().init(filename);
        getShareStage().show();
    }

    public void switchToGettingShareWindow() {
        getGetterShareController().init();
        getGetterShareStage().show();
    }
}
