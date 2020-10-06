package com.probill.service

import com.probill.model.User
import com.probill.repository.db.AppDb
import com.probill.utility.GeneralUtils
import javafx.print.*
import javafx.scene.Node
import javafx.scene.transform.Scale
import javafx.scene.transform.Transform
import javafx.stage.Stage

class PrintService {

    // JavaFx by RIP
    // https://docs.oracle.com/javase/tutorial/2d/printing/printable.html
    fun selectPrinter() {
        Printer.getAllPrinters().forEach {
            val pJ = PrinterJob.createPrinterJob(it)
            if (pJ != null) {
                val success = pJ.showPrintDialog(Stage())
                // this is the important line
                if (success) {
                    pJ.endJob()
                }
            }
        }
    }

    fun print(node: Node, stage: Stage, jobName: String, isFirstPage: Boolean) {
        val printer: Printer = Printer.getDefaultPrinter()
        val pageLayout: PageLayout = printer.createPageLayout(
            Paper.A5,
            PageOrientation.PORTRAIT,
            .1,
            .1,
            .1,
            .1
        )
//        val scaleX: Double = pageLayout.printableWidth / node.boundsInParent.width
//        val scaleY: Double = pageLayout.printableHeight / node.boundsInParent.height
//        node.transforms.add(Scale(scaleX, scaleY))
//        node.transforms.add(Transform.translate(100.0, 100.0))
        node.transforms.add(Transform.translate(25.0, 20.0))
        node.prefHeight(pageLayout.printableHeight)
        node.prefWidth(pageLayout.printableWidth)
        node.transforms.add(Scale(0.45, 0.45))

        val job: PrinterJob? = PrinterJob.createPrinterJob()
        job?.jobSettings?.printColor = PrintColor.MONOCHROME
        job?.jobSettings?.jobName = jobName
        if (job != null) {
            var isPrintSettingEnabled = false
            AppDb.metaDao.getLastMeta()?.let {
                if (it.isLoggedIn && it.user.enabled) {
                    AppDb.settingDao.getSettingForUsername(
                        it.user.username
                    )?.let { setting ->
                        isPrintSettingEnabled = setting.printSettingsRequired
                    }
                }
            }
            if (!isPrintSettingEnabled || !isFirstPage) {
                if (job.printPage(pageLayout, node)) job.endJob()
            } else if (isPrintSettingEnabled && job.showPrintDialog(stage)) {
                if (job.printPage(pageLayout, node)) job.endJob()
            }
        }
    }
//            job.jobStatusProperty().addListener { observable, oldValue, newValue ->
//                when (observable.value) {
//                    PrinterJob.JobStatus.PRINTING -> Log.d(TAG, "Printing ${observable.value}")
//                    PrinterJob.JobStatus.CANCELED -> Log.d(TAG, "Printing ${observable.value}")
//                    PrinterJob.JobStatus.NOT_STARTED -> Log.d(TAG, "Printing ${observable.value}")
//                    PrinterJob.JobStatus.DONE -> Log.d(TAG, "Printing ${observable.value}")
//                    PrinterJob.JobStatus.ERROR -> Log.d(TAG, "Printing ${observable.value}")
//                }
//            }
}