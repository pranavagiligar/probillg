package com.probill.ui

import com.probill.repository.db.AppDb
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.stage.Stage

open class BaseUi {
    fun openLogin() {
        AppDb.metaDao.getLastMeta()?.let {
            it.isLoggedIn = false
            AppDb.metaDao.update(it)
        }

        val stage = Stage()
        val loader = FXMLLoader()
        loader.location = javaClass.getResource("/login.fxml")
        loader.setController(LoginFlow(stage))
        val root = loader.load<Parent>()
        root.stylesheets.add(javaClass.getResource("/application.css").toExternalForm())
        stage.title = com.probill.Constant.APP_NAME
        stage.scene = javafx.scene.Scene(root)
        stage.show()
    }
}