package com.vk.admstorm.configuration.kbench

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.*
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.table.JBTable
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.completion.PhpCompletionUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.vk.admstorm.configuration.kphp.KphpUtils
import com.vk.admstorm.env.Env
import com.vk.admstorm.git.sync.SyncChecker
import com.vk.admstorm.notifications.AdmNotification
import com.vk.admstorm.notifications.AdmWarningNotification
import com.vk.admstorm.ssh.SshConnectionService
import com.vk.admstorm.utils.MyPathUtils
import com.vk.admstorm.utils.MySshUtils
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class KBenchConfigurationRunState(
    private val env: ExecutionEnvironment,
    private val conf: KBenchConfiguration
) : RunProfileState {

    private fun buildCommand(): String? {
        val filename = MyPathUtils.remotePathByLocalPath(env.project, conf.filename)

        val includeDirsFlag =
            if (conf.benchType == KBenchType.BenchPhp) ""
            else "--include-dirs='${KphpUtils.includeDirsAsList(env.project).joinToString(",")}'"

        val kphpBinary =
            if (conf.benchType.command == "bench-php") ""
            else "--kphp2cpp-binary ${Env.data.kphp2cpp}"

        val benchmem =
            if (conf.benchType == KBenchType.BenchPhp) ""
            else if (conf.benchmarkMemory) "--benchmem"
            else ""

        val ktestCustomBinaryPath = "~/ktest/ktest"
        val ktestBin = if (MyPathUtils.remoteFileExists(env.project, ktestCustomBinaryPath)) {
            ktestCustomBinaryPath
        } else {
            Env.data.ktestCommand
        }

        if (conf.benchType == KBenchType.BenchAb) {
            val isClassCompare = conf.scope == KBenchScope.Class
            if (isClassCompare) {
                val secondClass = if (conf.compareClassName.isEmpty()) {
                    SelectClassDialog.requestClass(env.project, conf.className)
                } else {
                    PhpIndex.getInstance(conf.project).getClassesByFQN(conf.compareClassName).firstOrNull()
                }
                if (secondClass == null) return null

                // Устанавливаем значение если оно не было установлено ранее
                conf.compareClassName = secondClass.fqn
                conf.name =
                    "${KBenchUtils.className(conf.className)} vs ${KBenchUtils.className(conf.compareClassName)}"

                val secondClassFile = secondClass.containingFile?.virtualFile?.path ?: return null
                val secondClassServerFile = MyPathUtils.remotePathByLocalPath(env.project, secondClassFile)

                return "$ktestBin ${conf.benchType.command}" +
                        " $filename $secondClassServerFile" +
                        " --count ${conf.countIteration}" +
                        " $includeDirsFlag" +
                        " --teamcity" +
                        " --disable-kphp-autoload" +
                        " $kphpBinary" +
                        " $benchmem"
            }

            val firstMethodName = KBenchUtils.benchmarkName(conf.methodName)
            val secondMethodRawName = conf.compareMethodName.ifEmpty {
                SelectDialog.requestMethod(env.project, conf.className, firstMethodName)?.name
            } ?: return null

            val secondMethodName = KBenchUtils.benchmarkName(secondMethodRawName)

            // Устанавливаем значение если оно не было установлено ранее
            conf.compareMethodName = secondMethodRawName
            conf.name = "${KBenchUtils.className(conf.className)}: $firstMethodName vs $secondMethodName"

            return "$ktestBin ${conf.benchType.command}" +
                    " $firstMethodName $secondMethodName" +
                    " --count ${conf.countIteration}" +
                    " $includeDirsFlag" +
                    " --teamcity" +
                    " --disable-kphp-autoload" +
                    " $kphpBinary" +
                    " $benchmem" +
                    " $filename"
        }

        val methodFilter = if (conf.scope == KBenchScope.Method) {
            val className = conf.className.split('\\').lastOrNull() ?: ""
            val methodName = KBenchUtils.benchmarkName(conf.methodName)
            "--run '$className::$methodName$'"
        } else {
            "--run '.*'"
        }

        return "$ktestBin ${conf.benchType.command}" +
                " --count ${conf.countIteration}" +
                " $includeDirsFlag" +
                " --teamcity" +
                " --disable-kphp-autoload" +
                " $kphpBinary" +
                " $methodFilter" +
                " $benchmem" +
                " $filename"
    }

    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        ApplicationManager.getApplication().invokeAndWait {
            doCheckSync()
        }

        return doBench()
    }

    private fun doBench(): DefaultExecutionResult? {
        if (!SshConnectionService.getInstance(env.project).isConnectedOrWarning()) {
            return null
        }

        val consoleProperties = KBenchConsoleProperties(conf, env.executor)
        val console = SMTestRunnerConnectionUtil
            .createConsole(
                consoleProperties.testFrameworkName,
                consoleProperties
            ) as SMTRunnerConsoleView

        val command = buildCommand() ?: return null
        val handler = MySshUtils.exec(env.project, command) ?: return null

        console.attachToProcess(handler)

        val smTestProxy = console.resultsViewer.root as SMTestProxy.SMRootTestProxy
        smTestProxy.setTestsReporterAttached()
        smTestProxy.setSuiteStarted()

        return DefaultExecutionResult(console, handler)
    }

    private fun doCheckSync() {
        SyncChecker.getInstance(env.project).doCheckSyncSilentlyTask({
            onCanceledSync()
        }) {}
    }

    private fun onCanceledSync() {
        AdmWarningNotification("Current launch may not be correct due to out of sync")
            .withTitle("Launch on out of sync")
            .withActions(
                AdmNotification.Action("Synchronize...") { _, notification ->
                    notification.expire()
                    SyncChecker.getInstance(env.project).doCheckSyncSilentlyTask({}, {})
                }
            )
            .show()
    }

    class SelectClassDialog(
        project: Project,
        private val exceptClass: String
    ) : DialogWrapper(project, true) {

        companion object {
            fun requestClass(project: Project, exceptClass: String): PhpClass? {
                val dialog = SelectClassDialog(project, exceptClass)
                if (!dialog.showAndGet()) {
                    return null
                }

                val selected = dialog.selected()
                return PhpIndex.getInstance(project).getClassesByFQN(selected).firstOrNull()
            }
        }

        private val myClassTextField: LanguageTextField

        init {
            title = "Compare with Class"

            myClassTextField = LanguageTextField(PhpLanguage.INSTANCE, project, "")

            PhpCompletionUtil.installClassCompletion(myClassTextField, null, {}, { phpClass: PhpClass ->
                KBenchUtils.isBenchmarkClass(phpClass) && phpClass.fqn != exceptClass
            })

            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row {
                    cell(myClassTextField)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .label("Class:", LabelPosition.LEFT)
                        .comment("Except $exceptClass")
                }.resizableRow()
            }
                .apply {
                    preferredSize = Dimension(350, 60)
                }
        }

        fun selected() = myClassTextField.text
    }

    class SelectDialog<T>(
        project: Project,
        dialogTitle: String,
        columnName: String,
        symbols: List<T>,
    ) : DialogWrapper(project, true) {

        companion object {
            fun requestMethod(project: Project, classNane: String, exceptMethod: String): Method? {
                val selectedClass = PhpIndex.getInstance(project).getAnyByFQN(classNane).firstOrNull()

                val methods = selectedClass?.methods
                    ?.filter {
                        KBenchUtils.isBenchmarkMethod(it) && KBenchUtils.benchmarkName(it.name) != exceptMethod
                    }
                    ?: emptyList()

                val dialog = SelectDialog<Method>(project, "Compare with Method", "Methods", methods)
                if (!dialog.showAndGet()) {
                    return null
                }

                return dialog.selected()
            }
        }

        private var myTable: JBTable
        private var myModel: TableModel<T>

        init {
            title = dialogTitle

            myModel = TableModel(columnName, symbols)
            myTable = JBTable(myModel)
            myTable.emptyText.text = "No other benchmark $columnName"

            myTable.setEnableAntialiasing(true)
            myTable.selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
            myTable.rowSelectionAllowed = true
            myTable.setShowGrid(false)
            myTable.columnModel.columnMargin = 0
            myTable.tableHeader.reorderingAllowed = false
            myTable.border = IdeBorderFactory.createBorder(SideBorder.LEFT.and(SideBorder.RIGHT).and(SideBorder.BOTTOM))

            myTable.setDefaultRenderer(Any::class.java, SymbolCellRenderer())

            object : DoubleClickListener() {
                override fun onDoubleClick(e: MouseEvent): Boolean {
                    onSelect()
                    return true
                }
            }.installOn(myTable)

            init()
        }

        private fun onSelect() {
            doOKAction()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row {
                    label("Select method to compare with:")
                }
                row {
                    scrollCell(myTable)
                        .horizontalAlign(HorizontalAlign.FILL)
                        .verticalAlign(VerticalAlign.FILL)
                }.resizableRow()
            }
                .apply {
                    preferredSize = Dimension(300, 400)
                }
        }

        fun selected() = myModel.getSymbol(myTable.selectedRow)

        private inner class SymbolCellRenderer : ColoredTableCellRenderer() {
            override fun customizeCellRenderer(
                table: JTable,
                value: Any?,
                selected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ) {
                border = null

                if (value is Method) {
                    icon = AllIcons.Nodes.Method
                    append(value.name)
                }
            }
        }

        private inner class TableModel<T>(
            private val columnName: String,
            private val symbols: List<T>
        ) : AbstractTableModel() {
            override fun getRowCount() = symbols.size
            override fun getColumnCount() = 1
            override fun isCellEditable(rowIndex: Int, columnIndex: Int) = false

            fun getSymbol(rowIndex: Int): T? = symbols.getOrNull(rowIndex)

            override fun getColumnName(column: Int): String {
                return when (column) {
                    0 -> columnName
                    else -> "unknown"
                }
            }

            override fun getValueAt(rowIndex: Int, columnIndex: Int): T? {
                return when (columnIndex) {
                    0 -> symbols[rowIndex]
                    else -> null
                }
            }
        }
    }
}
