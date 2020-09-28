package com.probill;

import com.probill.model.Meta;
import com.probill.repository.db.AppDb;
import com.probill.test.MockInsertTest;
import com.probill.ui.LoginFlow;
import com.probill.utility.Log;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MyApp extends Application {

    private final static String TAG = MyApp.class.getSimpleName();
    private final static boolean DB_TEST = false;

    @Override
    public void start(Stage primaryStage) throws Exception {

        Meta meta =  AppDb.Companion.getMetaDao().getLastMeta();
        if (meta != null && meta.isLoggedIn()) {
            LoginFlow.Companion.openMainWindow(primaryStage);
        } else {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/login.fxml"));
            loader.setController(new LoginFlow(primaryStage));
            Parent root = loader.load();
            root.getStylesheets().add(getClass()
                    .getResource("/application.css").toExternalForm());
            primaryStage.setTitle(Constant.APP_NAME);
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        new Thread(() -> {
            if (DB_TEST) {
                Log.INSTANCE.d(TAG, "Conducting database test");
                AppDb db = new AppDb();
                db.dropDb();
                db.createTables();
//                new MockInsertTest().start();
            } else {
                new AppDb().createTables();
//                if (AppDb.Companion.getUserDao().getByUsername(BaseDao.Companion.getLOGIN_USERNAME()) == null) {
//                    new MockInsertTest().insertTheUser();
//                }
                launch(args);
            }
        }).start();
//        if (!DB_TEST) launch(args);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AppDb.Companion.stop();
                super.run();
            }
        });

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> e.printStackTrace());
    }
}
