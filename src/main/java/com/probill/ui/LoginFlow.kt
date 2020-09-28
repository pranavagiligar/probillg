package com.probill.ui

import com.probill.Constant
import com.probill.model.Meta
import com.probill.model.Setting
import com.probill.model.User
import com.probill.repository.db.AppDb
import com.probill.repository.net.api.AuthApi
import com.probill.service.ApiService
import com.probill.utility.GeneralUtils.isNullOrEmpty
import com.probill.utility.Log
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.stage.Stage
import kotlinx.coroutines.GlobalScope
import java.net.URL
import java.sql.Timestamp
import java.util.*

class LoginFlow(private val primaryStage: Stage) : Initializable {

    private val TAG = LoginFlow::class.java.simpleName

    lateinit var usernameField: TextField
    lateinit var passwordField: PasswordField
    private lateinit var api: AuthApi

    companion object {
        fun openMainWindow(parentStage: Stage) {
            val stage = Stage()
            val loader = FXMLLoader()
            loader.location = javaClass.getResource("/main_window.fxml")
            loader.setController(
                MainWindow(stage)
            )
            val root = loader.load<Parent>()
            root.stylesheets.add(
                javaClass.getResource("/application.css").toExternalForm()
            )
            stage.title = Constant.APP_NAME
            stage.scene = Scene(root)
            parentStage.close()
            stage.show()
        }
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        api = ApiService.getApiService(AuthApi::class.java)

        usernameField.setOnKeyReleased {
            if (it.code == KeyCode.ENTER) {
                passwordField.requestFocus()
            }
        }
        passwordField.setOnKeyReleased {
            if (it.code == KeyCode.ENTER) {
                onLoginClicked(null)
            }
        }
    }

    fun onLoginClicked(action: ActionEvent?) {
        if (!validateInputs()) {
            Alert(Alert.AlertType.ERROR, "Invalid credentials").showAndWait()
            return
        }

        ApiService.request<Any>(GlobalScope, {
            val login = api
                .login(usernameField.text.trim(), passwordField.text.trim())
            Log.d(TAG, "Login success : $login")
            val user = User(
                login.username,
                login.password,
                login.name,
                login.company,
                login.address,
                login.phone,
                login.gstNumber,
                login.session,
                login.enabled,
                Timestamp(login.expiry),
                login.pollingInterval
            )
            val setting = Setting(
                user,
                login.invoicePerPage,
                login.breakupPerInvoice,
                login.eSugamRequired
            )

            val existingUser = AppDb.userDao.getByUsername(login.username)
            if (existingUser != null) {
                AppDb.userDao.update(user)
            } else {
                AppDb.userDao.insert(user)
            }
            val existingSetting =
                AppDb.settingDao.getSettingForUsername(user.username)
            if (existingSetting != null) {
                AppDb.settingDao.update(setting)
            } else {
                AppDb.settingDao.insert(setting)
            }
            AppDb.metaDao.insert(
                Meta(
                    user,
                    login.enabled
                )
            )
            if (!login.enabled) {
                Platform.runLater {
                    Alert(
                        Alert.AlertType.ERROR,
                        "The username '${login.username}' has"
                            + " been disabled. Contact your software provider"
                    ).show()
                }
                return@request
            }
            Platform.runLater {
                openMainWindow(primaryStage)
            }
        }) {
            Log.d(TAG, "Login failed : ${it.data}")
            Platform.runLater {
                Alert(Alert.AlertType.ERROR, it.message).showAndWait()
            }
        }
    }

    private fun validateInputs() =
        !isNullOrEmpty(usernameField.text) && !isNullOrEmpty(passwordField.text)
}